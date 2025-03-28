package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class DocumentFieldTimeRangeTest {

    private DocumentFieldTimeRange documentFieldTimeRange;

    @Before
    public void setUp() {
        documentFieldTimeRange = new DocumentFieldTimeRange();
    }

    @Test
    public void testValue() {
        documentFieldTimeRange.setValue("2025-03-28");
        assertEquals("2025-03-28", documentFieldTimeRange.getValue());
    }

    @Test
    public void testMin() {
        documentFieldTimeRange.setMin("2020-01-01");
        assertEquals("2020-01-01", documentFieldTimeRange.getMin());
    }

    @Test
    public void testMax() {
        documentFieldTimeRange.setMax("2025-12-31");
        assertEquals("2025-12-31", documentFieldTimeRange.getMax());
    }

    @Test
    public void testToString() {
        documentFieldTimeRange.setValue("2025-03-28");
        documentFieldTimeRange.setMin("2020-01-01");
        documentFieldTimeRange.setMax("2025-12-31");

        String result = documentFieldTimeRange.toString();
        assertTrue(result.contains("value: 2025-03-28"));
        assertTrue(result.contains("min: 2020-01-01"));
        assertTrue(result.contains("max: 2025-12-31"));
    }

    @Test
    public void testChainSetters() {
        documentFieldTimeRange.value("2025-03-28").min("2020-01-01").max("2025-12-31");

        assertEquals("2025-03-28", documentFieldTimeRange.getValue());
        assertEquals("2020-01-01", documentFieldTimeRange.getMin());
        assertEquals("2025-12-31", documentFieldTimeRange.getMax());
    }
}
