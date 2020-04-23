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

import org.nanohttpd.concurrent.util.RegistrarRunnable;
import org.nanohttpd.protocols.http.client.*;
import org.nanohttpd.protocols.http.response.DefaultStatusCode;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.server.DefaultServerExecutor;
import org.nanohttpd.protocols.http.server.ServerStartException;
import org.nanohttpd.protocols.http.sockets.DefaultServerSocketFactory;
import org.nanohttpd.protocols.http.sockets.ServerSocketFactory;
import org.nanohttpd.protocols.http.tempfiles.DefaultTempFileManagerFactory;
import org.nanohttpd.protocols.http.tempfiles.TempFileManager;
import org.nanohttpd.util.Factory;
import org.nanohttpd.util.Handler;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
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

    public static void safeClose(Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (IOException e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Could not close", e);
        }
    }

    protected List<Handler<HTTPSession, Response>> interceptors = new ArrayList<>(4);

    private final List<ClientRequestExecutor> activeClientConnectionList = Collections.synchronizedList(
            new ArrayList<ClientRequestExecutor>());

    private ExecutorServiceFactory clientRequestExecutorServiceFactory;

    private ExecutorService clientRequestExecutorService;

    private ClientRequestExecutorFactory clientRequestExecutorFactory;

    private Handler<HTTPSession, Response> httpHandler;

    private ServerSocket serverSocket;

    private Thread serverThread;

    private ServerSocketFactory serverSocketFactory;

    private Factory<TempFileManager> tempFileManagerFactory;

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

        serverSocketFactory = socketFactory;

        // creates a default handler that redirects to deprecated serve();
        httpHandler = new Handler<HTTPSession, Response>() {

            @Override
            public Response handle(HTTPSession input) {
                return NanoHTTPD.this.serve(input);
            }
        };
    }

    public void addHTTPInterceptor(Handler<HTTPSession, Response> interceptor) {
        interceptors.add(interceptor);
    }

    protected ServerExecutor createServerExecutor() {
        return new DefaultServerExecutor(this);
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
        Map<String, List<String>> parms = new HashMap<>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = decodePercent(sep >= 0 ? e.substring(0, sep) : e).trim();
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

    // TODO: 23.04.2020 Remove this and use the charset that is used by the library

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

    public ExecutorService getClientRequestExecutorService() {
        if (clientRequestExecutorService == null || clientRequestExecutorService.isShutdown())
            clientRequestExecutorService = getClientRequestExecutorServiceFactory().create();

        return clientRequestExecutorService;
    }

    public ExecutorServiceFactory getClientRequestExecutorServiceFactory() {
        if (clientRequestExecutorServiceFactory == null)
            clientRequestExecutorServiceFactory = new DefaultExecutorServiceFactory();

        return clientRequestExecutorServiceFactory;
    }

    public ClientRequestExecutorFactory getClientRequestExecutorFactory() {
        if (clientRequestExecutorFactory == null)
            clientRequestExecutorFactory = new DefaultClientRequestExecutorFactory();

        return clientRequestExecutorFactory;
    }

    public final int getListeningPort() {
        return serverSocket == null ? getServerSocketFactory().getBindPort() : serverSocket.getLocalPort();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public ServerSocketFactory getServerSocketFactory() {
        return serverSocketFactory;
    }

    public Factory<TempFileManager> getTempFileManagerFactory() {
        if (tempFileManagerFactory == null)
            tempFileManagerFactory = new DefaultTempFileManagerFactory();

        return tempFileManagerFactory;
    }

    public final boolean isListening() {
        return serverSocket != null && serverThread != null && serverSocket.isBound() && serverThread.isAlive();
    }

    public final boolean isInterrupted() {
        return serverThread == null || serverThread.isInterrupted() || serverSocket == null || serverSocket.isClosed();
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

    public void handleConnectionRequest(Socket socket) {
        getClientRequestExecutorService().submit(new RegistrarRunnable<>(
                getClientRequestExecutorFactory().create(this, socket), activeClientConnectionList));
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

    public void setClientRequestExecutorFactory(ClientRequestExecutorFactory factory) {
        clientRequestExecutorFactory = factory;
    }

    public void setHTTPHandler(Handler<HTTPSession, Response> handler) {
        this.httpHandler = handler;
    }

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     *
     * @param factory new strategy for handling temp files.
     */
    public void setTempFileManagerFactory(Factory<TempFileManager> factory) {
        tempFileManagerFactory = factory;
    }

    public void start() throws ServerStartException {
        try {
            start(false, 0);
        } catch (TimeoutException ignored) {
            // Cannot reach here because, unless the server is started, it will not return. (blocks)
        }
    }

    /**
     * Start listening for connections.
     *
     * @param daemon       set {@link Thread#setDaemon(boolean)}.
     * @param waitForStart the time in milliseconds that we will wait until the server starts listening. '0' will mean
     *                     it will wait forever unless it starts. This will return as soon as the server starts
     *                     listening and doesn't mean that the server will take as long to start.
     * @throws TimeoutException when the value given for waitForStart is bigger than 0 zero and the server did not start
     *                          under the given time.
     */
    public void start(boolean daemon, int waitForStart) throws TimeoutException, ServerStartException {
        if (isListening())
            throw new IllegalStateException("The server is already running.");

        if (waitForStart < 0)
            throw new IllegalArgumentException("The time to wait cannot be smaller than 1.");

        ServerExecutor executor = createServerExecutor();
        serverThread = new Thread(executor);
        serverThread.setDaemon(daemon);
        serverThread.setName("uduhttpd daemon");
        serverThread.start();

        long startTime = System.nanoTime();
        while (!isListening()) {
            if (executor.getStartError() != null)
                throw executor.getStartError();

            if (waitForStart > 0 && (System.nanoTime() - startTime) > waitForStart * 1e6)
                throw new TimeoutException("Could not start the server in time.");
        }
    }

    /**
     * Stop listening for connections and wait for the thread to exit.
     */
    public void stop() {
        try {
            stop(0);
        } catch (TimeoutException e) {
            // Cannot reach here because, unless the server is stopped, it will not return. (blocks)
        }
    }

    /**
     * Stop listening for connections and exit the thread.
     *
     * @param wait time before giving up. '0' to wait indefinitely
     */
    public void stop(int wait) throws TimeoutException {
        safeClose(serverSocket);
        serverSocket = null;

        if (isListening()) {
            if (activeClientConnectionList.size() > 0) {
                List<ClientRequestExecutor> copyList = new ArrayList<>(activeClientConnectionList);
                for (ClientRequestExecutor requestExecutor : copyList)
                    safeClose(requestExecutor);
            }

            if (serverThread != null && serverThread.isAlive()) {
                try {
                    serverThread.join(wait);
                    if (serverThread.isAlive())
                        throw new TimeoutException("Could not stop the server in time.");

                    serverThread = null;
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    public abstract static class ServerExecutor implements Runnable {
        private ServerStartException startException = null;

        @Override
        public final void run() {
            NanoHTTPD server = getServer();

            try {
                server.serverSocket = server.getServerSocketFactory().create();

                try {
                    serve(server.getServerSocket());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                startException = new ServerStartException("The server thread did not start during initialization of " +
                        "the socket. See the cause error for details.", e);
            } finally {
                safeClose(server.getServerSocket());
            }
        }

        protected abstract NanoHTTPD getServer();

        public ServerStartException getStartError() {
            return startException;
        }

        protected abstract void serve(ServerSocket serverSocket);
    }
}
