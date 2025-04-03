package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SortObjectTest {

    private SortObject sortObject;

    @Before
    public void setUp() {
        sortObject = new SortObject();
    }

    @Test
    public void testSetAndGetDirection() {
        sortObject.setDirection("ASC");
        assertEquals("ASC", sortObject.getDirection());
    }

    @Test
    public void testSetAndGetNullHandling() {
        sortObject.setNullHandling("NATIVE");
        assertEquals("NATIVE", sortObject.getNullHandling());
    }

    @Test
    public void testSetAndGetAscending() {
        sortObject.setAscending(true);
        assertTrue(sortObject.getAscending());
    }

    @Test
    public void testSetAndGetProperty() {
        sortObject.setProperty("name");
        assertEquals("name", sortObject.getProperty());
    }

    @Test
    public void testSetAndGetIgnoreCase() {
        sortObject.setIgnoreCase(false);
        assertFalse(sortObject.getIgnoreCase());
    }

    @Test
    public void testFluentMethods() {
        sortObject.direction("DESC")
                .nullHandling("IGNORE")
                .ascending(false)
                .property("age")
                .ignoreCase(true);

        assertEquals("DESC", sortObject.getDirection());
        assertEquals("IGNORE", sortObject.getNullHandling());
        assertFalse(sortObject.getAscending());
        assertEquals("age", sortObject.getProperty());
        assertTrue(sortObject.getIgnoreCase());
    }

    @Test
    public void testToString() throws Exception {
        sortObject.setDirection("ASC");
        sortObject.setNullHandling("NATIVE");
        sortObject.setAscending(true);
        sortObject.setProperty("name");
        sortObject.setIgnoreCase(false);

        String expectedOutput = "class SortObject {\n" +
                "    direction: ASC\n" +
                "    nullHandling: NATIVE\n" +
                "    ascending: true\n" +
                "    property: name\n" +
                "    ignoreCase: false\n" +
                "}";

        assertEquals(expectedOutput, sortObject.toString());
    }

}
