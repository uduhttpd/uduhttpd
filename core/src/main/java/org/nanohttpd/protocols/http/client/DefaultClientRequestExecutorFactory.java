package org.nanohttpd.protocols.http.client;

import org.nanohttpd.protocols.http.NanoHTTPD;

import java.net.Socket;

public class DefaultClientRequestExecutorFactory implements ClientRequestExecutorFactory {
    @Override
    public ClientRequestExecutor create(NanoHTTPD server, Socket clientSocket) {
        return new DefaultClientRequestExecutor(server, clientSocket);
    }
}
