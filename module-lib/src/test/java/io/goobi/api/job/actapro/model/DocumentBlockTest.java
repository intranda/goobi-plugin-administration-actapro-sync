package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DocumentBlockTest {

    private DocumentBlock documentBlock;
    private DocumentField documentField;

    @Before
    public void setUp() {
        documentBlock = new DocumentBlock();
        documentField = new DocumentField();
        documentField.setType("text");
        documentField.setValue("fieldValue");
    }

    @Test
    public void testType() {
        documentBlock.setType("header");
        assertEquals("header", documentBlock.getType());
    }

    @Test
    public void testFields() {
        List<DocumentField> fields = new ArrayList<>();
        fields.add(documentField);

        documentBlock.setFields(fields);
        assertEquals(1, documentBlock.getFields().size());
        assertEquals(documentField, documentBlock.getFields().get(0));
    }

    @Test
    public void testAddFieldsItem() {
        documentBlock.setFields(new ArrayList<>());
        documentBlock.addFieldsItem(documentField);

        assertEquals(1, documentBlock.getFields().size());
        assertEquals(documentField, documentBlock.getFields().get(0));
    }

    @Test
    public void testToString() {
        documentBlock.setType("header");

        List<DocumentField> fields = new ArrayList<>();
        fields.add(documentField);
        documentBlock.setFields(fields);

        String result = documentBlock.toString();
        assertTrue(result.contains("type: header"));
        assertTrue(result.contains("fields:"));
    }

    @Test
    public void testChainSetters() {
        List<DocumentField> fields = new ArrayList<>();
        fields.add(documentField);

        documentBlock.type("footer")
                .fields(fields);

        assertEquals("footer", documentBlock.getType());
        assertEquals(fields, documentBlock.getFields());
    }
}
