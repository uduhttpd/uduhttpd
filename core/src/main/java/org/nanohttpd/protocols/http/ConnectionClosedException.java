package org.nanohttpd.protocols.http;

import java.io.IOException;


/**
 * When a HTTP connection is closed, this will be used to indicate the reason.
 */
public class ConnectionClosedException extends IOException {

    /**
     * Create an instance with no explanation.
     */
    public ConnectionClosedException() {
        super();
    }

    /**
     * Create an instance with message.
     *
     * @param message the message explaining the reason.
     */
    public ConnectionClosedException(String message) {
        super(message);
    }

    /**
     * Create an instance with message and cause.
     *
     * @param message the message explaining the cause.
     * @param cause   the cause that led to connection closed.
     */
    public ConnectionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
