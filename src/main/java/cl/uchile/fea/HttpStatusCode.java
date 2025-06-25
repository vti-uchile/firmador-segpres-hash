package cl.uchile.fea;

import org.apache.http.HttpStatus;

/**
 * The HTTP status code.
 */
public enum HttpStatusCode {

    // 1xx: Informational

    CONTINUE(HttpStatus.SC_CONTINUE, "Continue"),
    SWITCHING_PROTOCOLS(HttpStatus.SC_SWITCHING_PROTOCOLS, "Switching Protocols"),
    PROCESSING(HttpStatus.SC_PROCESSING, "Processing"),
    //EARLY_HINTS(103, "Early Hints"),

    // 2xx: Success

    OK(HttpStatus.SC_OK, "OK"),
    CREATED(HttpStatus.SC_CREATED, "Created"),
    ACCEPTED(HttpStatus.SC_ACCEPTED, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Non Authoritative Information"),
    NO_CONTENT(HttpStatus.SC_NO_CONTENT, "No Content"),
    RESET_CONTENT(HttpStatus.SC_RESET_CONTENT, "Reset Content"),
    PARTIAL_CONTENT(HttpStatus.SC_PARTIAL_CONTENT, "Partial Content"),
    MULTI_STATUS(HttpStatus.SC_MULTI_STATUS, "Multi-Status"),
    //ALREADY_REPORTED(208, "Already Reported"),
    //IM_USED(226, "IM Used"),

    // 3xx: Redirection

    MULTIPLE_CHOICES(HttpStatus.SC_MULTIPLE_CHOICES, "Multiple Choice"),
    MOVED_PERMANENTLY(HttpStatus.SC_MOVED_PERMANENTLY, "Moved Permanently"),
    MOVED_TEMPORARILY(HttpStatus.SC_MOVED_TEMPORARILY, "Moved Temporarily"), // Found
    SEE_OTHER(HttpStatus.SC_SEE_OTHER, "See Other"),
    NOT_MODIFIED(HttpStatus.SC_NOT_MODIFIED, "Not Modified"),
    USE_PROXY(HttpStatus.SC_USE_PROXY, "Use Proxy"),
    TEMPORARY_REDIRECT(HttpStatus.SC_TEMPORARY_REDIRECT, "Temporary Redirect"),
    //PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // 4xx: Client Error

    BAD_REQUEST(HttpStatus.SC_BAD_REQUEST, "Bad Request"),
    UNAUTHORIZED(HttpStatus.SC_UNAUTHORIZED, "Unauthorized"),
    PAYMENT_REQUIRED(HttpStatus.SC_PAYMENT_REQUIRED, "Payment Required"),
    FORBIDDEN(HttpStatus.SC_FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.SC_NOT_FOUND, "Not Found"),
    METHOD_NOT_ALLOWED(HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed"),
    NOT_ACCEPTABLE(HttpStatus.SC_NOT_ACCEPTABLE, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED, "Proxy Authentication Required"),
    REQUEST_TIMEOUT(HttpStatus.SC_REQUEST_TIMEOUT, "Request Timeout"),
    CONFLICT(HttpStatus.SC_CONFLICT, "Conflict"),
    GONE(HttpStatus.SC_GONE, "Gone"),
    LENGTH_REQUIRED(HttpStatus.SC_LENGTH_REQUIRED, "Length Required"),
    PRECONDITION_FAILED(HttpStatus.SC_PRECONDITION_FAILED, "Precondition Failed"),
    REQUEST_TOO_LONG(HttpStatus.SC_REQUEST_TOO_LONG, "Request Entity Too Large"),
    REQUEST_URI_TOO_LONG(HttpStatus.SC_REQUEST_URI_TOO_LONG, "Request-URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"),
    REQUESTED_RANGE_NOT_SATISFIABLE(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable"),
    EXPECTATION_FAILED(HttpStatus.SC_EXPECTATION_FAILED, "Expectation Failed"),
    //UNPROCESSABLE_ENTITY(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity"),
    INSUFFICIENT_SPACE_ON_RESOURCE(HttpStatus.SC_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space on Resource"),
    METHOD_FAILURE(HttpStatus.SC_METHOD_FAILURE, "Method Failure"),
    //MISDIRECTED_REQUEST(421, "Misdirected Request"),
    UNPROCESSABLE_ENTITY(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity"),
    LOCKED(HttpStatus.SC_LOCKED, "Locked"),
    FAILED_DEPENDENCY(HttpStatus.SC_FAILED_DEPENDENCY, "Failed Dependency"),
    //TOO_EARLY(425, "Too Early"),
    //UPGRADE_REQUIRED(426, "Upgrade Required"),
    //PRECONDITION_REQUIRED(428, "Precondition Required"),
    TOO_MANY_REQUESTS(HttpStatus.SC_TOO_MANY_REQUESTS, "Too Many Requests"),
    //REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    //UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    // 5xx: Server Error

    INTERNAL_SERVER_ERROR(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error"),
    NOT_IMPLEMENTED(HttpStatus.SC_NOT_IMPLEMENTED, "Not Implemented"),
    BAD_GATEWAY(HttpStatus.SC_BAD_GATEWAY, "Bad Gateway"),
    SERVICE_UNAVAILABLE(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service Unavailable"),
    GATEWAY_TIMEOUT(HttpStatus.SC_GATEWAY_TIMEOUT, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED, "HTTP Version Not Supported"),
    //VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
    INSUFFICIENT_STORAGE(HttpStatus.SC_INSUFFICIENT_STORAGE, "Insufficient Storage");
    //LOOP_DETECTED(508, "Loop Detected"),
    //NOT_EXTENDED(510, "Not Extended"),
    //NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    /**
     * The code.
     */
    private final int code;
    /**
     * The description.
     */
    private final String description;

    HttpStatusCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the code.
     * @return The code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the description.
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + " " + description;
    }

    /**
     * Gets the HTTP status by code.
     * @param code The code
     * @return The status
     */
    public static HttpStatusCode getByCode(int code) {
        for (HttpStatusCode status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid status code: " + code);
    }
}
