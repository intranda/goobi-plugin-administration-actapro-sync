package io.goobi.api.job.actapro.model;

import org.apache.commons.lang3.StringUtils;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ActaProApi {

    /**
     * Authenticate at the service
     * 
     */

    public static AuthenticationToken authenticate(Client client, String authServiceHeader, String authServiceUrl, String authServiceUsername,
            String authServicePassword) {
        if (StringUtils.isBlank(authServiceHeader) || StringUtils.isBlank(authServiceUrl) || StringUtils.isBlank(authServiceUsername)
                || StringUtils.isBlank(authServicePassword)) {
            return null;
        }
        Form form = new Form();
        form.param("username", authServiceUsername);
        form.param("password", authServicePassword);
        form.param("grant_type", "password");

        WebTarget target = client.target(authServiceUrl);

        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", authServiceHeader);
        Response response = builder.post(Entity.form(form));
        return response.readEntity(AuthenticationToken.class);
    }

    public static Document updateDocument(Client client, AuthenticationToken token, String connectorUrl, Document doc) {
        if (token == null) {
            return null;
        }

        if (StringUtils.isBlank(doc.getDocKey())) {
            // its a new document, call createDocument instead
            return null;
        }

        WebTarget target = client.target(connectorUrl).path("document").path(doc.getDocKey());
        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", "Bearer " + token.getAccessToken());
        Response response = builder.put(Entity.entity(doc, MediaType.APPLICATION_JSON));
        if (200 == response.getStatus()) {
            return response.readEntity(Document.class);
        } else {
            ErrorResponse error = response.readEntity(ErrorResponse.class);
            log.error("Status: {}, Error: {}", error.getStatus(), error.getMessage());
            return null;
        }
    }

    public static Document createDocument(Client client, AuthenticationToken token, String connectorUrl, String parentDocKey, Document doc) {
        if (token == null) {
            return null;
        }
        WebTarget target = client.target(connectorUrl).path("document").queryParam("parentDocKey", parentDocKey).queryParam("format", "json");
        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", "Bearer " + token.getAccessToken());
        Response response = builder.post(Entity.entity(doc, MediaType.APPLICATION_JSON));
        if (200 == response.getStatus()) {
            return response.readEntity(Document.class);
        } else {
            ErrorResponse error = response.readEntity(ErrorResponse.class);
            log.error("Status: {}, Error: {}", error.getStatus(), error.getMessage());
            return null;
        }
    }

    /**
     * 
     * get a single document with the given key
     * 
     * @param client
     * @param token
     * @param key
     * @return
     */

    public static Document getDocumentByKey(Client client, AuthenticationToken token, String connectorUrl, String key) {
        if (token == null) {
            return null;
        }
        WebTarget target = client.target(connectorUrl).path("document").path(key);
        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", "Bearer " + token.getAccessToken());

        Response response = builder.get();
        if (200 == response.getStatus()) {

            Document doc = response.readEntity(Document.class);
            log.trace("DocKey: {}", doc.getDocKey());
            log.trace("type: {}", doc.getType());
            log.trace("DocTitle: {}", doc.getDocTitle());
            log.trace("CreatorID: {}", doc.getCreatorID());
            log.trace("OwnerID: {}", doc.getOwnerId());
            log.trace("CreationDate: {}", doc.getCreationDate());
            log.trace("ChangeDate: {}", doc.getChangeDate());
            log.trace("object: {}", doc.getObject());
            DocumentBlock block = doc.getBlock();
            log.trace("block type: {}", block.getType());

            for (DocumentField field : block.getFields()) {

                log.trace("*** {} ***", field.getType());
                if (StringUtils.isNotBlank(field.getPlainValue())) {
                    log.trace(field.getPlainValue());
                }
                if (StringUtils.isNotBlank(field.getValue())) {
                    log.trace(field.getValue());
                }
                if (field.getFields() != null) {
                    for (DocumentField subfield : field.getFields()) {
                        log.trace("    {}: {}", subfield.getType(), subfield.getValue());
                    }
                }
            }
            return doc;
        } else {
            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            return null;
        }
    }
}
