package cl.uchile.fea.segpres.models;

import java.util.List;

public class SignatureResponse {

    private List<HashResponse> hashes;
    private Metadata metadata;
    private Long idSolicitud;

    public List<HashResponse> getHashes() {
        return hashes;
    }
    public void setHashes(List<HashResponse> hashes) {
        this.hashes = hashes;
    }

    public Metadata getMetadata() {
        return metadata;
    }
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Long getIdSolicitud() {
        return idSolicitud;
    }
    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }
}
