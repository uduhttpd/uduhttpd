/*
 * Copyright (C) 2012 - 2016 nanohttpd (TestNanoFileUpload.java)
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

package org.nanohttpd.junit.fileupload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.nanohttpd.fileupload.NanoFileUpload;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.HTTPSessionImpl;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.DefaultStatusCode;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.server.ServerStartException;
import org.nanohttpd.protocols.http.tempfiles.TempFileManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * very strange but if the file upload is the first request the test fails.
 *
 * @author ritchieGitHub
 */
@FixMethodOrder
public class TestNanoFileUpload {

    private static final String UPLOAD_JAVA_FILE = "src/test/java/" + TestNanoFileUpload.class.getName().replace('.', '/') + ".java";

    private static final URL TEST_FILE1 = TestNanoFileUpload.class.getResource("/Smugglers 'n Caregivers 'n Loners \"Unchained\".txt");

    protected TestServer testServer;

    public static class TestServer extends NanoHTTPD {

        public Response response = Response.newFixedLengthResponse("");

        public String uri;

        public Method method;

        public Map<String, String> header;

        public Map<String, String> parms;

        public Map<String, List<FileItem>> files;

        public Map<String, List<String>> decodedParamters;

        public Map<String, List<String>> decodedParamtersFromParameter;

        public String queryParameterString;

        public TestServer() {
            super(8192);
            uploader = new NanoFileUpload(new DiskFileItemFactory());
        }

        public HTTPSessionImpl createSession(TempFileManager tempFileManager, InputStream inputStream,
                                             OutputStream outputStream) {
            return new HTTPSessionImpl(this, tempFileManager, inputStream, outputStream);
        }

        public HTTPSessionImpl createSession(TempFileManager tempFileManager, InputStream inputStream,
                                             OutputStream outputStream, InetAddress inetAddress) {
            return new HTTPSessionImpl(this, tempFileManager, inputStream, outputStream, inetAddress);
        }

        NanoFileUpload uploader;

        @Override
        public Response serve(HTTPSession session) {

            this.uri = session.getUri();
            this.method = session.getMethod();
            this.header = session.getHeaders();
            this.parms = session.getParms();
            if (NanoFileUpload.isMultipartContent(session)) {
                try {
                    if ("/uploadFile1".equals(this.uri)) {
                        session.getHeaders().put("content-length", "AA");
                        files = uploader.parseParameterMap(session);
                    }
                    if ("/uploadFile2".equals(this.uri)) {
                        files = new HashMap<>();
                        List<FileItem> parseRequest = uploader.parseRequest(session);
                        files.put(parseRequest.get(0).getFieldName(), parseRequest);
                    }
                    if ("/uploadFile3".equals(this.uri)) {
                        files = new HashMap<>();
                        FileItemIterator iter = uploader.getItemIterator(session);
                        while (iter.hasNext()) {
                            FileItemStream item = iter.next();
                            final String fileName = item.getName();
                            FileItem fileItem = uploader.getFileItemFactory().createItem(item.getFieldName(),
                                    item.getContentType(), item.isFormField(), fileName);
                            files.put(fileItem.getFieldName(), Collections.singletonList(fileItem));
                            try {
                                Streams.copy(item.openStream(), fileItem.getOutputStream(), true);
                            } catch (Exception e) {
                            }
                            fileItem.setHeaders(item.getHeaders());
                        }
                    }
                } catch (Exception e) {
                    this.response.setStatus(DefaultStatusCode.INTERNAL_ERROR);
                    e.printStackTrace();
                }
            }
            this.queryParameterString = session.getQueryParameterString();
            this.decodedParamtersFromParameter = decodeParameters(this.queryParameterString);
            this.decodedParamters = decodeParameters(session.getQueryParameterString());
            return this.response;
        }

    }

    @Test
    public void testNormalRequest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpTrace httphead = new HttpTrace("http://localhost:8192/index.html");
        CloseableHttpResponse response = httpclient.execute(httphead);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        response.close();
    }

    @Test
    public void testPostWithMultipartFormUpload1() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String textFileName = UPLOAD_JAVA_FILE;
        HttpPost post = new HttpPost("http://localhost:8192/uploadFile1");

        executeUpload(httpclient, textFileName, post);
        FileItem file = this.testServer.files.get("upfile").get(0);
        Assert.assertEquals(file.getSize(), new File(textFileName).length());
    }

    @Test
    public void testPostSpecialCharactersWithMultipartFormUpload() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://localhost:8192/uploadFile2");
        String testFile = URLDecoder.decode(TEST_FILE1.getFile(), "UTF-8");

        executeUpload(httpclient, testFile, post);
        FileItem file = this.testServer.files.get("upfile").get(0);
        Assert.assertEquals(file.getName(), new File(testFile).getName());
    }

    @Test
    public void testPostWithMultipartFormUpload2() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String textFileName = UPLOAD_JAVA_FILE;
        HttpPost post = new HttpPost("http://localhost:8192/uploadFile2");

        executeUpload(httpclient, textFileName, post);
        FileItem file = this.testServer.files.get("upfile").get(0);
        Assert.assertEquals(file.getSize(), new File(textFileName).length());
    }

    @Test
    public void testPostWithMultipartFormUpload3() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String textFileName = UPLOAD_JAVA_FILE;
        HttpPost post = new HttpPost("http://localhost:8192/uploadFile3");

        executeUpload(httpclient, textFileName, post);
        FileItem file = this.testServer.files.get("upfile").get(0);
        Assert.assertEquals(file.getSize(), new File(textFileName).length());
    }

    private void executeUpload(CloseableHttpClient httpclient, String textFileName, HttpPost post) throws IOException {
        FileBody fileBody = new FileBody(new File(textFileName), ContentType.DEFAULT_BINARY);
        StringBody stringBody1 = new StringBody("Message 1", ContentType.MULTIPART_FORM_DATA);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        builder.addPart("text1", stringBody1);
        HttpEntity entity = builder.build();

        post.setEntity(entity);
        HttpResponse response = httpclient.execute(post);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }

    @Before
    public void setUp() throws ServerStartException {
        this.testServer = new TestServer();
        this.testServer.start();
    }

    @After
    public void tearDown() {
        this.testServer.stop();
    }

}
