package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DocumentFieldTimeRangesTest {

    private DocumentFieldTimeRanges documentFieldTimeRanges;
    private DocumentFieldTimeRange documentFieldTimeRange;

    @Before
    public void setUp() {
        documentFieldTimeRanges = new DocumentFieldTimeRanges();
        documentFieldTimeRange = new DocumentFieldTimeRange();
        documentFieldTimeRange.setValue("2025-03-28");
        documentFieldTimeRange.setMin("2020-01-01");
        documentFieldTimeRange.setMax("2025-12-31");
    }

    @Test
    public void testMin() {
        documentFieldTimeRanges.setMin("2020-01-01");
        assertEquals("2020-01-01", documentFieldTimeRanges.getMin());
    }

    @Test
    public void testMax() {
        documentFieldTimeRanges.setMax("2025-12-31");
        assertEquals("2025-12-31", documentFieldTimeRanges.getMax());
    }

    @Test
    public void testValues() {
        List<DocumentFieldTimeRange> timeRanges = new ArrayList<>();
        timeRanges.add(documentFieldTimeRange);

        documentFieldTimeRanges.setValues(timeRanges);
        assertEquals(1, documentFieldTimeRanges.getValues().size());
        assertEquals(documentFieldTimeRange, documentFieldTimeRanges.getValues().get(0));
    }

    @Test
    public void testAddValuesItem() {
        documentFieldTimeRanges.setValues(new ArrayList<>());
        documentFieldTimeRanges.addValuesItem(documentFieldTimeRange);

        assertEquals(1, documentFieldTimeRanges.getValues().size());
        assertEquals(documentFieldTimeRange, documentFieldTimeRanges.getValues().get(0));
    }

    @Test
    public void testToString() {
        List<DocumentFieldTimeRange> timeRanges = new ArrayList<>();
        timeRanges.add(documentFieldTimeRange);

        documentFieldTimeRanges.setMin("2020-01-01");
        documentFieldTimeRanges.setMax("2025-12-31");
        documentFieldTimeRanges.setValues(timeRanges);

        String result = documentFieldTimeRanges.toString();
        assertTrue(result.contains("min: 2020-01-01"));
        assertTrue(result.contains("max: 2025-12-31"));
        assertTrue(result.contains("values:"));
    }

    @Test
    public void testChainSetters() {
        List<DocumentFieldTimeRange> timeRanges = new ArrayList<>();
        timeRanges.add(documentFieldTimeRange);

        documentFieldTimeRanges.min("2020-01-01")
                .max("2025-12-31")
                .values(timeRanges);

        assertEquals("2020-01-01", documentFieldTimeRanges.getMin());
        assertEquals("2025-12-31", documentFieldTimeRanges.getMax());
        assertEquals(timeRanges, documentFieldTimeRanges.getValues());
    }
}
