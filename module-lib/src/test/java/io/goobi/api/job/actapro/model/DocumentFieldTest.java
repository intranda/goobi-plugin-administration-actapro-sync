package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DocumentFieldTest {

    private DocumentField documentField;
    private DocumentFieldTimeRanges timerange;
    private DocumentField nestedField;

    @Before
    public void setUp() {
        documentField = new DocumentField();
        timerange = new DocumentFieldTimeRanges();
        nestedField = new DocumentField();

        // Setup values for the timerange
        DocumentFieldTimeRange timeRangeValue = new DocumentFieldTimeRange();
        timeRangeValue.setValue("2025-03-28");
        timeRangeValue.setMin("2020-01-01");
        timeRangeValue.setMax("2025-12-31");
        List<DocumentFieldTimeRange> timeRanges = new ArrayList<>();
        timeRanges.add(timeRangeValue);
        timerange.setValues(timeRanges);

        // Setup the nested fields
        nestedField.setType("nestedType");
        nestedField.setValue("nestedValue");
    }

    @Test
    public void testType() {
        documentField.setType("text");
        assertEquals("text", documentField.getType());
    }

    @Test
    public void testValue() {
        documentField.setValue("This is a value");
        assertEquals("This is a value", documentField.getValue());
    }

    @Test
    public void testPlainValue() {
        documentField.setPlainValue("plainTextValue");
        assertEquals("plainTextValue", documentField.getPlainValue());
    }

    @Test
    public void testTimerange() {
        documentField.setTimerange(timerange);
        assertEquals(timerange, documentField.getTimerange());
    }

    @Test
    public void testFields() {
        List<DocumentField> fields = new ArrayList<>();
        fields.add(nestedField);

        documentField.setFields(fields);
        assertEquals(1, documentField.getFields().size());
        assertEquals(nestedField, documentField.getFields().get(0));
    }

    @Test
    public void testAddFieldsItem() {
        documentField.setFields(new ArrayList<>());
        documentField.addFieldsItem(nestedField);

        assertEquals(1, documentField.getFields().size());
        assertEquals(nestedField, documentField.getFields().get(0));
    }

    @Test
    public void testToString() {
        documentField.setType("text");
        documentField.setValue("This is a value");
        documentField.setPlainValue("plainTextValue");
        documentField.setTimerange(timerange);

        List<DocumentField> fields = new ArrayList<>();
        fields.add(nestedField);
        documentField.setFields(fields);

        String result = documentField.toString();
        assertTrue(result.contains("type: text"));
        assertTrue(result.contains("value: This is a value"));
        assertTrue(result.contains("plainValue: plainTextValue"));
        assertTrue(result.contains("timerange:"));
        assertTrue(result.contains("fields:"));
    }

    @Test
    public void testChainSetters() {
        documentField.type("text")
                .value("This is a value")
                .plainValue("plainTextValue")
                .timerange(timerange);

        List<DocumentField> fields = new ArrayList<>();
        fields.add(nestedField);

        documentField.fields(fields);

        assertEquals("text", documentField.getType());
        assertEquals("This is a value", documentField.getValue());
        assertEquals("plainTextValue", documentField.getPlainValue());
        assertEquals(timerange, documentField.getTimerange());
        assertEquals(fields, documentField.getFields());
    }
}
