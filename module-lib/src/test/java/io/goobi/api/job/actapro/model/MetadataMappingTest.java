package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MetadataMappingTest {

    private MetadataMapping metadataMapping;

    @Before
    public void setUp() {
        metadataMapping = new MetadataMapping("jsonTypeValue", "jsonGroupTypeValue", "eadFieldValue", "eadGroupValue", "eadAreaValue");
    }

    @Test
    public void testConstructor() {
        assertEquals("jsonTypeValue", metadataMapping.getJsonType());
        assertEquals("jsonGroupTypeValue", metadataMapping.getJsonGroupType());
        assertEquals("eadFieldValue", metadataMapping.getEadField());
        assertEquals("eadGroupValue", metadataMapping.getEadGroup());
        assertEquals("eadAreaValue", metadataMapping.getEadArea());
    }

    @Test
    public void testSettersAndGetters() {
        metadataMapping.setJsonType("newJsonType");
        assertEquals("newJsonType", metadataMapping.getJsonType());

        metadataMapping.setJsonGroupType("newJsonGroupType");
        assertEquals("newJsonGroupType", metadataMapping.getJsonGroupType());

        metadataMapping.setEadField("newEadField");
        assertEquals("newEadField", metadataMapping.getEadField());

        metadataMapping.setEadGroup("newEadGroup");
        assertEquals("newEadGroup", metadataMapping.getEadGroup());

        metadataMapping.setEadArea("newEadArea");
        assertEquals("newEadArea", metadataMapping.getEadArea());
    }
}
