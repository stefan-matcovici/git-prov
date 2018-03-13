package ro.uaic.info.gitprov.models.ProvStore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

public class ProvStoreRequestStorage {
    @JsonProperty("rec_id")
    private String id;

    @JsonProperty("public")
    private String isPublic;

    @JsonProperty("content")
    @JsonRawValue
    private String content;

    public ProvStoreRequestStorage(String id, boolean isPublic, String content) {
        this.id = id;
        this.isPublic = String.valueOf(isPublic);
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsPublic() {
        return isPublic;
    }

    public void setPublic(String isPublic) {
        this.isPublic = isPublic;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
