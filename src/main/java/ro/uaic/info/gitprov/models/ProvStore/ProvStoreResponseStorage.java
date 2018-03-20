package ro.uaic.info.gitprov.models.ProvStore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProvStoreResponseStorage {

    private static final String PROVSTORE_DOCUMENT_LINK = "https://provenance.ecs.soton.ac.uk/store/documents/";

    @JsonProperty("created_at")
    private String date;

    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("id")
    private String id;

    private boolean invalid;

    private String owner;

    @JsonProperty("public")
    private String isPublic;

    @JsonProperty("rec_id")
    private String resourceId;

    @JsonProperty("resource_uri")
    private String resourceUri;


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(String isPublic) {
        this.isPublic = isPublic;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = PROVSTORE_DOCUMENT_LINK + id;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }
}
