package io.goobi.api.job.actapro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

/**
 * Document content
 **/
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class JsonNode {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class JsonNode {\n");

        sb.append("}");
        return sb.toString();
    }

}
