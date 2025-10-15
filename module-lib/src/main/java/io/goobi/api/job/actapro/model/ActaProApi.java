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

            return response.readEntity(Document.class);

        } else {
            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            return null;
        }
    }

    public static String getJsonDocumentAsString(Client client, AuthenticationToken token, String connectorUrl, String key) {
        if (token == null) {
            return null;
        }
        WebTarget target = client.target(connectorUrl).path("document").path(key);
        Invocation.Builder builder = target.request();
        builder.header("Accept", "application/json");
        builder.header("Authorization", "Bearer " + token.getAccessToken());

        Response response = builder.get();
        if (200 == response.getStatus()) {

            return response.readEntity(String.class);
        } else {
            log.error("Status: {}, Error: {}", response.getStatus(), response.getStatusInfo().getReasonPhrase());
            return null;
        }
    }

    public static boolean updateDocumentField(Document doc, MetadataMapping mm, String value) {
        boolean metadataChanged = false;
        DocumentField f = null;

        for (DocumentField field : doc.getBlock().getFields()) {
            String fieldType = field.getType();

            if (StringUtils.isNoneBlank(mm.getJsonGroupType())) {
                if (mm.getJsonGroupType().equals(fieldType)) {
                    for (DocumentField subfield : field.getFields()) {
                        String subType = subfield.getType();
                        if (subType.equals(mm.getJsonType())) {
                            f = subfield;
                        }
                    }
                }
            } else if (mm.getJsonType().equals(fieldType)) {
                f = field;
            }
        }
        // change fields

        if (value != null && f != null) {
            // field exists in both instances, update plain_value/value
            if (StringUtils.isNotBlank(f.getPlainValue())) {
                if (!value.equals(f.getPlainValue())) {
                    f.setPlainValue(value);
                    metadataChanged = true;
                }
            } else if (!value.equals(f.getValue())) {
                f.setValue(value);
                metadataChanged = true;
            }
        } else if (value != null && f == null) {
            // field is new in node, create new DocumentField
            // check if group field is needed
            if (StringUtils.isNoneBlank(mm.getJsonGroupType())) {
                DocumentField groupField = null;
                // re-use existing group field
                for (DocumentField gf : doc.getBlock().getFields()) {
                    if (mm.getJsonGroupType().equals(gf.getType())) {
                        groupField = gf;
                        break;
                    }
                }
                // or create it if its missing
                if (groupField == null) {
                    groupField = new DocumentField();
                    groupField.setType(mm.getJsonGroupType());
                    doc.getBlock().addFieldsItem(groupField);
                }
                // add new field as sub field
                DocumentField df = new DocumentField();
                df.setType(mm.getJsonType());
                df.setValue(value);
                groupField.addFieldsItem(df);
            } else {
                // add new field to block
                DocumentField df = new DocumentField();
                df.setType(mm.getJsonType());
                df.setValue(value);
                doc.getBlock().addFieldsItem(df);
            }
            metadataChanged = true;
        } else if (value == null && f != null) {
            // field was deleted in the node, remove DocumentField
            doc.getBlock().getFields().remove(f);
            metadataChanged = true;

        } else if (value == null && f == null) {
            // metadata does not exist on both sides, nothing to do
        }
        return metadataChanged;
    }
}
