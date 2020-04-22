package org.nanohttpd.protocols.http;

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

import org.nanohttpd.protocols.http.response.DefaultStatusCode;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.sockets.DefaultServerSocketFactory;
import org.nanohttpd.protocols.http.sockets.ServerSocketFactory;
import org.nanohttpd.protocols.http.tempfiles.DefaultTempFileManagerFactory;
import org.nanohttpd.protocols.http.tempfiles.TempFileManager;
import org.nanohttpd.protocols.http.threading.AsyncRunner;
import org.nanohttpd.protocols.http.threading.DefaultAsyncRunner;
import org.nanohttpd.util.Factory;
import org.nanohttpd.util.FactoryThrowing;
import org.nanohttpd.util.Handler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p>
 * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen,
 * 2010 by Konstantinos Togias
 * </p>
 * <p/>
 * <p/>
 * <b>Features + limitations: </b>
 * <ul>
 * <p/>
 * <li>Only one Java file</li>
 * <li>Java 5 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if
 * you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT
 * support in 1.25)</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Supports file upload (since version 1.2, 2010)</li>
 * <li>Supports partial content (streaming)</li>
 * <li>Supports ETags</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server supports partial content (streaming)</li>
 * <li>File server supports ETags</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common MIME types</li>
 * <li>All header names are converted to lower case so they don't vary between
 * browsers/clients</li>
 * <p/>
 * </ul>
 * <p/>
 * <p/>
 * <b>How to use: </b>
 * <ul>
 * <p/>
 * <li>Subclass and implement serve() and embed to your own program</li>
 * <p/>
 * </ul>
 * <p/>
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD
 * licence)
 */
public abstract class NanoHTTPD {

    public static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";

    public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";

    public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";

    public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     * This is required as the Keep-Alive HTTP connections would otherwise block
     * the socket reading thread forever (or as long the browser is open).
     */
    public static final int SOCKET_READ_TIMEOUT = 5000;

    /**
     * Common MIME type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";

    /**
     * Common MIME type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";

    /**
     * Pseudo-Parameter to use to store the actual query string in the
     * parameters map for later re-processing.
     */
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";

    /**
     * logger to log to.
     */
    public static final Logger LOG = Logger.getLogger(NanoHTTPD.class.getName());

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    protected static Map<String, String> MIME_TYPES;

