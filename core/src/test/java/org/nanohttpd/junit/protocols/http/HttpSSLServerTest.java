/*
 * Copyright (C) 2012 - 2016 nanohttpd (HttpSSLServerTest.java)
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.sockets.SecureServerSocketFactory;
import org.nanohttpd.protocols.http.sockets.SecureSockets;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class HttpSSLServerTest extends HttpServerTest {

    @Override
    protected TestServer newServerInstance() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", new File("src/test/resources/keystore.jks").getAbsolutePath());
        SSLServerSocketFactory sslServerSocketFactory = SecureSockets.createServerSocketFactory(
                "/keystore.jks", "password".toCharArray());
        SecureServerSocketFactory serverSocketFactory = new SecureServerSocketFactory(null, 9043,
                NanoHTTPD.SOCKET_READ_TIMEOUT, sslServerSocketFactory, null);
        return new TestServer(serverSocketFactory);
    }

    @Test
    public void testSSLConnection() throws IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpTrace httphead = new HttpTrace("https://localhost:9043/index.html");
        HttpResponse response = httpclient.execute(httphead);
        HttpEntity entity = response.getEntity();
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        Assert.assertEquals(9043, this.testServer.getListeningPort());
        Assert.assertTrue(this.testServer.isListening());
    }


    /**
     * using http to connect to https.
     *
     * @throws ClientProtocolException
     * @throws IOException
     */
    // FIXME: 6/23/20 the ssl to http connection test
    /*
    @Test(expected = ClientProtocolException.class)
    public void testHttpOnSSLConnection() throws ClientProtocolException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpTrace httphead = new HttpTrace("http://localhost:9043/index.html");
        httpclient.execute(httphead);
    }*/

    @Before
    public void setUp() throws Exception {
        this.testServer = newServerInstance();
        this.tempFileManager = new TestTempFileManager();
        this.testServer.start();
    }

    @After
    public void stopServer() throws TimeoutException {
        this.testServer.stop();
    }
}
