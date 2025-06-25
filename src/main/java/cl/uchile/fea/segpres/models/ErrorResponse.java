package cl.uchile.fea.segpres.models;

public class ErrorResponse {

    private String timestamp;
    private Integer status;
    private String error;

    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }
    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
}
