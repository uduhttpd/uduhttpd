package org.nanohttpd.protocols.http.response;

public class StatusCodes {
    public static StatusCode of(int statusCode) throws UnknownStatusCodeException {
        for (DefaultStatusCode defaultStatusCode : DefaultStatusCode.values())
            if (defaultStatusCode.getStatusCode() == statusCode)
                return defaultStatusCode;

        throw new UnknownStatusCodeException(statusCode);
    }

    public static StatusCode of(int statusCode, String httpDescription) {
        try {
            return of(statusCode);
        } catch (UnknownStatusCodeException e) {
            return new UndefinedStatusCode(statusCode, httpDescription);
        }
    }

    public static String toHttpStatusString(StatusCode statusCode) {
        return statusCode.getStatusCode() + " " + statusCode.getDescription();
    }
}
