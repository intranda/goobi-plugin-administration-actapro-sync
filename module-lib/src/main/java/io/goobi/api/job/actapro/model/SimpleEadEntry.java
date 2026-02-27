package io.goobi.api.job.actapro.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleEadEntry {

    private Integer id;
    private String uuid;
    private String sequence;
    private int hierarchy;
    private int order;

    private Integer parentId;

    public SimpleEadEntry() {
    }

}