    public static Map<String, String> mimeTypes() {
        if (MIME_TYPES == null) {
            MIME_TYPES = new HashMap<>();
            loadMimeTypes(MIME_TYPES, "META-INF/nanohttpd/default-mimetypes.properties");
            loadMimeTypes(MIME_TYPES, "META-INF/nanohttpd/mimetypes.properties");
            if (MIME_TYPES.isEmpty()) {
                LOG.log(Level.WARNING, "no mime types found in the classpath! please provide mimetypes.properties");
            }
        }
        return MIME_TYPES;
    }

    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    private static void loadMimeTypes(Map<String, String> result, String resourceName) {
        try {
            Enumeration<URL> resources = NanoHTTPD.class.getClassLoader().getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                Properties properties = new Properties();
                InputStream stream = null;
                try {
                    stream = url.openStream();
                    properties.load(stream);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "could not load mimetypes from " + url, e);
                } finally {
                    safeClose(stream);
                }
                result.putAll((Map) properties);
            }
        } catch (IOException e) {
            LOG.log(Level.INFO, "no mime types available at " + resourceName);
        }
    }

    /**
     * Get MIME type from file name extension, if possible
     *
     * @param uri the string representing a file
     * @return the connected mime/type
     */
    public static String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = mimeTypes().get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? "application/octet-stream" : mime;
    }

    public static void safeClose(Object closeable) {
        try {
            if (closeable != null) {
                if (closeable instanceof Closeable) {
                    ((Closeable) closeable).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close");
                }
            }
        } catch (IOException e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Could not close", e);
        }
    }

    private ServerSocket mServerSocket;

    public ServerSocket getServerSocket() {
        return mServerSocket;
    }

    private ServerSocketFactory mServerSocketFactory;

    private Thread mServerThread;

    private Handler<HTTPSession, Response> httpHandler;

    protected List<Handler<HTTPSession, Response>> interceptors = new ArrayList<>(4);

    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    protected AsyncRunner asyncRunner;

    private Factory<TempFileManager> mTempFileManagerFactory;

    public NanoHTTPD() {
        this(0);
    }

    public NanoHTTPD(int port) {
        this(port, SOCKET_READ_TIMEOUT);
    }

    public NanoHTTPD(int port, int timeout) {
        this((InetAddress) null, port, timeout);
    }

    public NanoHTTPD(String host, int port) throws UnknownHostException {
        this(host, port, SOCKET_READ_TIMEOUT);
    }

    public NanoHTTPD(String host, int port, int timeout) throws UnknownHostException {
        this(InetAddress.getByName(host), port, timeout);
    }

    public NanoHTTPD(InetAddress address, int port, int timeout) {
        this(new DefaultServerSocketFactory(address, port, timeout));
    }

    public NanoHTTPD(ServerSocketFactory socketFactory) {
        if (socketFactory == null)
            throw new NullPointerException("Socket factory cannot be null.");

        mServerSocketFactory = socketFactory;
        setTempFileManagerFactory(new DefaultTempFileManagerFactory());
        setAsyncRunner(new DefaultAsyncRunner());

        // creates a default handler that redirects to deprecated serve();
        this.httpHandler = new Handler<HTTPSession, Response>() {

            @Override
            public Response handle(HTTPSession input) {
                return NanoHTTPD.this.serve(input);
            }
        };
    }

    public void setHTTPHandler(Handler<HTTPSession, Response> handler) {
        this.httpHandler = handler;
    }

    public void addHTTPInterceptor(Handler<HTTPSession, Response> interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * Forcibly closes all connections that are open.
     */
    public synchronized void closeAllConnections() {
        stop();
    }

    /**
     * create a instance of the client handler, subclasses can return a subclass
     * of the ClientHandler.
     *
     * @param clientSocket the socket representing the client
     * @param inputStream  the input stream
     * @return the client handler
     */
    protected ClientHandler createClientHandler(final Socket clientSocket, final InputStream inputStream) {
        return new ClientHandler(this, inputStream, clientSocket);
    }

    /**
     * Instantiate the server runnable, can be overwritten by subclasses to
     * provide a subclass of the ServerRunnable.
     *
     * @return the server runnable.
     */
    // TODO: 22.04.2020 Improve the usage of ServerRunnable
    protected ServerRunnable createServerRunnable() {
        return new ServerRunnable(this);
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter
     * name might have been supplied several times, by return lists of values.
     * In general these lists will contain a single element.
     *
     * @param parms original <b>NanoHTTPD</b> parameters values, as passed to the
     *              <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to
     * <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected static Map<String, List<String>> decodeParameters(Map<String, String> parms) {
        return decodeParameters(parms.get(NanoHTTPD.QUERY_STRING_PARAMETER));
    }

    // -------------------------------------------------------------------------------
    // //

    /**
     * Decode parameters from a URL, handing the case where a single parameter
     * name might have been supplied several times, by return lists of values.
     * In general these lists will contain a single element.
     *
     * @param queryString a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to
     * <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected static Map<String, List<String>> decodeParameters(String queryString) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = sep >= 0 ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<String>());
                }
                String propertyValue = sep >= 0 ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes
     * "foo bar"
     */
    public static String decodePercent(String str) {
        try {
            return URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            NanoHTTPD.LOG.log(Level.WARNING, "Encoding not supported, ignored", e);
        }
        return null;
    }

    public final int getListeningPort() {
        return mServerSocket == null ? mServerSocketFactory.getBindPort() : mServerSocket.getLocalPort();
    }

    public FactoryThrowing<ServerSocket, IOException> getServerSocketFactory() {
        return mServerSocketFactory;
    }

    public Factory<TempFileManager> getTempFileManagerFactory() {
        return mTempFileManagerFactory;
    }

    public final boolean isListening() {
        return mServerSocket != null && !mServerSocket.isClosed() && mServerThread.isAlive();
    }

    public final boolean isServerThreadInterrupted() {
        return mServerThread == null || mServerThread.isInterrupted();
    }

    public final boolean isServerThreadAlive() {
        return mServerThread != null && mServerThread.isAlive();
    }

    /**
     * This is the "master" method that delegates requests to handlers and makes
     * sure there is a response to every request. You are not supposed to call
     * or override this method in any circumstances. But no one will stop you if
     * you do. I'm a Javadoc, not Code Police.
     *
     * @param session the incoming session
     * @return a response to the incoming session
     */
    public Response handle(HTTPSession session) {
        for (Handler<HTTPSession, Response> interceptor : interceptors) {
            Response response = interceptor.handle(session);
            if (response != null)
                return response;
        }
        return httpHandler.handle(session);
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this returns a 404 "Not Found" plain text error response.)
     *
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     */
    @Deprecated
    protected Response serve(HTTPSession session) {
        return Response.newFixedLengthResponse(DefaultStatusCode.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    /**
     * Pluggable strategy for asynchronously executing requests.
     *
     * @param asyncRunner new strategy for handling threads.
     */
    public void setAsyncRunner(AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     *
     * @param factory new strategy for handling temp files.
     */
    public void setTempFileManagerFactory(Factory<TempFileManager> factory) {
        mTempFileManagerFactory = factory;
    }

    public void start() throws IOException {
        start(true);
    }

    /**
     * Start the server.
     *
     * @param daemon start the thread as daemon or not.
     * @throws IOException when anything related to socket is gone wrong.
     */
    public void start(boolean daemon) throws IOException {
        mServerSocket = getServerSocketFactory().create();
        mServerSocket.setReuseAddress(true);

        ServerRunnable serverRunnable = createServerRunnable();
        mServerThread = new Thread(serverRunnable);
        mServerThread.setDaemon(daemon);
        mServerThread.setName("uduhttpd daemon");
        mServerThread.start();
    }

    /**
     * Start the server and ensure that it has started listening. Beware that this returns as soon as the server starts
     * listening and does not care if waitForStart milliseconds has passed or not.
     *
     * @param daemon       start the thread as daemon or not.
     * @param waitForStart milliseconds to wait before giving up.
     * @throws IOException      when anything related to socket goes wrong.
     * @throws TimeoutException when the given waitForStart time is exceeded, but the server did not start listening.
     */
    public void start(boolean daemon, int waitForStart) throws IOException, TimeoutException {
        if (waitForStart < 1)
            throw new IllegalArgumentException("The time to wait cannot be smaller than 1.");

        start(daemon);

        long failAt = (long) (System.nanoTime() + (waitForStart * 1e6));
        while (!isListening())
            if (System.nanoTime() > failAt)
                throw new TimeoutException("Could not start the server in the given time.");
    }

    /**
     * Start the server and ensure that it has started listening. Beware that this returns as soon as the server starts
     * listening and does not care if waitForStart milliseconds has passed or not.
     *
     * @param waitForStart milliseconds to wait before giving up.
     * @throws IOException      when anything related to socket goes wrong.
     * @throws TimeoutException when the given waitForStart time is exceeded, but the server did not start listening.
     */
    public void start(int waitForStart) throws IOException, TimeoutException {
        start(true, waitForStart);
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            safeClose(this.mServerSocket);
            this.asyncRunner.closeAll();
            if (this.mServerThread != null) {
                this.mServerThread.join();
            }
        } catch (Exception e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Could not stop all connections", e);
        }
    }
}
