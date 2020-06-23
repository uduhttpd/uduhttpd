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

import org.nanohttpd.protocols.http.ConnectionClosedException;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.HTTPSessionImpl;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.tempfiles.TempFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
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

    protected HTTPSession createSession(NanoHTTPD server, TempFileManager tempFileManager, InputStream inputStream,
                                        OutputStream outputStream, InetAddress clientAddress) {
        return new HTTPSessionImpl(server, tempFileManager, inputStream, outputStream, clientAddress);
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

            while (!clientSocket.isClosed()) {
                createSession(server, tempFileManager, inputStream, outputStream, clientSocket.getInetAddress()).execute();
            }
        } catch (ConnectionClosedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client exited unexpectedly.", e);
        } finally {
            NanoHTTPD.safeClose(this);
        }
    }
}
