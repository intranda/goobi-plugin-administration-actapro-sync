package io.goobi.api.job.actapro.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MetadataMapping {

    private String jsonType;
    private String jsonGroupType;

    private String eadField;
    private String eadGroup;
    private String eadArea;

}
