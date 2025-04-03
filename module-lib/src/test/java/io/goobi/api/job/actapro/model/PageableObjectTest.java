package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PageableObjectTest {

    private PageableObject pageableObject;

    @Before
    public void setUp() {
        pageableObject = new PageableObject();
        pageableObject.setOffset(100L);
        pageableObject.setPaged(true);
        pageableObject.setUnpaged(false);
        pageableObject.setPageSize(20);
        pageableObject.setPageNumber(2);
    }

    @Test
    public void testOffset() {
        assertEquals(Long.valueOf(100), pageableObject.getOffset());
    }

    @Test
    public void testPaged() {
        assertTrue(pageableObject.getPaged());
    }

    @Test
    public void testUnpaged() {
        assertFalse(pageableObject.getUnpaged());
    }

    @Test
    public void testPageSize() {
        assertEquals(Integer.valueOf(20), pageableObject.getPageSize());
    }

    @Test
    public void testPageNumber() {
        assertEquals(Integer.valueOf(2), pageableObject.getPageNumber());
    }

    @Test
    public void testChainSetters() {
        pageableObject.offset(200L)
                .paged(false)
                .unpaged(true)
                .pageSize(30)
                .pageNumber(3);

        assertEquals(Long.valueOf(200), pageableObject.getOffset());
        assertFalse(pageableObject.getPaged());
        assertTrue(pageableObject.getUnpaged());
        assertEquals(Integer.valueOf(30), pageableObject.getPageSize());
        assertEquals(Integer.valueOf(3), pageableObject.getPageNumber());
    }

    @Test
    public void testToString() {
        String result = pageableObject.toString();
        assertNotNull(result); // Ensure toString produces a non-null output
        assertTrue(result.contains("100"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
        assertTrue(result.contains("20"));
        assertTrue(result.contains("2"));
    }
}
