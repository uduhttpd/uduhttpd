/*
 * Copyright (C) 2012 - 2016 nanohttpd (NanoHTTPD.java)
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

package org.nanohttpd.protocols.http;

import org.nanohttpd.protocols.http.client.ClientRequestExecutor;
import org.nanohttpd.protocols.http.client.ClientRequestExecutorFactory;
import org.nanohttpd.protocols.http.client.DefaultClientRequestExecutorFactory;
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
import org.nanohttpd.util.concurrent.DefaultExecutorServiceFactory;
import org.nanohttpd.util.concurrent.ExecutorServiceFactory;
import org.nanohttpd.util.concurrent.RegistrarRunnable;

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
     * @see #getTempFileManagerFactory()
     */
    public void setTempFileManagerFactory(Factory<TempFileManager> factory) {
        tempFileManagerFactory = factory;
    }

    /**
     * Start listening for connections and block the calling thread until the server is ready. The value used for
     * {@link Thread#setDaemon(boolean)} defaults in this case.
     *
     * @return the server executor
     * @throws ServerStartException when the server doesn't start with the factory configuration or the thread exits
     *                              for some reason.
     */
    public ServerExecutor start() throws ServerStartException {
        return start(true);
    }

    /**
     * Start listening for connections while block the calling thread until the server starts or an error is thrown.
     *
     * @param daemon set {@link Thread#setDaemon(boolean)}.
     * @return the server executor.
     * @throws ServerStartException when the server doesn't start with the factory configuration or the thread exits
     *                              for some reason.
     * @see #start(boolean, int)
     * @see #startAsynchronously(boolean)
     */
    public ServerExecutor start(boolean daemon) throws ServerStartException {
        return startBlockingPrivate(daemon, 0);
    }

    /**
     * Start listening for connections and block the calling thread until the server is ready.
     *
     * @param daemon       set {@link Thread#setDaemon(boolean)}.
     * @param waitForStart maximum time to block the calling thread.
     * @return the server executor.
     * @throws TimeoutException      if the waitForStart exceeded and the server failed to start.
     * @throws ServerStartException  when the server doesn't start with the factory configuration or the thread exits
     *                               for some reason.
     * @throws IllegalStateException if the server is not running. Use {@link #isListening()} to make sure it is running.
     * @see #start(boolean)
     * @see #startAsynchronously(boolean)
     */
    public ServerExecutor start(boolean daemon, int waitForStart) throws TimeoutException, ServerStartException {
        if (waitForStart == 0)
            throw new IllegalArgumentException("Indefinite blocking is not allowed with this method. Use " +
                    "start(boolean) for that");

        ServerExecutor executor = startBlockingPrivate(daemon, waitForStart);
        if (!isListening())
            throw new TimeoutException("Could not start the server in time.");

        return executor;
    }

    /**
     * Start listening for connections without blocking the calling thread. However, beware that unlike blocking ones,
     * this will not throw an error if the server fails to start. For that, you need to make use of the returned
     * {@link ServerExecutor} instance and {@link #isListening()}, {@link #isInterrupted()} methods.
     *
     * @return the server executor.
     * @throws IllegalStateException if the server is already running.
     * @see #start(boolean)
     * @see #start(boolean, int)
     */
    public ServerExecutor startAsynchronously(boolean daemon) {
        if (isListening())
            throw new IllegalStateException("The server is already running.");

        ServerExecutor executor = createServerExecutor();
        serverThread = new Thread(executor);
        serverThread.setDaemon(daemon);
        serverThread.setName("uduhttpd daemon");
        serverThread.start();

        return executor;
    }

    private ServerExecutor startBlockingPrivate(boolean daemon, int waitForStart) throws ServerStartException {
        if (waitForStart < 0)
            throw new IllegalArgumentException("The time to wait cannot be a negative.");

        ServerExecutor executor = startAsynchronously(daemon);

        try {
            executor.waitUntilStarts(waitForStart);
        } catch (InterruptedException e) {
            throw new ServerStartException("Could not wait for the server to start because the current thread " +
                    "making the call has been interrupted.", e);
        }

        if (executor.getStartError() != null)
            throw executor.getStartError();

        return executor;
    }

    /**
     * Stop listening for connections and block the calling thread indefinitely until the server thread exits.
     *
     * @see #stop(int)
     * @see #stopAsynchronously()
     */
    public void stop() {
        stopBlockingPrivate(0);
    }

    /**
     * Stop listening for connections and wait for the server thread to exit.
     *
     * @param waitMs maximum milliseconds this call is allowed to take.
     * @throws TimeoutException when the server thread longer to exit.
     * @see #stop()
     * @see #stopAsynchronously()
     */
    public void stop(int waitMs) throws TimeoutException {
        if (waitMs == 0)
            throw new IllegalArgumentException("Indefinite blocking is only allowed with stop().");

        stopBlockingPrivate(waitMs);
        if (serverThread.isAlive())
            throw new TimeoutException("Could not stop the server in time.");
    }

    /**
     * Stop listening for connection and do this without blocking the calling thread.
     *
     * @see #stop()
     * @see #stop(int)
     */
    public void stopAsynchronously() {
        safeClose(serverSocket);
        serverSocket = null;

        if (isListening() && activeClientConnectionList.size() > 0) {
            List<ClientRequestExecutor> copyList = new ArrayList<>(activeClientConnectionList);
            for (ClientRequestExecutor requestExecutor : copyList)
                safeClose(requestExecutor);
        }
    }

    private void stopBlockingPrivate(int waitMs) {
        if (waitMs < 0)
            throw new IllegalArgumentException("The time to wait cannot be negative.");

        stopAsynchronously();

        try {
            if (serverThread != null && serverThread.isAlive())
                serverThread.join(waitMs);
        } catch (InterruptedException ignored) {

        }
    }

    /**
     * Reusable server executor. The server socket instance is assigned during the execution and the factory classes
     * should not create ServerSocket instances outside of this class unless you are going to unbind the address before
     * this executor enters the execution phase.
     *
     * @see DefaultServerExecutor
     */
    public abstract static class ServerExecutor implements Runnable {
        public final Object startupLock = new Object();
        private ServerStartException startException = null;
        private boolean started = false;
        private boolean stopped = false;

        @Override
        public final void run() {
            started = false;
            stopped = false;
            NanoHTTPD server = getServer();
            ServerSocket serverSocket = null;

            try {
                serverSocket =  server.getServerSocketFactory().create();
                server.serverSocket = serverSocket;
                started = true;
            } catch (Exception e) {
                startException = new ServerStartException("The server thread crashed during initialization of " +
                        "the socket. See the cause error for details.", e);
            } finally {
                synchronized (startupLock) {
                    startupLock.notifyAll();
                }
            }

            if (started)
                try {
                    serve(serverSocket);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            safeClose(server.getServerSocket());
            stopped = true;
            server.serverThread = null;
        }

        protected abstract NanoHTTPD getServer();

        public ServerStartException getStartError() {
            return startException;
        }

        public final boolean isStarted() {
            return started;
        }

        public final boolean isStopped() {
            return stopped;
        }

        protected abstract void serve(ServerSocket serverSocket);

        /**
         * Wait until the server starts. The thread that will execute this should already be started. This will return
         * immediately if the execution has already started.
         *
         * @param ms time to wait as described in {@link Object#wait(long)}
         * @throws InterruptedException  when the calling thread is interrupted.
         * @throws IllegalStateException when the executor has already run and doesn't wait for another execution.
         */
        public void waitUntilStarts(long ms) throws InterruptedException {
            if (isStopped())
                throw new IllegalStateException("This executor already stopped");

            if (!isStarted())
                synchronized (startupLock) {
                    startupLock.wait(ms);
                }
        }
    }
}
