package io.goobi.api.job.actapro.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Search parameters
 **/
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DocumentSearchParams {

    @JsonProperty("query")
    private String query = null;

    @JsonProperty("documentTypes")

    private List<String> documentTypes = new ArrayList<>();

    @JsonProperty("filters")
    private List<DocumentSearchFilter> filters = new ArrayList<>();

    @JsonProperty("fields")
    private List<String> fields = new ArrayList<>();

    public DocumentSearchParams query(String query) {
        this.query = query;
        return this;
    }

    public DocumentSearchParams documentTypes(List<String> documentTypes) {
        this.documentTypes = documentTypes;
        return this;
    }

    public DocumentSearchParams addDocumentTypesItem(String documentTypesItem) {
        this.documentTypes.add(documentTypesItem);
        return this;
    }

    public DocumentSearchParams filters(List<DocumentSearchFilter> filters) {
        this.filters = filters;
        return this;
    }

    public DocumentSearchParams addFiltersItem(DocumentSearchFilter filtersItem) {
        this.filters.add(filtersItem);
        return this;
    }

    public DocumentSearchParams fields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    public DocumentSearchParams addFieldsItem(String fieldsItem) {
        this.fields.add(fieldsItem);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentSearchParams {\n");

        sb.append("    query: ").append(toIndentedString(query)).append("\n");
        sb.append("    documentTypes: ").append(toIndentedString(documentTypes)).append("\n");
        sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
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
