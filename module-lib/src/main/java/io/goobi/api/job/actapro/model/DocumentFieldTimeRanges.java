package io.goobi.api.job.actapro.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DocumentFieldTimeRanges {

    @JsonProperty("min")
    private String min = null;

    @JsonProperty("max")
    private String max = null;

    @JsonProperty("values")
    private List<DocumentFieldTimeRange> values = null;

    public DocumentFieldTimeRanges min(String min) {
        this.min = min;
        return this;
    }

    public DocumentFieldTimeRanges max(String max) {
        this.max = max;
        return this;
    }

    public DocumentFieldTimeRanges values(List<DocumentFieldTimeRange> values) {
        this.values = values;
        return this;
    }

    public DocumentFieldTimeRanges addValuesItem(DocumentFieldTimeRange valuesItem) {
        this.values.add(valuesItem);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentFieldTimeRanges {\n");
        sb.append("    min: ").append(toIndentedString(min)).append("\n");
        sb.append("    max: ").append(toIndentedString(max)).append("\n");
        sb.append("    values: ").append(toIndentedString(values)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private static String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
