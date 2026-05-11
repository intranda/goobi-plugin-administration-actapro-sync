package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.api.job.actapro.model.Document;
import io.goobi.api.job.actapro.model.DocumentField;

public class ActaproSyncPluginTest {

    private static String resourcesFolder;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/resources/";
        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/resources/";
        }
        System.setProperty("log4j.configurationFile", resourcesFolder + "log4j2.xml");
    }

    @Test
    public void testParseXmlDocumentSet_documentCount() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        assertEquals(3, docs.size());
    }

    @Test
    public void testParseXmlDocumentSet_archDocument() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document arch = docs.get("Arch    test-arch-001");
        assertNotNull(arch);
        assertEquals("Arch    test-arch-001", arch.getDocKey());
        assertEquals("Test Archive", arch.getDocTitle());
        assertEquals("Arch", arch.getType());
        assertEquals("ACTAPRO", arch.getCreatorID());
    }

    @Test
    public void testParseXmlDocumentSet_nestedRefGp() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document tekt = docs.get("Tekt    test-tekt-001");
        assertNotNull(tekt);
        DocumentField refGp = tekt.getBlock().getFields().stream()
                .filter(f -> "Ref_Gp".equals(f.getType()))
                .findFirst().orElse(null);
        assertNotNull(refGp);
        assertEquals(4, refGp.getFields().size());
        DocumentField refDocKey = refGp.getFields().stream()
                .filter(f -> "Ref_DocKey".equals(f.getType()))
                .findFirst().orElse(null);
        assertNotNull(refDocKey);
        assertEquals("Arch    test-arch-001", refDocKey.getValue());
    }

    @Test
    public void testParseXmlDocumentSet_plainValue() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document tekt = docs.get("Tekt    test-tekt-001");
        DocumentField laufzeit = tekt.getBlock().getFields().stream()
                .filter(f -> "Laufzeit".equals(f.getType()))
                .findFirst().orElse(null);
        assertNotNull(laufzeit);
        assertEquals("1900-2000", laufzeit.getValue());
        assertEquals("Laufzeit 1900-2000", laufzeit.getPlainValue());
    }

    @Test
    public void testGetParentDocKey_withParent() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document tekt = docs.get("Tekt    test-tekt-001");
        String parentKey = ActaProSyncAdministrationPlugin.getParentDocKey(tekt);
        assertEquals("Arch    test-arch-001", parentKey);
    }

    @Test
    public void testGetParentDocKey_rootHasNoParent() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document arch = docs.get("Arch    test-arch-001");
        assertNull(ActaProSyncAdministrationPlugin.getParentDocKey(arch));
    }

    @Test
    public void testGetDocOrder() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, Document> docs = ActaProSyncAdministrationPlugin.parseXmlDocumentSet(xmlFile);
        Document tekt = docs.get("Tekt    test-tekt-001");
        assertEquals("1", ActaProSyncAdministrationPlugin.getDocOrder(tekt));
    }

    @Test
    public void testBuildXmlPath_singleRoot() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, String> parentIndex = new LinkedHashMap<>();
        ActaProSyncAdministrationPlugin.buildParentIndex(xmlFile, parentIndex);
        String path = ActaProSyncAdministrationPlugin.buildXmlPath("Arch    test-arch-001", parentIndex);
        assertEquals("Arch    test-arch-001", path);
    }

    @Test
    public void testBuildXmlPath_threeLevel() throws Exception {
        Path xmlFile = Paths.get(resourcesFolder + "test-documents.xml");
        Map<String, String> parentIndex = new LinkedHashMap<>();
        ActaProSyncAdministrationPlugin.buildParentIndex(xmlFile, parentIndex);
        String path = ActaProSyncAdministrationPlugin.buildXmlPath("Best    test-best-001", parentIndex);
        assertEquals("Arch    test-arch-001; Tekt    test-tekt-001; Best    test-best-001", path);
    }
}
