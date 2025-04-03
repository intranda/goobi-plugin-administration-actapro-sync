package io.goobi.api.job.actapro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DocumentFieldTimeRange {

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("min")
    private String min = null;

    @JsonProperty("max")
    private String max = null;

    public DocumentFieldTimeRange value(String value) {
        this.value = value;
        return this;
    }

    public DocumentFieldTimeRange min(String min) {
        this.min = min;
        return this;
    }

    public DocumentFieldTimeRange max(String max) {
        this.max = max;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentFieldTimeRange {\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    min: ").append(toIndentedString(min)).append("\n");
        sb.append("    max: ").append(toIndentedString(max)).append("\n");
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
