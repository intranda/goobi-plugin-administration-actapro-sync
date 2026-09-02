package io.goobi.api.job.actapro.model;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.goobi.interfaces.IMetadataField;
import org.junit.Test;

import de.intranda.goobi.plugins.model.EadMetadataField;

public class NodeApiTest {

    private static final MetadataMapping FILE_PATH_MAPPING = new MetadataMapping("DD_FilePath", "DD", "separatedmaterial", "", "5");

    private static EadMetadataField createField(String name, boolean repeatable) {
        return new EadMetadataField(name, 5, "./ead:p", "element", repeatable, true, true, "input", name, false, null, null, false, null, null,
                false, null);
    }

    @Test
    public void testSaveValueAddsSecondValueForRepeatableField() {
        EadMetadataField field = createField("separatedmaterial", true);
        Set<IMetadataField> filledFields = NodeApi.newFilledFieldsCollector();

        NodeApi.saveValue(FILE_PATH_MAPPING, "first", field, filledFields);
        NodeApi.saveValue(FILE_PATH_MAPPING, "second", field, filledFields);

        assertEquals(2, field.getValues().size());
        assertEquals("first", field.getValues().get(0).getValue());
        assertEquals("second", field.getValues().get(1).getValue());
    }

    @Test
    public void testSaveValueOverwritesValueForNonRepeatableField() {
        EadMetadataField field = createField("separatedmaterial", false);
        Set<IMetadataField> filledFields = NodeApi.newFilledFieldsCollector();

        NodeApi.saveValue(FILE_PATH_MAPPING, "first", field, filledFields);
        NodeApi.saveValue(FILE_PATH_MAPPING, "second", field, filledFields);

        assertEquals(1, field.getValues().size());
        assertEquals("second", field.getValues().get(0).getValue());
    }

    @Test
    public void testSaveValueReplacesValuesOfPreviousImport() {
        EadMetadataField field = createField("separatedmaterial", true);

        Set<IMetadataField> firstImport = NodeApi.newFilledFieldsCollector();
        NodeApi.saveValue(FILE_PATH_MAPPING, "a", field, firstImport);
        NodeApi.saveValue(FILE_PATH_MAPPING, "b", field, firstImport);

        Set<IMetadataField> secondImport = NodeApi.newFilledFieldsCollector();
        NodeApi.saveValue(FILE_PATH_MAPPING, "a", field, secondImport);
        NodeApi.saveValue(FILE_PATH_MAPPING, "b", field, secondImport);

        assertEquals(2, field.getValues().size());
        assertEquals("a", field.getValues().get(0).getValue());
        assertEquals("b", field.getValues().get(1).getValue());
    }

    @Test
    public void testSaveValueIgnoresFieldsWithOtherName() {
        EadMetadataField otherField = createField("PaLastname", false);
        Set<IMetadataField> filledFields = NodeApi.newFilledFieldsCollector();

        NodeApi.saveValue(FILE_PATH_MAPPING, "value", otherField, filledFields);

        assertEquals(null, otherField.getValues());
    }
}
