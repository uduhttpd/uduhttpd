package org.nanohttpd.protocols.http.server;

import org.nanohttpd.protocols.http.NanoHTTPD;

import java.io.IOException;
import java.net.ServerSocket;

public class DefaultServerExecutor extends NanoHTTPD.ServerExecutor {
    private final NanoHTTPD server;

    public DefaultServerExecutor(NanoHTTPD server) {
        this.server = server;
    }

    @Override
    protected NanoHTTPD getServer() {
        return server;
    }

    @Override
    protected void serve(ServerSocket serverSocket) {
        do {
            try {
                serverSocket.setReuseAddress(true);
                server.handleConnectionRequest(serverSocket.accept());
            } catch (IOException e) {
                if (!serverSocket.isClosed())
                    e.printStackTrace();
            }
        } while (!serverSocket.isClosed() && !Thread.interrupted());
    }
}
