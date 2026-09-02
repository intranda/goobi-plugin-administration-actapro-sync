package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.configuration.ConfigurationException;
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
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.omnifaces.cdi.PushContext;

import de.intranda.goobi.plugins.model.RecordGroup;
import de.intranda.goobi.plugins.persistence.ArchiveManagementManager;
import de.intranda.goobi.plugins.persistence.NodeInitializer;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
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
import jakarta.annotation.PreDestroy;
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

    private static final long serialVersionUID = 2632106883746583247L;

    private static final Namespace H1_NS = Namespace.getNamespace("h1", "http://www.startext.de/HiDA/DefService/XMLSchema");

    @Getter
    private String title = "intranda_administration_actapro_sync";

    @Getter
    private PluginType type = PluginType.Administration;

    @Getter
    private String gui = "/uii/plugin_administration_actapro_sync.xhtml";

    @Getter
    @Setter
    private LocalDate startDate = LocalDate.now();

    @Getter
    @Setter
    private LocalDate endDate;

    @Getter
    @Setter
    private String database;

    @Getter
    private ActaProApiConfiguration actaProConfig;

    private DateTimeFormatter documentDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss:SSS");

    private DateTimeFormatter requestDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private long lastPush = System.currentTimeMillis();

    private PushContext pusher;
    @Getter
    private Queue<String> logQueue = new CircularFifoQueue<>(48);

    @Getter
    private final AtomicBoolean run = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        actaProConfig = new ActaProApiConfiguration();

        updateLog("Configuration successfully read");

    }

    public void xmlImport() {
        if (!run.compareAndSet(false, true)) {
            updateLog("Previous import is still running, abort");
            return;
        }

        String databaseName = database;
        if (StringUtils.isBlank(databaseName) || "null".equals(databaseName)) {
            Helper.setFehlerMeldung("intranda_administration_actapro_noDatabase");
            run.set(false);
            return;
        }

        String rootElementID = actaProConfig.getRootId(databaseName);
        updateLog("Start XML import for inventory " + databaseName);
        updateLog("Root element ID: " + rootElementID);

        executor.submit(() -> {
            try {
                run.set(true);
                lastPush = System.currentTimeMillis();

                RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
                if (recordGroup == null) {
                    Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                    run.set(false);
                    return;
                }
                updateLog("Archive database loaded.");

                Path importDir = Paths.get(actaProConfig.getXmlImportFolder());
                if (!Files.isDirectory(importDir)) {
                    updateLog("No XML files found in " + actaProConfig.getXmlImportFolder());
                    run.set(false);
                    return;
                }
                List<Path> allXmlPaths = new ArrayList<>();
                try (Stream<Path> stream = Files.list(importDir)) {
                    allXmlPaths = stream.filter(p -> p.getFileName().toString().endsWith(".xml"))
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .collect(Collectors.toList());
                }
                if (allXmlPaths.isEmpty()) {
                    updateLog("No XML files found in " + actaProConfig.getXmlImportFolder());
                    run.set(false);
                    return;
                }

                List<Path> xmlFiles = new ArrayList<>();
                if (StringUtils.isNotBlank(actaProConfig.getXmlTectonicsFile())) {
                    Path tectonicsFile = importDir.resolve(actaProConfig.getXmlTectonicsFile());
                    if (Files.exists(tectonicsFile)) {
                        xmlFiles.add(tectonicsFile);
                        updateLog("Added tectonics file: " + actaProConfig.getXmlTectonicsFile());
                    }
                }
                for (Path p : allXmlPaths) {
                    if (!p.getFileName().toString().equals(actaProConfig.getXmlTectonicsFile())) {
                        xmlFiles.add(p);
                    }
                }
                updateLog("Found " + xmlFiles.size() + " XML files to process.");

                // Pass 1: build lightweight parent index (only DocKey -> parentDocKey strings)
                Map<String, String> parentIndex = new LinkedHashMap<>();
                int totalDocCount = 0;
                for (Path xmlFile : xmlFiles) {
                    try {
                        int count = buildParentIndex(xmlFile, parentIndex);
                        totalDocCount += count;
                        updateLog("Indexed " + count + " documents from " + xmlFile.getFileName());
                    } catch (Exception e) {
                        log.error("Error indexing {}: {}", xmlFile.getFileName(), e.getMessage(), e);
                        updateLog("Error indexing " + xmlFile.getFileName() + ": " + e.getMessage());
                    }
                }
                updateLog("Index built: " + totalDocCount + " documents in " + xmlFiles.size() + " files.");

                // Load existing nodes into cache to avoid per-document DB lookups
                Map<String, Integer> nodeIdCache = NodeApi.loadNodeIdCache(recordGroup, actaProConfig.getIdentifierFieldName());
                updateLog("Node ID cache loaded: " + nodeIdCache.size() + " existing entries.");

                // Pass 2: import file by file, keeping only one file's documents in memory
                int imported = 0;
                for (Path xmlFile : xmlFiles) {
                    updateLog("Importing: " + xmlFile.getFileName());
                    try {
                        Map<String, io.goobi.api.job.actapro.model.Document> fileDocs = parseXmlDocumentSet(xmlFile);
                        for (io.goobi.api.job.actapro.model.Document doc : fileDocs.values()) {
                            doc.setPath(buildXmlPath(doc.getDocKey(), parentIndex));
                        }
                        for (io.goobi.api.job.actapro.model.Document doc : fileDocs.values()) {
                            try {
                                importDocumentFromXml(doc, recordGroup, rootElementID, fileDocs, nodeIdCache);
                                imported++;
                                if (imported % 100 == 0) {
                                    updateLog("Imported " + imported + " of " + totalDocCount + " documents");
                                }
                            } catch (Exception e) {
                                log.error("Error importing {}: {}", doc.getDocKey(), e.getMessage(), e);
                                updateLog("Error importing " + doc.getDocKey() + ": " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing {}: {}", xmlFile.getFileName(), e.getMessage(), e);
                        updateLog("Error processing " + xmlFile.getFileName() + ": " + e.getMessage());
                    }
                }

                updateLog("XML import finished. Processed " + imported + " documents.");
            } catch (Exception e) {
                log.error("Uncaught exception in XML import: {}", e.getMessage(), e);
                updateLog("XML import error: " + e.getMessage());
            } finally {
                run.set(false);
            }
        });
    }

    static int buildParentIndex(Path xmlFile, Map<String, String> parentIndex) throws JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        org.jdom2.Document xmlDoc = saxBuilder.build(xmlFile.toFile());
        Element root = xmlDoc.getRootElement();
        int count = 0;
        for (Element docEl : root.getChildren("Document", H1_NS)) {
            String docKey = docEl.getAttributeValue("DocKey");
            if (StringUtils.isBlank(docKey) || parentIndex.containsKey(docKey)) {
                continue;
            }
            String parentDocKey = null;
            Element blockEl = docEl.getChild("Block", H1_NS);
            if (blockEl != null) {
                outer: for (Element fieldEl : blockEl.getChildren("Field", H1_NS)) {
                    if ("Ref_Gp".equals(fieldEl.getAttributeValue("Type"))) {
                        for (Element subField : fieldEl.getChildren("Field", H1_NS)) {
                            if ("Ref_DocKey".equals(subField.getAttributeValue("Type"))) {
                                parentDocKey = subField.getAttributeValue("Value");
                                break outer;
                            }
                        }
                    }
                }
            }
            parentIndex.put(docKey, parentDocKey);
            count++;
        }
        return count;
    }

    static Map<String, io.goobi.api.job.actapro.model.Document> parseXmlDocumentSet(Path xmlFile) throws JDOMException, IOException {
        Map<String, io.goobi.api.job.actapro.model.Document> result = new LinkedHashMap<>();
        SAXBuilder saxBuilder = new SAXBuilder();
        org.jdom2.Document xmlDoc = saxBuilder.build(xmlFile.toFile());
        Element root = xmlDoc.getRootElement();

        for (Element docEl : root.getChildren("Document", H1_NS)) {
            io.goobi.api.job.actapro.model.Document doc = new io.goobi.api.job.actapro.model.Document();
            doc.setDocKey(docEl.getAttributeValue("DocKey"));
            doc.setDocTitle(docEl.getAttributeValue("DocTitle"));
            doc.setCreatorID(docEl.getAttributeValue("CreatorID"));
            doc.setOwnerId(docEl.getAttributeValue("OwnerID"));
            doc.setCreationDate(docEl.getAttributeValue("CreationDate"));
            doc.setChangeDate(docEl.getAttributeValue("ChangeDate"));

            Element blockEl = docEl.getChild("Block", H1_NS);
            if (blockEl != null) {
                DocumentBlock block = new DocumentBlock();
                block.setType(blockEl.getAttributeValue("Type"));
                doc.setType(blockEl.getAttributeValue("Type"));
                for (Element fieldEl : blockEl.getChildren("Field", H1_NS)) {
                    block.addFieldsItem(parseXmlField(fieldEl));
                }
                doc.setBlock(block);
            }

            if (StringUtils.isNotBlank(doc.getDocKey())) {
                result.put(doc.getDocKey(), doc);
            }
        }
        return result;
    }

    static DocumentField parseXmlField(Element fieldEl) {
        DocumentField field = new DocumentField();
        field.setType(fieldEl.getAttributeValue("Type"));
        field.setValue(fieldEl.getAttributeValue("Value"));
        field.setPlainValue(fieldEl.getAttributeValue("value_plain"));
        for (Element childEl : fieldEl.getChildren("Field", H1_NS)) {
            field.addFieldsItem(parseXmlField(childEl));
        }
        return field;
    }

    static String buildXmlPath(String docKey, Map<String, String> parentIndex) {
        List<String> pathParts = new ArrayList<>();
        String current = docKey;
        Set<String> visited = new HashSet<>();
        while (current != null && !visited.contains(current)) {
            pathParts.add(0, current);
            visited.add(current);
            current = parentIndex.get(current);
        }
        return String.join("; ", pathParts);
    }

    static String getParentDocKey(io.goobi.api.job.actapro.model.Document doc) {
        if (doc == null || doc.getBlock() == null) {
            return null;
        }
        for (DocumentField field : doc.getBlock().getFields()) {
            if ("Ref_Gp".equals(field.getType())) {
                for (DocumentField subfield : field.getFields()) {
                    if ("Ref_DocKey".equals(subfield.getType())) {
                        return subfield.getValue();
                    }
                }
            }
        }
        return null;
    }

    public void downloadFromActaPro() {
        // 1.) search for document with root id, initialize and import it
        // 2.) search for all documents with the root id as parent id
        // 3.) for each found document: initialize and import it
        // 4.) if doc type is not Vz: search for documents with current id as parent id and repeat step 3+4

        if (!run.compareAndSet(false, true)) {
            // abort, if import is running
            updateLog("Previous import is still running, abort");

            return;
        }

        String databaseName = database;

        if (StringUtils.isBlank(databaseName) || "null".equals(databaseName)) {
            Helper.setFehlerMeldung("intranda_administration_actapro_noDatabase");
            run.set(false);

            return;
        }
        String rootElementID = actaProConfig.getRootId(database);

        // check if database exist, load it

        updateLog("Start import for inventory " + databaseName);
        updateLog("ACTApro root element is " + rootElementID);
        executor.submit(() -> {
            try {
                run.set(true);
                lastPush = System.currentTimeMillis();
                RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
                if (recordGroup == null) {
                    Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                    run.set(false);
                    return;
                }

                updateLog("Archivemanagement database loaded.");
                Map<String, Integer> nodeIdCache = NodeApi.loadNodeIdCache(recordGroup, actaProConfig.getIdentifierFieldName());
                updateLog("Node ID cache loaded: " + nodeIdCache.size() + " existing entries.");
                try (Client client = ClientBuilder.newClient()) {
                    updateLog("Try to authenticate.");
                    AuthenticationToken token = ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(),
                            actaProConfig.getAuthServiceUrl(), actaProConfig.getAuthServiceUsername(),
                            actaProConfig.getAuthServicePassword());
                    updateLog("Authenticated.");
                    Document doc = null;
                    try {
                        doc = ActaProApi.getDocumentByKey(client, token, actaProConfig.getConnectorUrl(), rootElementID);
                    } catch (IOException e) {
                        log.error(e);
                        updateLog("API connection error, abort.");
                        return;
                    }
                    if (doc == null) {
                        updateLog("Root document with id " + rootElementID + " not found, abort.");
                        return;
                    }
                    String documentId = importDocument(client, doc, recordGroup, rootElementID, token, nodeIdCache);
                    if (StringUtils.isNotBlank(documentId) && !documentId.startsWith("Vz")) {
                        importDocuments(client, token, documentId, recordGroup, rootElementID, nodeIdCache);
                    }

                } catch (IOException e1) {
                    log.error(e1);
                }

                updateLog("Imported all documents");
            } catch (Exception e) {
                log.error("Uncaught exception im Import: {}", e.getMessage(), e);
                updateLog("Import error: " + e.getMessage());
            } finally {
                run.set(false);
            }
        });
    }

    public void uploadToActaPro() {
        if (!run.compareAndSet(false, true)) {
            // abort, if import is running
            logQueue.add("Previous export is still running, abort");
            return;
        }

        // load database

        String databaseName = database;
        updateLog("Start export for inventory " + databaseName);
        // check if database exist, load it

        executor.submit(() -> {
            try {
                run.set(true);
                lastPush = System.currentTimeMillis();

                RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
                if (recordGroup == null) {
                    Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                    run.set(false);
                    return;
                }
                updateLog("Archivemanagement database loaded.");
                IEadEntry rootElement = ArchiveManagementManager.loadRecordGroup(recordGroup.getId());

                List<IEadEntry> allNodes = rootElement.getAllNodes();
                updateLog("Found " + allNodes.size() + " nodes to export.");

                try (Client client = ClientBuilder.newClient()) {
                    updateLog("Try to authenticate.");
                    AuthenticationToken token =
                            ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(), actaProConfig.getAuthServiceUrl(),
                                    actaProConfig.getAuthServiceUsername(), actaProConfig.getAuthServicePassword());
                    updateLog("Authenticated.");

                    for (IEadEntry entry : allNodes) {

                        try {
                            NodeInitializer.initEadNodeWithMetadata(entry, actaProConfig.getConfig().getConfiguredFields());
                            updateLog("Node with id '" + entry.getId() + "' loaded.");
                            // check if id field exists
                            String nodeId = null;
                            for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                if (emf.getName().equals(actaProConfig.getIdentifierFieldName())) {
                                    nodeId = emf.getValues().get(0).getValue();
                                }
                            }
                            if (StringUtils.isNotBlank(nodeId)) {
                                updateLog("Update existing ACTApro document, it has the ID " + nodeId);

                                // if yes -> find document
                                Document doc = null;
                                try {
                                    doc = ActaProApi.getDocumentByKey(client, token, actaProConfig.getConnectorUrl(), nodeId);
                                } catch (IOException e) {
                                    log.error(e);
                                }
                                if (doc == null) {
                                    updateLog("Skip node as the id cannot be found in ACTApro");
                                    continue;
                                }
                                // check if parent is still the same
                                updateParentDocument(entry, doc);

                                // update doc title
                                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                    if ("unittitle".equals(emf.getName()) && StringUtils.isBlank(doc.getDocTitle())) {
                                        doc.setDocTitle(emf.getValues().get(0).getValue());
                                    }
                                }

                                if (writeMetadata(entry, doc)) {

                                    // update document
                                    ActaProApi.updateDocument(client, token, actaProConfig.getConnectorUrl(), doc);
                                }

                            } else if (entry.getParentNode() != null) {
                                // only sub elements are allowed, creating new root nodes is not supported
                                updateLog("Node does not exist yet, create a new ACTApro document.");
                                // if not -> create new document
                                Document doc = new Document();
                                doc.setObject("document");

                                // set  node type
                                for (Entry<String, INodeType> e : actaProConfig.getNodes().entrySet()) {
                                    if (e.getValue().getNodeName().equals(entry.getNodeType().getNodeName())) {
                                        doc.setType(e.getKey());
                                    }
                                }

                                // set doc title
                                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                    if ("unittitle".equals(emf.getName()) && StringUtils.isBlank(doc.getDocTitle())) {
                                        doc.setDocTitle(emf.getValues().get(0).getValue());
                                    }
                                }

                                DocumentBlock block = new DocumentBlock();
                                doc.setBlock(block);
                                block.setType(doc.getType());

                                String parentDocKey = null;
                                IEadEntry parent = entry.getParentNode();
                                try {
                                    NodeInitializer.initEadNodeWithMetadata(parent, actaProConfig.getConfig().getConfiguredFields());
                                    for (IMetadataField emf : parent.getIdentityStatementAreaList()) {
                                        if (emf.getName().equals(actaProConfig.getIdentifierFieldName())) {
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

                                    for (Entry<String, INodeType> e : actaProConfig.getNodes().entrySet()) {
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

                                    doc.setOwnerId(actaProConfig.getDocumentOwner());
                                    doc.setCreatorID(actaProConfig.getDocumentOwner());
                                    doc.setCreationDate(documentDateFormatter.format(LocalDateTime.now()));
                                    doc.setChangeDate(documentDateFormatter.format(LocalDateTime.now()));

                                    // insert as new doc
                                    doc = ActaProApi.createDocument(client, token, actaProConfig.getConnectorUrl(), parentDocKey, doc);
                                    // If doc is null, the upload  has failed, probably because the document is temporarily locked or there is a conflict.
                                    if (doc != null) {
                                        // get id from response document
                                        String newDocumentKey = doc.getDocKey();
                                        // save generated id
                                        for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                                            if (emf.getName().equals(actaProConfig.getIdentifierFieldName())) {
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
                                } finally {
                                    NodeApi.clearNode(parent);
                                }

                                ArchiveManagementManager.saveNode(recordGroup.getId(), entry);
                            }
                        } finally {
                            NodeApi.clearNode(entry);
                        }
                    }
                } finally {
                    if (allNodes != null) {
                        allNodes.clear();
                    }
                    rootElement = null;
                }
                updateLog("Export finished.");
                run.set(false);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error(e);
                }
                if (pusher != null) {
                    pusher.send("update");
                }
            } finally {
                run.set(false);
            }
        });
    }

    private boolean writeMetadata(IEadEntry entry, Document doc) {

        boolean metadataChanged = false;

        // for each configured field
        for (MetadataMapping mm : actaProConfig.getMetadataFields()) {
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
            Integer parentEntryId = ArchiveManagementManager.findNodeById(actaProConfig.getIdentifierFieldName(), parentNodeId);

            if (parentEntryId.intValue() != entry.getParentNode().getDatabaseId()) {
                // parent node was changed
                IEadEntry parent = entry.getParentNode();
                // update Ref_DocKey, Ref_DocOrder fields
                try {
                    NodeInitializer.initEadNodeWithMetadata(parent, actaProConfig.getConfig().getConfiguredFields());
                    String newParentNodeId = null;
                    for (IMetadataField emf : parent.getIdentityStatementAreaList()) {
                        if (emf.getName().equals(actaProConfig.getIdentifierFieldName())) {
                            newParentNodeId = emf.getValues().get(0).getValue();
                        }
                    }
                    for (DocumentField df : doc.getBlock().getFields()) {
                        if ("Ref_DocKey".equals(df.getType())) {
                            df.setValue(newParentNodeId);
                        } else if ("Ref_DocOrder".equals(df.getType())) {
                            df.setValue(String.valueOf(entry.getOrderNumber()));
                        } else if ("Ref_Doctype".equals(df.getType())) {
                            for (Entry<String, INodeType> e : actaProConfig.getNodes().entrySet()) {
                                if (e.getValue().getNodeName().equals(entry.getNodeType().getNodeName())) {
                                    df.setValue(e.getKey());
                                }
                            }

                        }
                    }
                } finally {
                    NodeApi.clearNode(parent);
                }
            }
        }
    }

    private void importDocuments(Client client, AuthenticationToken token, String parentId, RecordGroup recordGroup,
            String rootElementID, Map<String, Integer> nodeIdCache) throws IOException {
        Queue<String> toProcess = new LinkedList<>();
        toProcess.add(parentId);

        Instant tokenObtainedAt = Instant.now();

        while (!toProcess.isEmpty()) {
            // Proactively renew token 60 seconds before it expires
            if (token != null && token.getExpiresIn() > 0
                    && Instant.now().isAfter(tokenObtainedAt.plusSeconds(token.getExpiresIn() - 60))) {
                log.info("Token about to expire, renewing proactively");
                token = ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(), actaProConfig.getAuthServiceUrl(),
                        actaProConfig.getAuthServiceUsername(),
                        actaProConfig.getAuthServicePassword());
                tokenObtainedAt = Instant.now();
            }

            String current = toProcess.poll();
            try {
                List<String> children = searchChildren(client, token, current, recordGroup, rootElementID, nodeIdCache, false);
                toProcess.addAll(children);
            } catch (UnauthorizedException e) {
                log.warn("Token expired while processing '{}', re-authenticating and retrying", current);
                token = ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(), actaProConfig.getAuthServiceUrl(),
                        actaProConfig.getAuthServiceUsername(), actaProConfig.getAuthServicePassword());
                tokenObtainedAt = Instant.now();
                if (token != null) {
                    toProcess.add(current);
                } else {
                    log.error("Re-authentication failed, skipping document '{}'", current);
                }
            }
        }
    }

    public void partialUpdate() {

        if (!run.compareAndSet(false, true)) {
            // abort, if import is running
            updateLog("Previous import is still running, abort");
            return;
        }

        String databaseName = database;

        if (StringUtils.isBlank(databaseName) || "null".equals(databaseName)) {
            Helper.setFehlerMeldung("intranda_administration_actapro_noDatabase");
            run.set(false);

            return;
        }
        String rootElementID = actaProConfig.getRootId(database);
        // check if database exist, load it
        updateLog("Start import for inventory " + databaseName);
        updateLog("ACTApro root element is " + rootElementID);
        executor.submit(() -> {
            try {
                run.set(true);
                lastPush = System.currentTimeMillis();
                RecordGroup recordGroup = ArchiveManagementManager.getRecordGroupByTitle(databaseName);
                if (recordGroup == null) {
                    Helper.setFehlerMeldung("intranda_administration_actapro_databaseNotFound");
                    run.set(false);
                    return;
                }

                updateLog("Archivemanagement database loaded.");
                Map<String, Integer> nodeIdCache = NodeApi.loadNodeIdCache(recordGroup, actaProConfig.getIdentifierFieldName());
                updateLog("Node ID cache loaded: " + nodeIdCache.size() + " existing entries.");
                try (Client client = ClientBuilder.newClient()) {
                    updateLog("Try to authenticate.");
                    AuthenticationToken token = ActaProApi.authenticate(client, actaProConfig.getAuthServiceHeader(),
                            actaProConfig.getAuthServiceUrl(), actaProConfig.getAuthServiceUsername(),
                            actaProConfig.getAuthServicePassword());
                    updateLog("Authenticated.");

                    searchChildren(client, token, rootElementID, recordGroup, rootElementID, nodeIdCache, true);

                } catch (IOException e1) {
                    log.error(e1);
                }

                updateLog("Imported all documents");
            } catch (Exception e) {
                log.error("Uncaught exception im Import: {}", e.getMessage(), e);
                updateLog("Import error: " + e.getMessage());
            } finally {
                run.set(false);
            }
        });

    }

    /**
     * 
     * Find all documents created or modified after a given date
     *
     * @param client
     * @param token
     * @param date in format '2023-04-14T15:33:44Z'
     * @return
     * @throws IOException
     */

    private List<String> searchChildren(Client client, AuthenticationToken token, String parentId, RecordGroup recordGroup,
            String rootElementID, Map<String, Integer> nodeIdCache, boolean partialUpdate) throws IOException {

        List<String> answer = new ArrayList<>();

        DocumentSearchParams searchRequest = new DocumentSearchParams();

        searchRequest.query("*");
        if (startDate != null) {
            DocumentSearchFilter filter = new DocumentSearchFilter();
            filter.fieldName("chdate");
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

        if (!partialUpdate) {
            searchRequest.addFieldsItem("Ref_DocKey");
            DocumentSearchFilter filter = new DocumentSearchFilter();
            filter.fieldName("Ref_DocKey");
            filter.setOperator(OperatorEnum.EQUAL);
            filter.fieldValue(parentId);
            searchRequest.addFiltersItem(filter);
            updateLog("Search for documents with parent id " + parentId);
        }

        // post request
        boolean isLast = false;
        int currentPage = 0;
        while (!isLast) {
            WebTarget target = client.target(actaProConfig.getConnectorUrl()).path("documents").queryParam("page", currentPage);
            Invocation.Builder builder = target.request();
            builder.header("Accept", "application/json");
            builder.header("Authorization", "Bearer " + token.getAccessToken());

            try (Response response = ActaProApi.retry(new IOException("failed after 5 retries"), Duration.ofSeconds(5l), 5,
                    () -> builder.post(Entity.entity(searchRequest, MediaType.APPLICATION_JSON)))) {
                Queue<String> queue = new LinkedList<>();
                if (200 == response.getStatus()) {

                    SearchResultPage srp = response.readEntity(SearchResultPage.class);
                    List<Map<String, String>> contentMap = srp.getContent();
                    for (Map<String, String> content : contentMap) {
                        if (content.get("path").contains(parentId)) {
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
                                // only add documents from the selected archive
                                importDocument(client, doc, recordGroup, rootElementID, token, nodeIdCache);
                                String newId = doc.getDocKey();
                                queue.add(newId);
                            }

                        }
                    }

                    isLast = srp.getLast();
                    currentPage++;
                } else {
                    ErrorResponse error = response.readEntity(ErrorResponse.class);
                    if (error != null) {
                        log.error("Search error for child document search , status: {}, text: {}, parentid: {},  paginator {}", error.getStatus(),
                                error.getMessage(), parentId, currentPage);
                        updateLog("Search error, status: " + error.getStatus() + ", text: " + error.getMessage());
                    } else {
                        log.error(
                                "Search error for child document search, HTTP status: {}, response body could not be parsed, , parentid: {},  paginator {}",
                                response.getStatus(), parentId, currentPage);
                        updateLog("Search error, HTTP status: " + response.getStatus());
                    }
                    isLast = true;
                }
                while (!queue.isEmpty()) {
                    String newId = queue.poll();
                    if (!newId.startsWith("Vz ")) {
                        answer.add(newId);
                    }
                }
            } catch (UnauthorizedException e) {
                throw e;
            } catch (IOException e) {
                updateLog("Cannot get child elements for " + parentId);
            }
        }
        return answer;
    }

    private void parseDocumentMetadata(Document doc, IEadEntry entry) {
        DocumentBlock block = doc.getBlock();

        // shared for the complete document, so that repeated ACTApro fields fill a repeatable ead field instead of overwriting it
        Set<IMetadataField> filledFields = NodeApi.newFilledFieldsCollector();

        for (DocumentField field : block.getFields()) {

            String fieldType = field.getType();
            // find ead metadata name

            // first check, if field name is used in a group // has sub fields
            for (MetadataMapping mm : actaProConfig.getMetadataFields()) {
                DocumentField matchedField = null;
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
                    NodeApi.addMetadataValue(entry, mm, matchedField, filledFields);
                }
            }
        }
        entry.calculateFingerprint();
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

            updateLog("Found node with with ACTApro ID '" + documentId + "', update existing node.");

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
            updateLog("Document with ID " + documentId + " does not exist, create new node.");
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
                                updateLog("API download failed for document with id " + path);
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

    private void importDocumentFromXml(io.goobi.api.job.actapro.model.Document doc, RecordGroup recordGroup,
            String rootElementID, Map<String, io.goobi.api.job.actapro.model.Document> allDocuments,
            Map<String, Integer> nodeIdCache) {
        String documentId = doc.getDocKey();

        Integer entryId = nodeIdCache.get(documentId);
        if (entryId != null) {
            ExtendedEadEntry entry = NodeApi.loadExtendendEntry(entryId);
            NodeInitializer.initEadNodeWithMetadata(entry, actaProConfig.getConfig().getConfiguredFields());
            String fingerprintBefore = entry.getFingerprint();

            String parentNodeId = getParentDocKey(doc);
            String docOrder = NodeApi.getDocOrder(doc);
            if (parentNodeId != null && docOrder != null && entry.getParentId() != null) {
                Integer parentEntryId = nodeIdCache.get(parentNodeId);
                if (parentEntryId != null && parentEntryId.intValue() != entry.getParentId()) {
                    entry.setParentId(parentEntryId);
                    try {
                        entry.setOrderNumber(Integer.parseInt(docOrder));
                    } catch (NumberFormatException e) {
                        log.error("Cannot parse Ref_DocOrder '{}': {}", docOrder, e.getMessage());
                    }
                    NodeApi.saveNode(recordGroup.getId(), entry);
                }
            }

            parseDocumentMetadata(doc, entry);
            if (!fingerprintBefore.equals(entry.getFingerprint())) {
                NodeApi.saveNode(recordGroup.getId(), entry);
            }
        } else {
            if (doc.getPath() == null) {
                return;
            }
            String[] paths = doc.getPath().split(";");
            Integer lastElementId = null;
            boolean rootElementFound = false;

            for (String path : paths) {
                path = path.trim();
                if (path.equals(rootElementID)) {
                    rootElementFound = true;
                }
                if (rootElementFound) {
                    Integer parentEntryId = nodeIdCache.get(path);
                    if (parentEntryId != null) {
                        lastElementId = parentEntryId;
                    } else {
                        if (lastElementId == null) {
                            lastElementId = nodeIdCache.get(rootElementID);
                        }
                        io.goobi.api.job.actapro.model.Document currentDoc = allDocuments.get(path);
                        if (currentDoc != null) {
                            lastElementId = createNodeForDocument(currentDoc, lastElementId, recordGroup, nodeIdCache);
                        } else {
                            updateLog("Document '" + path + "' not found in XML files, skipping.");
                        }
                    }
                }
            }
        }
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

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        pusher = null;
        actaProConfig = null;

    }
}
