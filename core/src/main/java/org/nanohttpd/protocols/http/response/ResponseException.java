package org.nanohttpd.protocols.http.response;

public class ResponseException extends Exception {

    private static final long serialVersionUID = 6569838532917408380L;

    private final StatusCode mStatusCode;

    public ResponseException(StatusCode status, String message) {
        super(message);
        mStatusCode = status;
    }

    public ResponseException(StatusCode status, String message, Exception e) {
        super(message, e);
        mStatusCode = status;
    }

    public StatusCode getStatus() {
        return mStatusCode;
    }
}