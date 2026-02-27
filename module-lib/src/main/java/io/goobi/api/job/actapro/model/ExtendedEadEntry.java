package io.goobi.api.job.actapro.model;

import org.apache.commons.lang3.StringUtils;

import de.intranda.goobi.plugins.model.EadEntry;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtendedEadEntry extends EadEntry {

    public ExtendedEadEntry(Integer order, Integer hierarchy) {
        super(order, hierarchy);
    }

    private Integer parentId;
    private String sequence;
    private String nodeTypeName;
    private String parentSequence;

    @Override
    public void setSequence(String sequenceNumber) {
        this.sequence = sequenceNumber;
    }

    @Override
    public String getSequence() {
        if (StringUtils.isNotBlank(sequence)) {
            return sequence;
        } else {
            return parentSequence;
        }
    }
}
