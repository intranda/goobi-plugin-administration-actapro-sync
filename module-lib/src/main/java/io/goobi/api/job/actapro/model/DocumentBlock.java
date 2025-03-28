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
public class DocumentBlock {

    @JsonProperty("type")
    private String type = null;

    @JsonProperty("fields")
    private List<DocumentField> fields = new ArrayList<>();

    public DocumentBlock type(String type) {
        this.type = type;
        return this;
    }

    public DocumentBlock fields(List<DocumentField> fields) {
        this.fields = fields;
        return this;
    }

    public DocumentBlock addFieldsItem(DocumentField fieldsItem) {
        this.fields.add(fieldsItem);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentBlock {\n");

        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
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
