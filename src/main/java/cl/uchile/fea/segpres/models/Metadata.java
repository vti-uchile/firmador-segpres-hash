package cl.uchile.fea.segpres.models;

public class Metadata {

    private Boolean otpExpired;
    private Integer hashesSigned;
    private Integer signedFailed;
    private Integer hashesReceived;

    public Boolean getOtpExpired() {
        return otpExpired;
    }
    public void setOtpExpired(Boolean otpExpired) {
        this.otpExpired = otpExpired;
    }

    public Integer getHashesSigned() {
        return hashesSigned;
    }
    public void setHashesSigned(Integer hashesSigned) {
        this.hashesSigned = hashesSigned;
    }

    public Integer getSignedFailed() {
        return signedFailed;
    }
    public void setSignedFailed(Integer signedFailed) {
        this.signedFailed = signedFailed;
    }

    public Integer getHashesReceived() {
        return hashesReceived;
    }
    public void setHashesReceived(Integer hashesReceived) {
        this.hashesReceived = hashesReceived;
    }
}
