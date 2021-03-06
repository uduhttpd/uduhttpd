/*
 * Copyright (C) 2012 - 2016 nanohttpd (TestHttpServer.java)
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

package org.nanohttpd.junit.webserver;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.server.ServerStartException;
import org.nanohttpd.webserver.SimpleWebServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.*;

@FixMethodOrder
public class TestHttpServer extends AbstractTestHttpServer {
    private static SimpleWebServer webServer;

    @BeforeClass
    public static void setUp() throws Exception {
        String[] args = {"--host", "localhost", "--port", "9090", "--dir", "src/test/resources"};
        webServer = SimpleWebServer.start(args);
        Assert.assertTrue(webServer.isListening());
    }

    @AfterClass
    public static void tearDown() {
        webServer.stop();
        Assert.assertFalse(webServer.isListening());
    }

    @Test
    public void test0MakeMultipleRequests() throws IOException, ServerStartException {
        NanoHTTPD server1 = new NanoHTTPD(3995) {

        };

        NanoHTTPD server2 = new NanoHTTPD(3996) {

        };

        server1.start();
        server2.start();

        NanoHTTPD[] servers = {server1, server2};

        for (int i = 0; i < 100; i++) {
            int chosen = (int) Math.round(Math.random() * (servers.length - 1));
            NanoHTTPD server = servers[chosen];
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet("http://localhost:" + server.getListeningPort());
            CloseableHttpResponse response = httpclient.execute(httpget);
            Assert.assertEquals(404, response.getStatusLine().getStatusCode());
            response.close();
        }

        server1.stop();
        server2.stop();
    }

    @Test
    public void doTest404() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/xxx/yyy.html");
        CloseableHttpResponse response = httpclient.execute(httpget);
        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
        response.close();
    }

    @Test
    public void doPlugin() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/index.xml");
        CloseableHttpResponse response = httpclient.execute(httpget);
        String string = new String(readContents(response.getEntity()), StandardCharsets.UTF_8);
        Assert.assertEquals("<xml/>", string);
        response.close();

        httpget = new HttpGet("http://localhost:9090/testdir/testdir/different.xml");
        response = httpclient.execute(httpget);
        string = new String(readContents(response.getEntity()), StandardCharsets.UTF_8);
        Assert.assertEquals("<xml/>", string);
        response.close();
    }

    @Test
    public void doSomeBasicTest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/testdir/test.html");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), StandardCharsets.UTF_8);
        Assert.assertEquals("<html>\n<head>\n<title>dummy</title>\n</head>\n<body>\n<h1>it works</h1>" +
                "\n</body>\n</html>", string);
        response.close();

        httpget = new HttpGet("http://localhost:9090/");
        response = httpclient.execute(httpget);
        entity = response.getEntity();
        string = new String(readContents(entity), StandardCharsets.UTF_8);
        Assert.assertTrue(string.indexOf("testdir") > 0);
        response.close();

        httpget = new HttpGet("http://localhost:9090/testdir");
        response = httpclient.execute(httpget);
        entity = response.getEntity();
        string = new String(readContents(entity), StandardCharsets.UTF_8);
        Assert.assertTrue(string.indexOf("test.html") > 0);
        response.close();

        httpget = new HttpGet("http://localhost:9090/testdir/testpdf.pdf");
        response = httpclient.execute(httpget);
        entity = response.getEntity();

        byte[] actual = readContents(entity);
        byte[] expected = readContents(new FileInputStream("src/test/resources/testdir/testpdf.pdf"));
        Assert.assertArrayEquals(expected, actual);
        response.close();

    }

    @Test
    public void doArgumentTest() throws InterruptedException, IOException {
        final String testPort = "9458";
        Thread testServer = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] args = {"-h", "localhost", "-p", testPort, "-d", "src/test/resources"};
                try {
                    SimpleWebServer.main(args);
                } catch (IOException | ServerStartException e) {
                    e.printStackTrace();
                }
            }
        });

        testServer.start();
        Thread.sleep(200);

        HttpGet httpget = new HttpGet("http://localhost:" + testPort + "/");
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            HttpEntity entity = response.getEntity();
            String str = new String(readContents(entity), StandardCharsets.UTF_8);
            Assert.assertTrue("The response entity didn't contain the string 'testdir'", str.contains("testdir"));
        }
    }

    @Test
    public void testURLContainsParentDirectory() throws IOException {
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:9090/../test.html");
            response = httpClient.execute(httpGet);
            Assert.assertEquals("The response status should be 403(Forbidden), since the server won't serve " +
                    "requests with '../' due to security reasons", 403, response.getStatusLine().getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testIndexFileIsShownWhenURLEndsWithDirectory() throws IOException {
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/testdir");
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = new String(readContents(entity), StandardCharsets.UTF_8);
            Assert.assertThat("When the URL ends with a directory, and if an index.html file is present in that" +
                    " directory, the server should respond with that file", responseString, containsString(
                    "Simple index file"));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testPluginInternalRewrite() throws IOException {
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("http://localhost:9090/rewrite/index.xml");
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = new String(readContents(entity), StandardCharsets.UTF_8);
            Assert.assertThat("If a plugin returns an InternalRewrite from the serveFile method, the rewritten" +
                    " request should be served", responseString, allOf(containsString("dummy"),
                    containsString("it works")));
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testRangeHeaderWithStartPositionOnly() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/test.html");
            httpGet.addHeader("range", "bytes=10-");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = new String(readContents(entity), StandardCharsets.UTF_8);
            Assert.assertThat("The data from the beginning of the file should have been skipped as specified " +
                    "in the 'range' header", responseString, not(containsString("<head>")));
            Assert.assertThat("The response should contain the data from the end of the file since end " +
                    "position was not given in the 'range' header", responseString, containsString("</head>"));
            Assert.assertEquals("The content length should be the length starting from the requested byte",
                    "73", response.getHeaders("Content-Length")[0].getValue());
            Assert.assertEquals("The 'Content-Range' header should contain the correct lengths and offsets " +
                            "based on the range served", "bytes 10-82/83",
                    response.getHeaders("Content-Range")[0].getValue());
            Assert.assertEquals("Response status for a successful range request should be PARTIAL_CONTENT(206)",
                    206, response.getStatusLine().getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testRangeStartGreaterThanFileLength() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/test.html");
            httpGet.addHeader("range", "bytes=1000-");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            Assert.assertEquals("Response status for a request with 'range' header value which exceeds file " +
                            "length should be RANGE_NOT_SATISFIABLE(416)", 416,
                    response.getStatusLine().getStatusCode());
            Assert.assertEquals("The 'Content-Range' header should contain the correct lengths and offsets " +
                            "based on the range served", "bytes */83",
                    response.getHeaders("Content-Range")[0].getValue());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testRangeHeaderWithStartAndEndPosition() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/test.html");
            httpGet.addHeader("range", "bytes=10-40");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String responseString = new String(readContents(entity), StandardCharsets.UTF_8);
            Assert.assertThat("The data from the beginning of the file should have been skipped as specified " +
                            "in the 'range' header", responseString,
                    not(containsString("<head>")));
            Assert.assertThat("The data from the end of the file should have been skipped as specified in the " +
                    "'range' header", responseString, not(containsString("</head>")));
            Assert.assertEquals("The 'Content-Length' should be the length from the requested start position " +
                            "to end position", "31",
                    response.getHeaders("Content-Length")[0].getValue());
            Assert.assertEquals("The 'Contnet-Range' header should contain the correct lengths and offsets " +
                            "based on the range served", "bytes 10-40/83",
                    response.getHeaders("Content-Range")[0].getValue());
            Assert.assertEquals("Response status for a successful request with 'range' header should be " +
                    "PARTIAL_CONTENT(206)", 206, response.getStatusLine().getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testIfNoneMatchHeader() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/test.html");
            httpGet.addHeader("if-none-match", "*");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            Assert.assertEquals("The response status to a reqeuest with 'if-non-match=*' header should be " +
                    "NOT_MODIFIED(304), if the file exists", 304, response.getStatusLine().getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Test
    public void testRangeHeaderAndIfNoneMatchHeader() throws IOException {
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet("http://localhost:9090/testdir/test.html");
            httpGet.addHeader("range", "bytes=10-20");
            httpGet.addHeader("if-none-match", "*");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            Assert.assertEquals("The response status to a reqeuest with 'if-non-match=*' header and 'range' " +
                    "header should be NOT_MODIFIED(304), if the file exists, because 'if-non-match' header should be" +
                    " given priority", 304, response.getStatusLine().getStatusCode());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
