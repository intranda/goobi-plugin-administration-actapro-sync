package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.goobi.api.job.actapro.model.DocumentSearchFilter.OperatorEnum;

public class DocumentSearchFilterTest {

    private DocumentSearchFilter documentSearchFilter;

    @Before
    public void setUp() {
        documentSearchFilter = new DocumentSearchFilter();
    }

    @Test
    public void testFieldName() {
        documentSearchFilter.setFieldName("title");
        assertEquals("title", documentSearchFilter.getFieldName());
    }

    @Test
    public void testOperator() {
        documentSearchFilter.setOperator(DocumentSearchFilter.OperatorEnum.GREATER_THAN_OR_EQUAL_TO);
        assertEquals(DocumentSearchFilter.OperatorEnum.GREATER_THAN_OR_EQUAL_TO, documentSearchFilter.getOperator());
        assertEquals(">=", documentSearchFilter.getOperator().getValue());
    }

    @Test
    public void testFieldValue() {
        documentSearchFilter.setFieldValue("value");
        assertEquals("value", documentSearchFilter.getFieldValue());
    }

    @Test
    public void testEnumFromValue() {
        assertEquals(DocumentSearchFilter.OperatorEnum.EQUAL, DocumentSearchFilter.OperatorEnum.fromValue("="));
        assertEquals(DocumentSearchFilter.OperatorEnum._U, DocumentSearchFilter.OperatorEnum.fromValue("<>"));
        assertEquals(DocumentSearchFilter.OperatorEnum.GREATER_THAN_OR_EQUAL_TO, DocumentSearchFilter.OperatorEnum.fromValue(">="));
        assertEquals(DocumentSearchFilter.OperatorEnum.LESS_THAN_OR_EQUAL_TO, DocumentSearchFilter.OperatorEnum.fromValue("<="));
        assertEquals(DocumentSearchFilter.OperatorEnum.LIKE, DocumentSearchFilter.OperatorEnum.fromValue("like"));
    }

    @Test
    public void testFluentMethods() {
        documentSearchFilter.fieldName("title")
                .operator(OperatorEnum.EQUAL)
                .fieldValue("value");

        assertEquals("title", documentSearchFilter.getFieldName());
        assertEquals("=", documentSearchFilter.getOperator().getValue());
        assertEquals("value", documentSearchFilter.getFieldValue());
    }

    @Test
    public void testToString() {
        documentSearchFilter.setFieldName("title");
        documentSearchFilter.setOperator(DocumentSearchFilter.OperatorEnum.LIKE);
        documentSearchFilter.setFieldValue("book");

        String result = documentSearchFilter.toString();
        assertTrue(result.contains("fieldName: title"));
        assertTrue(result.contains("operator: like"));
        assertTrue(result.contains("fieldValue: book"));
    }
}
