package org.nanohttpd.protocols.http.client;

import java.io.Closeable;
import java.net.Socket;

public interface ClientRequestExecutor extends Closeable, Runnable {
    Socket getClientSocket();
}
