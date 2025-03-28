package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DocumentTest {

    private Document document;
    private DocumentBlock documentBlock;

    @Before
    public void setUp() {
        document = new Document();
        documentBlock = new DocumentBlock();
        documentBlock.setType("header");

        DocumentField documentField = new DocumentField();
        documentField.setType("text");
        documentField.setValue("fieldValue");
        documentBlock.addFieldsItem(documentField);
    }

    @Test
    public void testDocKey() {
        document.setDocKey("doc123");
        assertEquals("doc123", document.getDocKey());
    }

    @Test
    public void testDocTitle() {
        document.setDocTitle("Sample Document");
        assertEquals("Sample Document", document.getDocTitle());
    }

    @Test
    public void testType() {
        document.setType("pdf");
        assertEquals("pdf", document.getType());
    }

    @Test
    public void testCreatorID() {
        document.setCreatorID("creator123");
        assertEquals("creator123", document.getCreatorID());
    }

    @Test
    public void testOwnerId() {
        document.setOwnerId("owner123");
        assertEquals("owner123", document.getOwnerId());
    }

    @Test
    public void testCreationDate() {
        document.setCreationDate("2025-03-28");
        assertEquals("2025-03-28", document.getCreationDate());
    }

    @Test
    public void testChangeDate() {
        document.setChangeDate("2025-03-29");
        assertEquals("2025-03-29", document.getChangeDate());
    }

    @Test
    public void testObject() {
        document.setObject("document");
        assertEquals("document", document.getObject());
    }

    @Test
    public void testBlock() {
        document.setBlock(documentBlock);
        assertNotNull(document.getBlock());
        assertEquals("header", document.getBlock().getType());
    }

    @Test
    public void testToString() {
        document.setDocKey("doc123");
        document.setDocTitle("Sample Document");
        document.setCreatorID("creator123");
        document.setOwnerId("owner123");
        document.setCreationDate("2025-03-28");
        document.setChangeDate("2025-03-29");
        document.setObject("document");
        document.setBlock(documentBlock);

        String result = document.toString();
        assertTrue(result.contains("docKey: doc123"));
        assertTrue(result.contains("docTitle: Sample Document"));
        assertTrue(result.contains("creatorID: creator123"));
        assertTrue(result.contains("ownerId: owner123"));
        assertTrue(result.contains("creationDate: 2025-03-28"));
        assertTrue(result.contains("changeDate: 2025-03-29"));
        assertTrue(result.contains("object: document"));
        assertTrue(result.contains("block:"));
    }

    @Test
    public void testChainSetters() {
        document.docKey("doc123")
                .docTitle("Sample Document")
                .type("pdf")
                .creatorID("creator123")
                .ownerId("owner123")
                .creationDate("2025-03-28")
                .changeDate("2025-03-29")
                .object("document")
                .block(documentBlock);

        assertEquals("doc123", document.getDocKey());
        assertEquals("Sample Document", document.getDocTitle());
        assertEquals("pdf", document.getType());
        assertEquals("creator123", document.getCreatorID());
        assertEquals("owner123", document.getOwnerId());
        assertEquals("2025-03-28", document.getCreationDate());
        assertEquals("2025-03-29", document.getChangeDate());
        assertEquals("document", document.getObject());
        assertEquals(documentBlock, document.getBlock());
    }
}
