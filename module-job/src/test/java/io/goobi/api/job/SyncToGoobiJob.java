package io.goobi.api.job;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.goobi.production.flow.jobs.AbstractGoobiJob;

import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import io.goobi.api.job.actapro.model.ActaProApi;
import io.goobi.api.job.actapro.model.ActaProApiConfiguration;
import io.goobi.api.job.actapro.model.AuthenticationToken;
import io.goobi.api.job.actapro.model.Document;
import io.goobi.api.job.actapro.model.DocumentBlock;
import io.goobi.api.job.actapro.model.DocumentField;
import io.goobi.api.job.actapro.model.DocumentSearchFilter;
import io.goobi.api.job.actapro.model.DocumentSearchFilter.OperatorEnum;
import io.goobi.api.job.actapro.model.DocumentSearchParams;
import io.goobi.api.job.actapro.model.ErrorResponse;
import io.goobi.api.job.actapro.model.ExtendedEadEntry;
import io.goobi.api.job.actapro.model.MetadataMapping;
import io.goobi.api.job.actapro.model.NodeApi;
import io.goobi.api.job.actapro.model.SearchResultPage;
import io.goobi.api.job.actapro.model.SimpleEadEntry;
import io.goobi.api.job.actapro.model.UnauthorizedException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SyncToGoobiJob extends AbstractGoobiJob {

    private ActaProApiConfiguration actaProConfig;

    private static final DateTimeFormatter requestDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void execute() {
        // read configuration file
        actaProConfig = new ActaProApiConfiguration();

        LocalDate now = LocalDate.now();

        // for each configured inventory

        for (String inventory : actaProConfig.getConfiguredInventories()) {
            log.debug("Harvest latest changes for inventory " + inventory);
            String rootElementID = actaProConfig.getRootId(inventory);
            RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(inventory);
            if (recordGroup == null) {
                log.debug("Archive does not exist: " + inventory);
                continue;
            }

            Map<String, Integer> nodeIdCache = NodeApi.loadNodeIdCache(recordGroup, actaProConfig.getIdentifierFieldName());
            log.debug("Node ID cache loaded: " + nodeIdCache.size() + " existing entries.");
            try (Client client = ClientBuilder.newClient()) {
                AuthenticationToken token = ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(),
                        actaProConfig.getAuthServiceUrl(), actaProConfig.getAuthServiceUsername(),
                        actaProConfig.getAuthServicePassword());
                log.debug("Authenticated.");

                DocumentSearchParams searchRequest = new DocumentSearchParams();

                searchRequest.query("*");

                // set start date to current date - 1
                DocumentSearchFilter filter = new DocumentSearchFilter();
                filter.fieldName("chdate");
                filter.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
                filter.fieldValue(requestDateFormatter.format(now.minusDays(1)) + "T00:00:00Z");
                searchRequest.addFiltersItem(filter);
                boolean isLast = false;
                int currentPage = 0;
                while (!isLast) {
                    WebTarget target = client.target(actaProConfig.getConnectorUrl()).path("documents").queryParam("page", currentPage);
                    Invocation.Builder builder = target.request();
                    builder.header("Accept", "application/json");
                    builder.header("Authorization", "Bearer " + token.getAccessToken());

                    try (Response response = ActaProApi.retry(new IOException("failed after 5 retries"), Duration.ofSeconds(5l), 5,
                            () -> builder.post(Entity.entity(searchRequest, MediaType.APPLICATION_JSON)))) {
                        if (200 == response.getStatus()) {

                            SearchResultPage srp = response.readEntity(SearchResultPage.class);
                            List<Map<String, String>> contentMap = srp.getContent();
                            for (Map<String, String> content : contentMap) {
                                if (content.get("path").contains(rootElementID)) {
                                    String id = content.get("id");
                                    Document doc = null;
                                    try {
                                        doc = ActaProApi.getDocumentByKey(client, token, actaProConfig.getConnectorUrl(), id);
                                    } catch (UnauthorizedException e) {
                                        throw e;
                                    } catch (Exception e) {
                                        log.error("Unable to retrieve document with id '" + id + "'", e);
                                        log.error(e);
                                    }
                                    if (doc != null) {
                                        doc.setPath(content.get("path"));
                                        importDocument(client, doc, recordGroup, rootElementID, token, nodeIdCache);
                                    }
                                }
                            }
                            isLast = srp.getLast();
                            currentPage++;
                        } else {
                            ErrorResponse error = response.readEntity(ErrorResponse.class);
                            if (error != null) {
                                log.error("Search error for child document search , status: {}, text: {}, parentid: {},  paginator {}",
                                        error.getStatus(),
                                        error.getMessage(), rootElementID, currentPage);
                            } else {
                                log.error(
                                        "Search error for child document search, HTTP status: {}, response body could not be parsed, , parentid: {},  paginator {}",
                                        response.getStatus(), rootElementID, currentPage);
                            }
                            isLast = true;
                        }

                    } catch (UnauthorizedException e) {
                        throw e;
                    } catch (IOException e) {
                        log.error("Cannot get  elements for " + rootElementID);
                    }
                }
            } catch (IOException e1) {
                log.error(e1);
            }

        }
    }

    private String importDocument(Client client, Document doc, RecordGroup recordGroup, String rootElementID, AuthenticationToken token,
            Map<String, Integer> nodeIdCache) throws IOException {
        String documentId = doc.getDocKey();
        String parentNodeId = null;
        String docOrder = null;
        for (DocumentField field : doc.getBlock().getFields()) {
            String fieldType = field.getType();
            if ("Ref_Gp".equals(fieldType)) {
                for (DocumentField subfield : field.getFields()) {
                    if ("Ref_DocKey".equals(subfield.getType())) {
                        parentNodeId = subfield.getValue();
                    } else if ("Ref_DocOrder".equals(subfield.getType())) {
                        docOrder = subfield.getValue();
                    }
                }
            }
        }

        log.debug("Document id: {}", documentId);
        // find matching ead entry
        Integer entryId = nodeIdCache.get(documentId);
        if (entryId != null) {

            log.debug("Found node with with ACTApro ID '" + documentId + "', update existing node.");

            ExtendedEadEntry entry = NodeApi.loadExtendendEntry(entryId);

            NodeInitializer.initEadNodeWithMetadata(entry, actaProConfig.getConfig().getConfiguredFields());
            String fingerprintBeforeImport = entry.getFingerprint();

            // check if document still have the same parent node
            if (parentNodeId != null && docOrder != null && entry.getParentId() != null) {
                // parentNode == null is the root element, should not be possible
                Integer parentEntryId = nodeIdCache.get(parentNodeId);
                if (parentEntryId == null) {
                    // node has been changed to a new parent node that does not yet exist
                    // this case should not be possible because the new parent node is included in the
                    // document list before the current node and was created at this point
                } else if (parentEntryId.intValue() != entry.getParentId()) {
                    // node has a different parent
                    entry.setParentId(parentEntryId);
                    entry.setOrderNumber(Integer.parseInt(docOrder));
                    // move to correct position within the parent
                    NodeApi.saveNode(recordGroup.getId(), entry);
                }
            }

            // parse document, get metadata fields
            parseDocumentMetadata(doc, entry);

            String fingerprintAfterImport = entry.getFingerprint();
            // save, if metadata was changed
            if (!fingerprintBeforeImport.equals(fingerprintAfterImport)) {
                NodeApi.saveNode(recordGroup.getId(), entry);
            }

        } else {
            log.debug("Document with ID " + documentId + " does not exist, create new node.");
            if (doc.getPath() != null) {
                String[] paths = doc.getPath().split(";");

                Integer lastElementId = null;
                boolean rootElementFound = false;

                for (String path : paths) {
                    path = path.trim();
                    // ignore first parts of the path, if our root element is not the Arch element
                    if (path.equals(rootElementID)) {
                        rootElementFound = true;
                    }
                    if (rootElementFound) {
                        Integer parentEntryId = nodeIdCache.get(path);

                        if (parentEntryId != null) {
                            // ancestor element exists
                            lastElementId = parentEntryId;
                        } else {
                            // ancestor element does not exist, create it as sub element of last existing node
                            if (lastElementId == null) {
                                lastElementId = nodeIdCache.get(rootElementID);
                            }

                            Document currentDoc = null;
                            try {
                                currentDoc = ActaProApi.getDocumentByKey(client, token, actaProConfig.getConnectorUrl(), path);
                            } catch (UnauthorizedException e1) {
                                throw e1;
                            } catch (Exception e1) {
                                log.error(e1);
                                return null;
                            }
                            if (currentDoc != null) {
                                lastElementId = createNodeForDocument(currentDoc, lastElementId, recordGroup, nodeIdCache);
                            }
                        }
                    }
                }
            }
        }
        return documentId;

    }

    private Integer createNodeForDocument(io.goobi.api.job.actapro.model.Document doc, Integer parentDbId, RecordGroup recordGroup,
            Map<String, Integer> nodeIdCache) {
        int orderNumber = 0;
        String orderStr = NodeApi.getDocOrder(doc);
        if (orderStr != null) {
            try {
                orderNumber = Integer.parseInt(orderStr);
            } catch (NumberFormatException e) {
                log.error("Cannot parse Ref_DocOrder value '{}': {}", orderStr, e.getMessage());
            }
        }

        SimpleEadEntry parent = NodeApi.loadSimpleEntry(parentDbId);
        ExtendedEadEntry entry = new ExtendedEadEntry(orderNumber, parent.getHierarchy() + 1);
        entry.setParentId(parent.getId());
        if (StringUtils.isBlank(parent.getSequence())) {
            entry.setSequence(parent.getOrder() + "");
        } else {
            entry.setSequence(parent.getSequence() + "." + parent.getOrder());
        }
        entry.setId("id_" + UUID.randomUUID());
        entry.setLabel(doc.getDocTitle());

        for (IMetadataField emf : actaProConfig.getConfig().getConfiguredFields()) {
            if (emf.isGroup()) {
                NodeInitializer.loadGroupMetadata(entry, emf, null);
            } else if ("unittitle".equals(emf.getName())) {
                List<IValue> titleData = new ArrayList<>();
                titleData.add(new ExtendendValue(null, doc.getDocTitle(), null, null));
                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, titleData);
                NodeInitializer.addFieldToNode(entry, toAdd);
            } else if (emf.getName().equals(actaProConfig.getIdentifierFieldName())) {
                List<IValue> idData = new ArrayList<>();
                idData.add(new ExtendendValue(null, doc.getDocKey(), null, null));
                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, idData);
                NodeInitializer.addFieldToNode(entry, toAdd);
            } else {
                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, null);
                NodeInitializer.addFieldToNode(entry, toAdd);
            }
        }
        parseDocumentMetadata(doc, entry);
        entry.setNodeType(actaProConfig.getNodes().get(doc.getType()));
        entry.calculateFingerprint();
        NodeApi.saveNode(recordGroup.getId(), entry);
        if (nodeIdCache != null && doc.getDocKey() != null) {
            nodeIdCache.put(doc.getDocKey(), entry.getDatabaseId());
        }
        return entry.getDatabaseId();
    }

    private void parseDocumentMetadata(Document doc, IEadEntry entry) {
        DocumentBlock block = doc.getBlock();

        for (DocumentField field : block.getFields()) {

            String fieldType = field.getType();
            // find ead metadata name

            DocumentField matchedField = null;
            // first check, if field name is used in a group // has sub fields
            for (MetadataMapping mm : actaProConfig.getMetadataFields()) {
                if (mm.getJsonGroupType().equals(fieldType)) {
                    for (DocumentField subfield : field.getFields()) {
                        String subType = subfield.getType();
                        if (subType.equals(mm.getJsonType())) {
                            matchedField = subfield;
                        }
                    }
                    // if not, search for regular data
                } else if (mm.getJsonType().equals(fieldType)) {
                    matchedField = field;
                }

                if (matchedField != null) {
                    NodeApi.addMetadataValue(entry, mm, matchedField);
                }
            }
        }
        entry.calculateFingerprint();
    }

    @Override
    public String getJobName() {
        return "intranda_quartz_actaProToGoobiJob";
    }

}
