package org.nanohttpd.protocols.http.server;

public class ServerStartException extends Exception {
    public ServerStartException(String s) {
        super(s);
    }

    public ServerStartException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
