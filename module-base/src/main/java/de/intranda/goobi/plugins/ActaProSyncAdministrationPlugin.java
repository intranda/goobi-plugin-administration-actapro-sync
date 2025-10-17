package de.intranda.goobi.plugins;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.beans.Process;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;
import org.goobi.interfaces.INodeType;
import org.goobi.interfaces.IValue;
import org.goobi.model.ExtendendValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.goobi.production.plugin.interfaces.IPushPlugin;
import org.omnifaces.cdi.PushContext;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
import de.intranda.goobi.plugins.model.EadEntry;
import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import io.goobi.api.job.actapro.model.ActaProApi;
import io.goobi.api.job.actapro.model.AuthenticationToken;
import io.goobi.api.job.actapro.model.Document;
import io.goobi.api.job.actapro.model.DocumentBlock;
import io.goobi.api.job.actapro.model.DocumentField;
import io.goobi.api.job.actapro.model.DocumentSearchFilter;
import io.goobi.api.job.actapro.model.DocumentSearchFilter.OperatorEnum;
import io.goobi.api.job.actapro.model.DocumentSearchParams;
import io.goobi.api.job.actapro.model.ErrorResponse;
import io.goobi.api.job.actapro.model.MetadataMapping;
import io.goobi.api.job.actapro.model.SearchResultPage;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.exceptions.UGHException;

@PluginImplementation
@Log4j2
public class ActaProSyncAdministrationPlugin implements IAdministrationPlugin, IPushPlugin {
    private static final int MAX_DOCUMENT_IMPORT_RETRIES = 5;
    private static final int MAX_DOCUMENT_IMPORT_RETRY_DELAY = 3000;

    private static final long serialVersionUID = 2632106883746583247L;

    @Getter
    private String title = "intranda_administration_actapro_sync";

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_actapro_sync.xhtml";

    @Getter
    @Setter
    private LocalDate startDate = LocalDate.now();

    //    @Getter
    //    @Setter
    //    private String startDate = "2023-04-14T15:33:44Z";
    @Getter
    @Setter
    private LocalDate endDate;

    @Getter
    private List<String> configuredInventories;

    @Getter
    @Setter
    private String database;

    // authentication
    private String authServiceUrl;
    private String authServiceHeader;
    private String authServiceUsername;
    private String authServicePassword;

    private String connectorUrl;

    private String identifierFieldName;

    private String documentOwner;

    @Getter
    private transient ArchiveManagementConfiguration config;

    private transient XMLConfiguration actaProConfig;

    private transient List<MetadataMapping> metadataFields;

    private transient Map<String, INodeType> nodes;

    private DateTimeFormatter documentDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    private DateTimeFormatter requestDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateTimeFormatter logDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private long lastPush = System.currentTimeMillis();

    private PushContext pusher;
    @Getter
    private Queue<String> logQueue = new CircularFifoQueue<>(48);
    @Getter
    private boolean run = false;

