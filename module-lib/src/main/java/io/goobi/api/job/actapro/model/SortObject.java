package io.goobi.api.job.actapro.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SortObject {

    @JsonProperty("direction")
    private String direction = null;

    @JsonProperty("nullHandling")
    private String nullHandling = null;

    @JsonProperty("ascending")
    private Boolean ascending = null;

    @JsonProperty("property")
    private String property = null;

    @JsonProperty("ignoreCase")
    private Boolean ignoreCase = null;

    public SortObject direction(String direction) {
        this.direction = direction;
        return this;
    }

    public SortObject nullHandling(String nullHandling) {
        this.nullHandling = nullHandling;
        return this;
    }

    public SortObject ascending(Boolean ascending) {
        this.ascending = ascending;
        return this;
    }

    public SortObject property(String property) {
        this.property = property;
        return this;
    }

    public SortObject ignoreCase(Boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SortObject {\n");

        sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
        sb.append("    nullHandling: ").append(toIndentedString(nullHandling)).append("\n");
        sb.append("    ascending: ").append(toIndentedString(ascending)).append("\n");
        sb.append("    property: ").append(toIndentedString(property)).append("\n");
        sb.append("    ignoreCase: ").append(toIndentedString(ignoreCase)).append("\n");
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
