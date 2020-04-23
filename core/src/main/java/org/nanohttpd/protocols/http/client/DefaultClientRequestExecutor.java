package org.nanohttpd.protocols.http.client;

import org.nanohttpd.protocols.http.HTTPSessionImpl;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.tempfiles.TempFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

public class DefaultClientRequestExecutor implements ClientRequestExecutor {
    private final NanoHTTPD server;

    private final Socket clientSocket;

    public DefaultClientRequestExecutor(NanoHTTPD server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void close() throws IOException {
        clientSocket.close();
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void run() {

        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            TempFileManager tempFileManager = server.getTempFileManagerFactory().create();
            HTTPSessionImpl session = new HTTPSessionImpl(server, tempFileManager, inputStream, outputStream,
                    clientSocket.getInetAddress());
            // FIXME: 23.04.2020 Why is this run in a loop?
            while (!clientSocket.isClosed() || !Thread.interrupted()) {
                session.execute();
            }
        } catch (Exception e) {
            // When the socket is closed by the client, we throw our own SocketException to break the "keep alive" loop
            // above. If the exception was anything other than the expected SocketException OR a SocketTimeoutException,
            // print the stacktrace.
            if (!(e instanceof SocketException && "NanoHttpd Shutdown".equals(e.getMessage())) && !(e instanceof SocketTimeoutException)) {
                NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client broken, or an bug in the handler code", e);
            }
        } finally {
            NanoHTTPD.safeClose(this);
        }
    }
}
