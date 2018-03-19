package ro.uaic.info.gitprov.models.ProvStore;

public class UploadDocumentRequest {
    private String name;
    private boolean isPublic;

    public UploadDocumentRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
