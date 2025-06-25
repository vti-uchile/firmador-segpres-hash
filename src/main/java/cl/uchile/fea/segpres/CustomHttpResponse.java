package cl.uchile.fea.segpres;

/**
 * The custom HTTP response.
 */
public class CustomHttpResponse {

    /**
     * The HTTP status code.
     */
    public final int code;
    /**
     * The HTTP body.
     */
    public final String body;

    public CustomHttpResponse(int code, String body) {
        this.code = code;
        this.body = body;
    }

    /**
     * Gets the HTTP status code.
     * @return The code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the HTTP body.
     * @return The body
     */
    public String getBody() {
        return body;
    }
}
