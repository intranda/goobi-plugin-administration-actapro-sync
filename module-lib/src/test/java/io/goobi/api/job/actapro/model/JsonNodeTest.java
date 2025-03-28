package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JsonNodeTest {

    @Test
    public void testToString() {
        JsonNode jsonNode = new JsonNode();
        String result = jsonNode.toString();
        assertTrue(result.contains("class JsonNode"));
    }
}
