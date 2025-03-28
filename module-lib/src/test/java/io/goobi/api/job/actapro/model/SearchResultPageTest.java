package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SearchResultPageTest {

    private SearchResultPage searchResultPage;

    @Before
    public void setUp() {
        searchResultPage = new SearchResultPage();
        searchResultPage.setTotalPages(5);
        searchResultPage.setTotalElements(100L);
        searchResultPage.setLast(true);
        searchResultPage.setSize(20);
        searchResultPage.setNumber(2);
        searchResultPage.setFirst(false);
        searchResultPage.setNumberOfElements(20);
        searchResultPage.setEmpty(false);

        // Add a sample content item
        Map<String, String> contentItem = new HashMap<>();
        contentItem.put("key", "value");
        searchResultPage.addContentItem(contentItem);
    }

    @Test
    public void testTotalPages() {
        assertEquals(Integer.valueOf(5), searchResultPage.getTotalPages());
    }

    @Test
    public void testTotalElements() {
        assertEquals(Long.valueOf(100), searchResultPage.getTotalElements());
    }

    @Test
    public void testLast() {
        assertTrue(searchResultPage.getLast());
    }

    @Test
    public void testSize() {
        assertEquals(Integer.valueOf(20), searchResultPage.getSize());
    }

    @Test
    public void testNumber() {
        assertEquals(Integer.valueOf(2), searchResultPage.getNumber());
    }

    @Test
    public void testFirst() {
        assertFalse(searchResultPage.getFirst());
    }

    @Test
    public void testNumberOfElements() {
        assertEquals(Integer.valueOf(20), searchResultPage.getNumberOfElements());
    }

    @Test
    public void testEmpty() {
        assertFalse(searchResultPage.getEmpty());
    }

    @Test
    public void testContent() {
        assertNotNull(searchResultPage.getContent());
        assertEquals(1, searchResultPage.getContent().size());
        assertTrue(searchResultPage.getContent().get(0).containsKey("key"));
        assertEquals("value", searchResultPage.getContent().get(0).get("key"));
    }

    @Test
    public void testChainSetters() {
        searchResultPage.totalPages(10)
                .totalElements(200L)
                .last(false)
                .size(50)
                .number(4)
                .first(true)
                .numberOfElements(50)
                .empty(true);

        assertEquals(Integer.valueOf(10), searchResultPage.getTotalPages());
        assertEquals(Long.valueOf(200), searchResultPage.getTotalElements());
        assertFalse(searchResultPage.getLast());
        assertEquals(Integer.valueOf(50), searchResultPage.getSize());
        assertEquals(Integer.valueOf(4), searchResultPage.getNumber());
        assertTrue(searchResultPage.getFirst());
        assertEquals(Integer.valueOf(50), searchResultPage.getNumberOfElements());
        assertTrue(searchResultPage.getEmpty());
    }

    @Test
    public void testToString() {
        String result = searchResultPage.toString();
        assertNotNull(result); // Ensure toString produces a non-null output
        assertTrue(result.contains("5"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("false"));
    }
}
