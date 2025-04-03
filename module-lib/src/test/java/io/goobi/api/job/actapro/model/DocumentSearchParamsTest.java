package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class DocumentSearchParamsTest {

    private DocumentSearchParams documentSearchParams;
    private DocumentSearchFilter filter;

    @Before
    public void setUp() {
        documentSearchParams = new DocumentSearchParams();
        filter = new DocumentSearchFilter();
    }

    @Test
    public void testQuery() {
        documentSearchParams.setQuery("search query");
        assertEquals("search query", documentSearchParams.getQuery());
    }

    @Test
    public void testDocumentTypes() {
        documentSearchParams.setDocumentTypes(Arrays.asList("type1", "type2"));
        assertEquals(2, documentSearchParams.getDocumentTypes().size());
        assertTrue(documentSearchParams.getDocumentTypes().contains("type1"));
        assertTrue(documentSearchParams.getDocumentTypes().contains("type2"));
    }

    @Test
    public void testAddDocumentTypesItem() {
        documentSearchParams.addDocumentTypesItem("type3");
        assertEquals(1, documentSearchParams.getDocumentTypes().size());
        assertTrue(documentSearchParams.getDocumentTypes().contains("type3"));
    }

    @Test
    public void testFilters() {
        documentSearchParams.setFilters(Arrays.asList(filter));
        assertEquals(1, documentSearchParams.getFilters().size());
    }

    @Test
    public void testAddFiltersItem() {
        documentSearchParams.addFiltersItem(filter);
        assertEquals(1, documentSearchParams.getFilters().size());
    }

    @Test
    public void testFields() {
        documentSearchParams.setFields(Arrays.asList("field1", "field2"));
        assertEquals(2, documentSearchParams.getFields().size());
        assertTrue(documentSearchParams.getFields().contains("field1"));
        assertTrue(documentSearchParams.getFields().contains("field2"));
    }

    @Test
    public void testAddFieldsItem() {
        documentSearchParams.addFieldsItem("field3");
        assertEquals(1, documentSearchParams.getFields().size());
        assertTrue(documentSearchParams.getFields().contains("field3"));
    }

    @Test
    public void testChainSetters() {
        documentSearchParams.query("search query")
                .documentTypes(Arrays.asList("type1", "type2"))
                .filters(Arrays.asList(filter))
                .fields(Arrays.asList("field1", "field2"));

        assertEquals("search query", documentSearchParams.getQuery());
        assertEquals(2, documentSearchParams.getDocumentTypes().size());
        assertEquals(1, documentSearchParams.getFilters().size());
        assertEquals(2, documentSearchParams.getFields().size());

    }

    @Test
    public void testToString() {
        documentSearchParams.setQuery("search query");
        documentSearchParams.setDocumentTypes(Arrays.asList("type1", "type2"));
        documentSearchParams.setFields(Arrays.asList("field1", "field2"));

        String result = documentSearchParams.toString();
        assertTrue(result.contains("query: search query"));
        assertTrue(result.contains("documentTypes: [type1, type2]"));
        assertTrue(result.contains("fields: [field1, field2]"));
    }
}
