package cl.uchile.fea.segpres.models;

import java.util.List;

public class SignatureRequest {

    private String apiTokenKey;
    private String token;
    private List<HashRequest> hashes;

    public String getApiTokenKey() {
        return apiTokenKey;
    }
    public void setApiTokenKey(String apiTokenKey) {
        this.apiTokenKey = apiTokenKey;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public List<HashRequest> getHashes() {
        return hashes;
    }
    public void setHashes(List<HashRequest> hashes) {
        this.hashes = hashes;
    }
}
