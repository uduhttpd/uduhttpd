package org.nanohttpd.protocols.http.response;

// TODO: 20.04.2020 Need to be based on HttpException of some sort
public class UnknownStatusCodeException extends Exception {
    private final int mStatusCode;

    public UnknownStatusCodeException(int statusCode) {
        mStatusCode = statusCode;
    }

    public int getStatusCode() {
        return mStatusCode;
    }
}
