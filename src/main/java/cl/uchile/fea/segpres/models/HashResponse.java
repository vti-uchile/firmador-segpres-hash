package cl.uchile.fea.segpres.models;

public class HashResponse {

    private String content;
    private String status;
    private String contentType;
    private String documentStatus;
    private String hashOriginal;

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }
    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getHashOriginal() {
        return hashOriginal;
    }
    public void setHashOriginal(String hashOriginal) {
        this.hashOriginal = hashOriginal;
    }
}
