package io.goobi.api.job.actapro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Document {

    @JsonProperty("DocKey")
    private String docKey = null;

    @JsonProperty("DocTitle")
    private String docTitle = null;

    @JsonProperty("type")
    private String type = null;

    @JsonProperty("CreatorID")
    private String creatorID = null;

    @JsonProperty("OwnerID")
    //@JsonProperty("OwnerId") TODO: swagger api schema has OwnerId, actual api uses OwnerID
    private String ownerId = null;

    @JsonProperty("CreationDate")
    // TODO enable correct format after api got fixed
    //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss:SSS")
    private String creationDate = null;

    @JsonProperty("ChangeDate")
    //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy' 'HH:mm:ss:SSS", timezone = "GMT")
    // private Date changeDate = null;
    //    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss:SSS")
    private String changeDate = null;

    @JsonProperty("object")
    private String object = "document";

    @JsonProperty("block")
    private DocumentBlock block = null;

    private String path;

    public Document docKey(String docKey) {
        this.docKey = docKey;
        return this;
    }

    public Document type(String type) {
        this.type = type;
        return this;
    }

    public Document docTitle(String docTitle) {
        this.docTitle = docTitle;
        return this;
    }

    public Document creatorID(String creatorID) {
        this.creatorID = creatorID;
        return this;
    }

    public Document ownerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    //    public Document creationDate(Date creationDate) {
    //        this.creationDate = creationDate;
    //        return this;
    //    }

    public Document creationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }
    //    public Document changeDate(Date changeDate) {
    //        this.changeDate = changeDate;
    //        return this;
    //    }

    public Document changeDate(String changeDate) {
        this.changeDate = changeDate;
        return this;
    }

    public Document object(String object) {
        this.object = object;
        return this;
    }

    public Document block(DocumentBlock block) {
        this.block = block;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Document {\n");

        sb.append("    docKey: ").append(toIndentedString(docKey)).append("\n");
        sb.append("    docTitle: ").append(toIndentedString(docTitle)).append("\n");
        sb.append("    creatorID: ").append(toIndentedString(creatorID)).append("\n");
        sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
        sb.append("    creationDate: ").append(toIndentedString(creationDate)).append("\n");
        sb.append("    changeDate: ").append(toIndentedString(changeDate)).append("\n");
        sb.append("    object: ").append(toIndentedString(object)).append("\n");
        sb.append("    block: ").append(toIndentedString(block)).append("\n");
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
