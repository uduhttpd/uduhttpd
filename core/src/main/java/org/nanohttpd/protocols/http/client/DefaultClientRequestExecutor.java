/*
 * Copyright (C) 2020 uduhttpd
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.nanohttpd.protocols.http.client;

import org.apache.http.ConnectionClosedException;
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
            NanoHTTPD.LOG.info("Start handling");
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            TempFileManager tempFileManager = server.getTempFileManagerFactory().create();
            HTTPSessionImpl session = new HTTPSessionImpl(server, tempFileManager, inputStream, outputStream,
                    clientSocket.getInetAddress());
            // FIXME: 23.04.2020 Why is this run in a loop?
            while (!clientSocket.isClosed()) {
                NanoHTTPD.LOG.info("Execute");
                session.execute();
            }
        } catch (ConnectionClosedException ignored) {

        } catch (Exception e) {
            // When the socket is closed by the client, we throw our own SocketException to break the "keep alive" loop
            // above. If the exception was anything other than the expected SocketException OR a SocketTimeoutException,
            // print the stacktrace.
            if (!(e instanceof SocketException && "NanoHttpd Shutdown".equals(e.getMessage())) && !(e instanceof SocketTimeoutException)) {
                NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client broken, or an bug in the handler code", e);
            }

            e.printStackTrace();
        } finally {
            NanoHTTPD.safeClose(this);
        }
    }
}
