package cl.uchile.fea.segpres.models;

import com.google.gson.annotations.SerializedName;

public class HashRequest {

    private String content;

    @SerializedName("content-type")
    private String contentType;

    public HashRequest() {
        contentType = "application/pdf";
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
