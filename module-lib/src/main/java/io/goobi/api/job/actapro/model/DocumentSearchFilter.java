package io.goobi.api.job.actapro.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class DocumentSearchFilter {

    @JsonProperty("fieldName")
    private String fieldName = null;

    public enum OperatorEnum {
        EQUAL("="),
        _U("<>"),
        GREATER_THAN_OR_EQUAL_TO(">="),
        LESS_THAN_OR_EQUAL_TO("<="),
        LIKE("like");

        private String value;

        OperatorEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static OperatorEnum fromValue(String text) {
            for (OperatorEnum b : OperatorEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("operator")
    private OperatorEnum operator = null;

    @JsonProperty("fieldValue")
    private String fieldValue = null;

    public DocumentSearchFilter fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public DocumentSearchFilter operator(OperatorEnum operator) {
        this.operator = operator;
        return this;
    }

    public DocumentSearchFilter fieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DocumentSearchFilter {\n");

        sb.append("    fieldName: ").append(toIndentedString(fieldName)).append("\n");
        sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
        sb.append("    fieldValue: ").append(toIndentedString(fieldValue)).append("\n");
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
