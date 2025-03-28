package io.goobi.api.job.actapro.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DocumentField {

    @JsonProperty("type")
    private String type = null;

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("fields")
    private List<DocumentField> fields = new ArrayList<>();

    @JsonProperty("plain_value")
    private String plainValue = null;

    @JsonProperty("timerange")
    private DocumentFieldTimeRanges timerange = null;

    public DocumentField type(String type) {
        this.type = type;
        return this;
    }

    public DocumentField value(String value) {
        this.value = value;
        return this;
    }

    public DocumentField fields(List<DocumentField> fields) {
        this.fields = fields;
        return this;
    }

    public DocumentField addFieldsItem(DocumentField fieldsItem) {
        this.fields.add(fieldsItem);
        return this;
    }

    public DocumentField plainValue(String plainValue) {
        this.plainValue = plainValue;
        return this;
    }

    public DocumentField timerange(DocumentFieldTimeRanges timerange) {
        this.timerange = timerange;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentField {\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
        sb.append("    plainValue: ").append(toIndentedString(plainValue)).append("\n");
        sb.append("    timerange: ").append(toIndentedString(timerange)).append("\n");
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