    public ActaProSyncAdministrationPlugin() {
        log.trace("initialize plugin");
        try {
            readConfiguration();
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    private void readConfiguration() throws ConfigurationException {
        updateLog("Start reading the configuration");

        try {
            config = new ArchiveManagementConfiguration();
            config.readConfiguration("");
        } catch (ConfigurationException e) {
            log.error(e);
        }

        actaProConfig = new XMLConfiguration(
                ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_actapro_sync.xml");
        actaProConfig.setListDelimiter('&');
        actaProConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
        actaProConfig.setExpressionEngine(new XPathExpressionEngine());

        configuredInventories = new ArrayList<>();

        List<HierarchicalConfiguration> hcl = actaProConfig.configurationsAt("/inventory");

        for (HierarchicalConfiguration hc : hcl) {
            configuredInventories.add(hc.getString("@archiveName"));
        }

        authServiceUrl = actaProConfig.getString("/authentication/authServiceUrl");
        authServiceHeader = actaProConfig.getString("/authentication/authServiceHeader");
        authServiceUsername = actaProConfig.getString("/authentication/authServiceUsername");
        authServicePassword = actaProConfig.getString("/authentication/authServicePassword");
        connectorUrl = actaProConfig.getString("/connectorUrl");

        identifierFieldName = actaProConfig.getString("/eadIdField");

        documentOwner = actaProConfig.getString("/documentOwner", "ACTAPRO");

        metadataFields = new ArrayList<>();

        List<HierarchicalConfiguration> mapping = actaProConfig.configurationsAt("/metadata/field");
        for (HierarchicalConfiguration c : mapping) {
            MetadataMapping mm = new MetadataMapping(c.getString("@type"), c.getString("@groupType", ""), c.getString("@eadField"),
                    c.getString("@eadGroup", ""), c.getString("@eadArea"));
            metadataFields.add(mm);
        }

        nodes = new HashMap<>();
        List<HierarchicalConfiguration> nodeTypes = actaProConfig.configurationsAt("/nodeTypes/type");

        INodeType defaultType = null;
        for (INodeType nodeType : config.getConfiguredNodes()) {
            if ("folder".equals(nodeType.getNodeName())) {
                defaultType = nodeType;
            }
        }

        for (HierarchicalConfiguration c : nodeTypes) {
            String actaProType = c.getString("@actaPro");
            String nodeType = c.getString("@node");
            INodeType type = null;
            for (INodeType nt : config.getConfiguredNodes()) {
                if (nt.getNodeName().equals(nodeType)) {
                    type = nt;
                }
            }
            if (type != null) {
                // use configured type
                nodes.put(actaProType, type);
            } else {
                // or default type
                nodes.put(actaProType, defaultType);
            }
        }
        updateLog("Configuration successfully read");

    }

    public void downloadFromActaPro() {
        if (run) {
            // abort, if import is running
            updateLog("Previous import is still running, abort");

            return;
        }

        String databaseName = database;

        if (StringUtils.isBlank(databaseName) || "null".equals(databaseName)) {
            Helper.setFehlerMeldung("intranda_administration_actapro_noDatabase");
            run = false;
            return;
        }
        String actaproId = actaProConfig.getString("/inventory[@archiveName='" + database + "']/@actaproId");

        // check if database exist, load it

        updateLog("Start import for inventory " + databaseName);
        updateLog("ACTApro root element is " + actaproId);

        run = true;
        Runnable runnable = () -> {
            lastPush = System.currentTimeMillis();

            RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
            if (recordGroup == null) {
                Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                run = false;
                return;
            }

            IEadEntry rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());

            updateLog("Archivemanagement database loaded.");

            // search for all actapro documents within configured id
            List<Document> documents = null;
            try (Client client = ClientBuilder.newClient()) {
                updateLog("Try to authenticate.");
                AuthenticationToken token = ActaProApi.authenticate(client, authServiceHeader, authServiceUrl, authServiceUsername,
                        authServicePassword);
                updateLog("Authenticated.");

                long start = System.currentTimeMillis();
                documents = findDocuments(client, token, actaproId);
                updateLog("Found " + documents.size() + " ACTApro documents to import.");

                updateLog("Document collection duration: " + ((System.currentTimeMillis() - start) / 1000) + " sec");
            } catch (Exception e) {
                log.error("Error during document search: {}", e.getMessage(), e);
                updateLog("Error during document search: " + e.getMessage());
                throw e;
            }

            long start = System.currentTimeMillis();
            if (!documents.isEmpty()) {
                for (Document doc : documents) {
                    // get doc id
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
                    Integer entryId = ArchiveManagementManager.findNodeById(identifierFieldName, documentId);
                    if (entryId != null) {

                        updateLog("Found node with with ACTApro ID '" + documentId + "', update existing node.");

                        IEadEntry entry = null;
                        for (IEadEntry e : rootElement.getAllNodes()) {
                            if (entryId.equals(e.getDatabaseId())) {
                                entry = e;
                                break;
                            }
                        }

                        NodeInitializer.initEadNodeWithMetadata(entry, getConfig().getConfiguredFields());
                        String fingerprintBeforeImport = entry.getFingerprint();

                        // check if document still have the same parent node
                        if (parentNodeId != null && docOrder != null) {
                            IEadEntry parentNode = entry.getParentNode();
                            // parentNode == null is the root element, should not be possible
                            if (parentNode != null) {
                                Integer parentEntryId = ArchiveManagementManager.findNodeById(identifierFieldName, parentNodeId);
                                if (parentEntryId == null) {
                                    // node has been changed to a new parent node that does not yet exist
                                    // TODO this case should not be possible because the new parent node is included in the
                                    // document list before the current node and was created at this point
                                } else if (parentEntryId.intValue() != parentNode.getDatabaseId()) {
                                    // node has a different parent

                                    // remove element from old parent
                                    parentNode.getSubEntryList().remove(entry);
                                    // update order number of existing elements
                                    parentNode.reOrderElements();
                                    parentNode.updateHierarchy();
                                    List<IEadEntry> nodesToUpdate = parentNode.getAllNodes();

                                    // find new parent node
                                    IEadEntry newParentNode = null;
                                    for (IEadEntry e : rootElement.getAllNodes()) {
                                        if (e.getDatabaseId().equals(parentEntryId)) {
                                            newParentNode = e;
                                            break;
                                        }
                                    }
                                    entry.setParentNode(newParentNode);
                                    newParentNode.addSubEntry(entry);
                                    entry.setOrderNumber(Integer.parseInt(docOrder));
                                    // move to correct position within the parent

                                    newParentNode.sortElements();
                                    newParentNode.updateHierarchy();

                                    nodesToUpdate.addAll(newParentNode.getAllNodes());

                                    ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), nodesToUpdate);

                                }
                            }
                        }

                        // parse document, get metadata fields
                        parseDocumentMetadata(doc, entry);

                        String fingerprintAfterImport = entry.getFingerprint();
                        // save, if metadata was changed
                        if (!fingerprintBeforeImport.equals(fingerprintAfterImport)) {
                            ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                        }

                    } else {
                        updateLog("Document with ID " + documentId + " does not exist, create new node.");

                        String[] paths = doc.getPath().split(";");
                        Integer lastElementId = null;
                        boolean rootElementFound = false;
                        for (String path : paths) {
                            path = path.trim();
                            // ignore first parts of the path, if our root element is not the Arch element
                            if (path.equals(actaproId)) {
                                rootElementFound = true;
                            }
                            if (rootElementFound) {
                                Integer parentEntryId = ArchiveManagementManager.findNodeById(identifierFieldName, path);
                                if (parentEntryId != null) {
                                    // ancestor element exists
                                    lastElementId = parentEntryId;
                                } else {
                                    // ancestor element does not exist, create it as sub element of last existing node
                                    IEadEntry lastAncestorNode = null;
                                    for (IEadEntry e : rootElement.getAllNodes()) {
                                        if (e.getDatabaseId().equals(lastElementId)) {
                                            lastAncestorNode = e;
                                            break;
                                        }
                                    }
                                    try (Client client = ClientBuilder.newClient()) {
                                        AuthenticationToken token =
                                                ActaProApi.authenticate(client, authServiceHeader, authServiceUrl, authServiceUsername,
                                                        authServicePassword);
                                        Document currentDoc = ActaProApi.getDocumentByKey(client, token, connectorUrl, path);

                                        int orderNumber = 0;

                                        for (DocumentField field : currentDoc.getBlock().getFields()) {
                                            String fieldType = field.getType();
                                            if ("Ref_Gp".equals(fieldType)) {
                                                for (DocumentField subfield : field.getFields()) {
                                                    if ("Ref_DocOrder".equals(subfield.getType())) {
                                                        orderNumber = Integer.parseInt(subfield.getValue());
                                                    }
                                                }
                                            }
                                        }
                                        EadEntry entry =
                                                new EadEntry(orderNumber,
                                                        lastAncestorNode.getHierarchy() + 1);
                                        entry.setId("id_" + UUID.randomUUID());

                                        //  add all metadata from document

                                        entry.setLabel(currentDoc.getDocTitle());

                                        for (IMetadataField emf : config.getConfiguredFields()) {
                                            if (emf.isGroup()) {
                                                NodeInitializer.loadGroupMetadata(entry, emf, null);
                                            } else if ("unittitle".equals(emf.getName())) {
                                                List<IValue> titleData = new ArrayList<>();
                                                titleData.add(new ExtendendValue(null, currentDoc.getDocTitle(), null, null));
                                                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, titleData);
                                                NodeInitializer.addFieldToNode(entry, toAdd);
                                            } else if (emf.getName().equals(identifierFieldName)) {
                                                List<IValue> idData = new ArrayList<>();
                                                idData.add(new ExtendendValue(null, currentDoc.getDocKey(), null, null));
                                                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, idData);
                                                NodeInitializer.addFieldToNode(entry, toAdd);
                                            } else {
                                                IMetadataField toAdd = NodeInitializer.addFieldToEntry(entry, emf, null);
                                                NodeInitializer.addFieldToNode(entry, toAdd);
                                            }
                                        }
                                        parseDocumentMetadata(currentDoc, entry);

                                        entry.setNodeType(nodes.get(currentDoc.getType()));

                                        // move to correct position within the parent
                                        lastAncestorNode.addSubEntry(entry);
                                        lastAncestorNode.sortElements();
                                        lastAncestorNode.updateHierarchy();
                                        entry.calculateFingerprint();

                                        ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                                        lastElementId = entry.getDatabaseId();
                                        ArchiveManagementManager.updateNodeHierarchy(recordGroup.getId(), lastAncestorNode.getAllNodes());
                                    }
                                }
                            }
                        }
                    }
                    // set doc to null to free up some space
                    doc = null;
                }
            }
            updateLog("Import duration: " + ((System.currentTimeMillis() - start) / 1000) + " sec");
            run = false;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error(e);
            }
            if (pusher != null) {
                pusher.send("update");
            }
        };
        Thread t = new Thread(runnable);
        t.setUncaughtExceptionHandler((t1, e) -> {
            log.error("Uncaught exception in \"downloadFromActaPro\"-thread: {}", e.getMessage(), e);
            updateLog("Uncaught exception in \"downloadFromActaPro\"-thread: " + e.getMessage());
        });
        t.start();
    }

    private void parseDocumentMetadata(Document doc, IEadEntry entry) {
        DocumentBlock block = doc.getBlock();

        for (DocumentField field : block.getFields()) {

            String fieldType = field.getType();
            // find ead metadata name

            DocumentField matchedField = null;
            // first check, if field name is used in a group // has sub fields
            for (MetadataMapping mm : metadataFields) {
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
                    addMetadataValue(entry, mm, matchedField);
                }
            }
        }
        entry.calculateFingerprint();
    }

    private void addMetadataValue(IEadEntry entry, MetadataMapping matchedMapping, DocumentField matchedField) {
        String value = matchedField.getPlainValue();
        if (StringUtils.isBlank(value)) {
            value = matchedField.getValue();
        }

        switch (matchedMapping.getEadArea()) {
            case "1":
                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                    // add/replace value
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "2":
                for (IMetadataField emf : entry.getContextAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "3":
                for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "4":
                for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "5":
                for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "6":
                for (IMetadataField emf : entry.getNotesAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "7":
                for (IMetadataField emf : entry.getDescriptionControlAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
        }
    }

    public void uploadToActaPro() {
        if (run) {
            // abort, if import is running
            logQueue.add("Previous export is still running, abort");
            return;
        }

        // load database

        String databaseName = database;
        updateLog("Start export for inventory " + databaseName);
        // check if database exist, load it
        run = true;
        Runnable runnable = () -> {
            lastPush = System.currentTimeMillis();

            RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
            if (recordGroup == null) {
                Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                run = false;
                return;
            }
            updateLog("Archivemanagement database loaded.");
            IEadEntry rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());

            List<IEadEntry> allNodes = rootElement.getAllNodes();
            updateLog("Found " + allNodes.size() + " nodes to export.");

            try (Client client = ClientBuilder.newClient()) {
                updateLog("Try to authenticate.");
                AuthenticationToken token = ActaProApi.authenticate(client, authServiceHeader, authServiceUrl, authServiceUsername,
                        authServicePassword);
                updateLog("Authenticated.");

                for (IEadEntry entry : allNodes) {

                    NodeInitializer.initEadNodeWithMetadata(entry, getConfig().getConfiguredFields());
                    updateLog("Node with id '" + entry.getId() + "' loaded.");
                    // check if id field exists
                    String nodeId = null;
                    for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                        if (emf.getName().equals(identifierFieldName)) {
                            nodeId = emf.getValues().get(0).getValue();
                        }
                    }
                    if (StringUtils.isNotBlank(nodeId)) {
                        updateLog("Update existing ACTApro document, it has the ID " + nodeId);

                        // if yes -> find document
                        Document doc = ActaProApi.getDocumentByKey(client, token, connectorUrl, nodeId);
                        if (doc == null) {
                            updateLog("Skip node as the id cannot be found in ACTApro");
                            continue;
                        }
                        // check if parent is still the same
                        updateParentDocument(entry, doc);

                        // update doc title
                        for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                            if ("unittitle".equals(emf.getName())) {
                                doc.setDocTitle(emf.getValues().get(0).getValue());
                            }
                        }

                        if (writeMetadata(entry, doc)) {

                            // update document
                            ActaProApi.updateDocument(client, token, connectorUrl, doc);
                        }

                    } else if (entry.getParentNode() != null) {
                        // only sub elements are allowed, creating new root nodes is not supported
                        updateLog("Node does not exist yet, create a new ACTApro document.");
                        // if not -> create new document
                        Document doc = new Document();
                        doc.setObject("document");

                        // set  node type
                        for (Entry<String, INodeType> e : nodes.entrySet()) {
                            if (e.getValue().getNodeName().equals(entry.getNodeType().getNodeName())) {
                                doc.setType(e.getKey());
                            }
                        }

                        // set doc title
                        for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                            if ("unittitle".equals(emf.getName())) {
                                doc.setDocTitle(emf.getValues().get(0).getValue());
                            }
                        }

                        DocumentBlock block = new DocumentBlock();
                        doc.setBlock(block);
                        block.setType(doc.getType());

                        String parentDocKey = null;
                        IEadEntry parent = entry.getParentNode();
                        NodeInitializer.initEadNodeWithMetadata(parent, getConfig().getConfiguredFields());
                        for (IMetadataField emf : parent.getIdentityStatementAreaList()) {
                            if (emf.getName().equals(identifierFieldName)) {
                                parentDocKey = emf.getValues().get(0).getValue();
                            }
                        }

                        // Ref_Gp group: order (Ref_DocOrder),parent (Ref_DocKey, Ref_Doctype), Ref_Type=P

                        DocumentField refDocKeyField = new DocumentField();
                        refDocKeyField.setType("Ref_DocKey");
                        refDocKeyField.setValue(parentDocKey);
                        block.addFieldsItem(refDocKeyField);

                        DocumentField refDocOrderField = new DocumentField();
                        refDocOrderField.setType("Ref_DocOrder");
                        refDocOrderField.setValue(String.valueOf(entry.getOrderNumber()));
                        block.addFieldsItem(refDocOrderField);

                        for (Entry<String, INodeType> e : nodes.entrySet()) {
                            if (e.getValue().getNodeName().equals(entry.getNodeType().getNodeName())) {
                                DocumentField refDocTypeField = new DocumentField();
                                refDocTypeField.setType("Ref_Doctype");
                                refDocTypeField.setValue(e.getKey());
                                block.addFieldsItem(refDocTypeField);
                            }
                        }

                        // add metadata
                        writeMetadata(entry, doc);

                        // create required fields:

                        doc.setOwnerId(documentOwner);
                        doc.setCreatorID(documentOwner);
                        doc.setCreationDate(documentDateFormatter.format(LocalDateTime.now()));
                        doc.setChangeDate(documentDateFormatter.format(LocalDateTime.now()));

                        // insert as new doc
                        doc = ActaProApi.createDocument(client, token, connectorUrl, parentDocKey, doc);
                        // If doc is null, the upload  has failed, probably because the document is temporarily locked or there is a conflict.
                        if (doc != null) {
                            // get id from response document
                            String newDocumentKey = doc.getDocKey();
                            // save generated id
                            for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                if (emf.getName().equals(identifierFieldName)) {
                                    emf.getValues().get(0).setValue(newDocumentKey);
                                    // if process exists, write newDocumentKey to metadata
                                    if (StringUtils.isNotBlank(entry.getGoobiProcessTitle())) {
                                        Process goobiProcess = ProcessManager.getProcessByExactTitle(entry.getGoobiProcessTitle());
                                        if (goobiProcess != null) {
                                            try {
                                                Fileformat ff = goobiProcess.readMetadataFile();
                                                DocStruct logical = ff.getDigitalDocument().getLogicalDocStruct();
                                                if (logical.getType().isAnchor()) {
                                                    logical = logical.getAllChildren().get(0);
                                                }
                                                // check if metadata already exists, update value
                                                boolean metadataUpdated = false;
                                                for (Metadata md : logical.getAllMetadata()) {
                                                    if (md.getType().getName().equals(emf.getMetadataName())) {
                                                        md.setValue(newDocumentKey);
                                                        metadataUpdated = true;
                                                        break;
                                                    }
                                                }

                                                // or create a new field
                                                if (!metadataUpdated) {
                                                    Metadata md = new Metadata(
                                                            goobiProcess.getRegelsatz()
                                                                    .getPreferences()
                                                                    .getMetadataTypeByName(emf.getMetadataName()));
                                                    md.setValue(newDocumentKey);
                                                    logical.addMetadata(md);
                                                }
                                                goobiProcess.writeMetadataFile(ff);
                                            } catch (UGHException | IOException | SwapException e1) {
                                                log.error(e1);
                                            }

                                        }
                                    }
                                }
                            }
                        }

                        ArchiveManagementManager.saveNode(recordGroup.getId(), entry);

                    }
                }
            }

            updateLog("Export finished.");
            run = false;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error(e);
            }
            if (pusher != null) {
                pusher.send("update");
            }
        };
        new Thread(runnable).start();
    }

    private boolean writeMetadata(IEadEntry entry, Document doc) {

        boolean metadataChanged = false;

        // for each configured field
        for (MetadataMapping mm : metadataFields) {
            // find node metadata
            String value = getNodeMetadataVaue(mm, entry);

            // find documentField
            if (ActaProApi.updateDocumentField(doc, mm, value)) {
                metadataChanged = true;
            }

        }

        return metadataChanged;
    }

    private String getNodeMetadataVaue(MetadataMapping mm, IEadEntry entry) {
        switch (mm.getEadArea()) {
            case "1":
                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }

                break;
            case "2":
                for (IMetadataField emf : entry.getContextAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            case "3":
                for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            case "4":
                for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            case "5":
                for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            case "6":
                for (IMetadataField emf : entry.getNotesAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            case "7":
                for (IMetadataField emf : entry.getDescriptionControlAreaList()) {
                    if (StringUtils.isNotBlank(mm.getEadGroup())) {
                        if (emf.getName().equals(mm.getEadGroup())) {
                            IMetadataGroup grp = emf.getGroups().get(0);
                            for (IMetadataField f : grp.getFields()) {
                                if (f.getName().equals(mm.getEadField())) {
                                    return f.getValues().get(0).getValue();
                                }
                            }
                        }
                    } else if (emf.getName().equals(mm.getEadField())) {
                        return emf.getValues().get(0).getValue();
                    }
                }
                break;
            default:
                // do nothing
        }

        return null;

    }

    private void updateParentDocument(IEadEntry entry, Document doc) {
        String parentNodeId = null;
        for (DocumentField df : doc.getBlock().getFields()) {
            if ("Ref_DocKey".equals(df.getType())) {
                parentNodeId = df.getValue();
            }
        }

        // ignore root element
        if (StringUtils.isNotBlank(parentNodeId) && entry.getParentNode() != null) {
            Integer parentEntryId = ArchiveManagementManager.findNodeById(identifierFieldName, parentNodeId);
            if (parentEntryId.intValue() != entry.getParentNode().getDatabaseId()) {
                // parent node was changed
                IEadEntry parent = entry.getParentNode();
                // update Ref_DocKey, Ref_DocOrder fields
                NodeInitializer.initEadNodeWithMetadata(parent, getConfig().getConfiguredFields());
                String newParentNodeId = null;
                for (IMetadataField emf : parent.getIdentityStatementAreaList()) {
                    if (emf.getName().equals(identifierFieldName)) {
                        newParentNodeId = emf.getValues().get(0).getValue();
                    }
                }
                for (DocumentField df : doc.getBlock().getFields()) {
                    if ("Ref_DocKey".equals(df.getType())) {
                        df.setValue(newParentNodeId);
                    } else if ("Ref_DocOrder".equals(df.getType())) {
                        df.setValue(String.valueOf(entry.getOrderNumber()));
                    } else if ("Ref_Doctype".equals(df.getType())) {
                        for (Entry<String, INodeType> e : nodes.entrySet()) {
                            if (e.getValue().getNodeName().equals(entry.getNodeType().getNodeName())) {
                                df.setValue(e.getKey());
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * 
     * Find all documents created or modified after a given date
     *
     * @param client
     * @param token
     * @param date in format '2023-04-14T15:33:44Z'
     * @return
     */

    private List<Document> findDocuments(Client client, AuthenticationToken token, String rootElementId) {

        DocumentSearchParams searchRequest = new DocumentSearchParams();

        searchRequest.query("*");
        if (startDate != null) {
            searchRequest.getFields().add("crdate");

            DocumentSearchFilter filter = new DocumentSearchFilter();
            filter.fieldName("crdate");
            filter.setOperator(OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
            filter.fieldValue(requestDateFormatter.format(startDate) + "T00:00:00Z");
            searchRequest.addFiltersItem(filter);

            if (endDate != null) {
                filter = new DocumentSearchFilter();
                filter.fieldName("crdate");
                filter.setOperator(OperatorEnum.LESS_THAN_OR_EQUAL_TO);
                filter.fieldValue(requestDateFormatter.format(endDate) + "T23:59:59Z");
                searchRequest.addFiltersItem(filter);
            }
        }
        // doctype vz and parentid = xy
        searchRequest.addDocumentTypesItem("Arch");
        searchRequest.addDocumentTypesItem("Tekt");
        searchRequest.addDocumentTypesItem("Best");
        searchRequest.addDocumentTypesItem("Klas");
        searchRequest.addDocumentTypesItem("Ser");
        searchRequest.addDocumentTypesItem("Vz");

        updateLog("Search for documents");

        List<Document> documents = new ArrayList<>();
        // post request
        boolean isLast = false;
        int currentPage = 1;
        while (!isLast) {
            WebTarget target = client.target(connectorUrl).path("documents").queryParam("page", currentPage);
            Invocation.Builder builder = target.request();
            builder.header("Accept", "application/json");
            builder.header("Authorization", "Bearer " + token.getAccessToken());

            Response response = builder.post(Entity.entity(searchRequest, MediaType.APPLICATION_JSON));

            if (200 == response.getStatus()) {

                SearchResultPage srp = response.readEntity(SearchResultPage.class);
                List<Map<String, String>> contentMap = srp.getContent();
                for (Map<String, String> content : contentMap) {
                    if (content.get("path").contains(rootElementId)) {
                        int retry = MAX_DOCUMENT_IMPORT_RETRIES;
                        boolean success = false;
                        String id = content.get("id");
                        while (retry > 0) {
                            Document doc = ActaProApi.getDocumentByKey(client, token, connectorUrl, id);
                            // TODO: Improve error handling instead of null values!
                            if (doc == null) {
                                log.error("Unable to retrieve document with id '{}', retrying {} more times", id, retry);
                                updateLog("Unable to retrieve document with id '" + id + "', retrying " + retry + " more times");
                                if (id.startsWith("Vz")) {
                                    // if we found the deepest hierarchy type, we set success to true, so that the entire import does not fail.
                                    // The individual document cannot be imported, but the import itself can continue.
                                    // But if an element from a higher hierarchy fails, we abort because we cannot build a tree without this node.
                                    success = true;
                                }
                                try {
                                    Thread.sleep(MAX_DOCUMENT_IMPORT_RETRY_DELAY);
                                } catch (InterruptedException e) {
                                    log.error("Failed to wait for retry delay", e);
                                    throw new RuntimeException(e);
                                }
                                retry--;
                            } else {
                                doc.setPath(content.get("path"));
                                // only add documents from the selected archive
                                documents.add(doc);
                                success = true;
                                break;
                            }
                        }
                        if (!success) {
                            log.error("Unable to retrieve document with id '{}'.", id);
                            return Collections.emptyList();
                        }
                    }
                }

                isLast = srp.getLast();
                currentPage++;
                if (currentPage % 10 == 0 || isLast) {
                    updateLog("Found " + documents.size() + " documents on " + currentPage + " page(s) of " + srp.getTotalPages() + " page(s) - ["
                            + LocalDateTime.now().format(logDateFormatter) + "]");
                }
            } else {
                ErrorResponse error = response.readEntity(ErrorResponse.class);
                log.error("Search error, status: {}, text: {} ", error.getStatus(), error.getMessage());
                updateLog("Search error, status: " + error.getStatus() + ", text: " + error.getMessage());
                isLast = true;
            }
        }
        log.debug("Found {} documents on {} pages.", documents.size(), currentPage);
        return documents;
    }

    private void saveValue(MetadataMapping matchedMapping, String value, IMetadataField emf) {
        if (StringUtils.isNotBlank(matchedMapping.getEadGroup())) {
            if (emf.getName().equals(matchedMapping.getEadGroup())) {
                IMetadataGroup grp = emf.getGroups().get(0);
                for (IMetadataField f : grp.getFields()) {
                    if (f.getName().equals(matchedMapping.getEadField())) {
                        if (!f.getValues().isEmpty()) {
                            f.getValues().get(0).setValue(value);
                        } else {
                            f.addValue();
                            f.getValues().get(0).setValue(value);
                        }
                    }
                }
            }
        } else if (emf.getName().equals(matchedMapping.getEadField())) {
            if (!emf.getValues().isEmpty()) {
                emf.getValues().get(0).setValue(value);
            } else {
                emf.addValue();
                emf.getValues().get(0).setValue(value);
            }
        }
    }

    public static void main(String[] args) throws StreamWriteException, DatabindException, FileNotFoundException, IOException {

        ActaProSyncAdministrationPlugin plugin = new ActaProSyncAdministrationPlugin();
        try (Client client = ClientBuilder.newClient()) {
            AuthenticationToken token = ActaProApi.authenticate(client, plugin.authServiceHeader, plugin.authServiceUrl, plugin.authServiceUsername,
                    plugin.authServicePassword);
            Document doc = ActaProApi.getDocumentByKey(client, token, plugin.connectorUrl, "Vz      1ce4eedf-ed41-407c-bd09-c3c0d6d3c2f2");

            // change a field

            for (DocumentField df : doc.getBlock().getFields()) {
                if ("Vz_Bez".equals(df.getType())) {
                    df.setValue(df.getValue() + " - test");
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new FileOutputStream("/tmp/bla.json"), doc);

            // upload document
            ActaProApi.updateDocument(client, token, plugin.connectorUrl, doc);
        }

    }

    @Override
    public void setPushContext(PushContext pusher) {
        this.pusher = pusher;
    }

    private void updateLog(String logmessage) {
        logQueue.add(logmessage);
        if (pusher != null && System.currentTimeMillis() - lastPush > 500) {
            lastPush = System.currentTimeMillis();
            pusher.send("update");
        }
    }
}
