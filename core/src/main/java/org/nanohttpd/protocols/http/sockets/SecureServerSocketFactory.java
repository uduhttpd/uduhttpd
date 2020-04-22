package org.nanohttpd.protocols.http.sockets;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2016 nanohttpd
 * %%
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
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import org.nanohttpd.protocols.http.NanoHTTPD;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Creates a new SSLServerSocket
 */
public class SecureServerSocketFactory extends ServerSocketFactoryImpl {
    private final SSLServerSocketFactory mServerSocketFactory;
    private final String[] mSslProtocols;

    public SecureServerSocketFactory(InetAddress bindAddress, int bindPort, int timeout,
                                     SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
        super(bindAddress, bindPort, timeout);
        mServerSocketFactory = sslServerSocketFactory;
        mSslProtocols = sslProtocols;
    }

    public SecureServerSocketFactory(SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
        this(null, 0 , NanoHTTPD.SOCKET_READ_TIMEOUT, sslServerSocketFactory, sslProtocols);
    }

    @Override
    public ServerSocket create() throws IOException {
        SSLServerSocket serverSocket = (SSLServerSocket) mServerSocketFactory.createServerSocket(
                getBindPort(), 0, getBindAddress());
        serverSocket.setSoTimeout(getSoTimeout());
        serverSocket.setEnabledProtocols(mSslProtocols != null ? mSslProtocols : serverSocket.getSupportedProtocols());
        serverSocket.setUseClientMode(false);
        serverSocket.setWantClientAuth(false);
        serverSocket.setNeedClientAuth(false);
        return serverSocket;
    }
}
