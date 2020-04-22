package org.nanohttpd.protocols.http.client;

import org.nanohttpd.protocols.http.NanoHTTPD;

import java.net.Socket;

public interface ClientRequestExecutorFactory {
    ClientRequestExecutor create(NanoHTTPD server, Socket clientSocket);
}
