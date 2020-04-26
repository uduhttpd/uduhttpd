/*
 * Copyright (C) 2012 - 2016 nanohttpd (ServerSocketFactoryTest.java)
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

package org.nanohttpd.junit.protocols.http;

import org.junit.Assert;
import org.junit.Test;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.sockets.DefaultServerSocketFactory;
import org.nanohttpd.protocols.http.sockets.SecureServerSocketFactory;
import org.nanohttpd.protocols.http.sockets.ServerSocketFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerSocketFactoryTest extends NanoHTTPD {

    public static final int PORT = 8192;

    public ServerSocketFactoryTest() {
        super(new TestFactory());
    }

    @Test
    public void isCustomServerSocketFactory() {
        Assert.assertTrue(getServerSocketFactory() instanceof TestFactory);
    }

    @Test
    public void testCreateServerSocket() throws IOException {
        Assert.assertNotNull(getServerSocketFactory().create());
    }

    @Test
    public void testSSLServerSocketFail() {
        String[] protocols = {""};
        System.setProperty("javax.net.ssl.trustStore", new File("src/test/resources/keystore.jks").getAbsolutePath());
        ServerSocketFactory ssFactory = new SecureServerSocketFactory(null, protocols);
        try {
            ssFactory.create();
            Assert.fail();
        } catch (Exception ignored) {
        }
    }

    private static class TestFactory extends DefaultServerSocketFactory {
        public TestFactory() {
            super(null, PORT, NanoHTTPD.SOCKET_READ_TIMEOUT);
        }

        @Override
        public ServerSocket create() {
            try {
                return new ServerSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
