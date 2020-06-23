/*
 * Copyright (C) 2012 - 2016 nanohttpd (SimpleWebServer.java)
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

package org.nanohttpd.webserver;

import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.DefaultStatusCode;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.StatusCode;
import org.nanohttpd.protocols.http.server.ServerStartException;

import java.io.*;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.*;

public class SimpleWebServer extends NanoHTTPD {

    /**
     * Default Index file names.
     */
    public static final List<String> INDEX_FILE_NAMES = new ArrayList<>();

    /**
     * The distribution licence
     */
    private static final String LICENCE;

    static {
        INDEX_FILE_NAMES.add("index.html");
        INDEX_FILE_NAMES.add("index.htm");

        mimeTypes();

        String license;

        try {
            InputStream stream = SimpleWebServer.class.getResourceAsStream("/LICENSE.txt");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = stream.read(buffer)) >= 0)
                bytes.write(buffer, 0, count);

            license = bytes.toString("UTF-8");
        } catch (Exception e) {
            license = "unknown";
        }

        LICENCE = license;
    }

    private static final Map<String, WebServerPlugin> mimeTypeHandlers = new HashMap<>();

    /**
     * Starts as a standalone file server and waits for Enter.
     */
    public static void main(String[] args) throws IOException, ServerStartException {
        start(args);
    }

    public static SimpleWebServer createCommandLineInstance(String[] args) throws UnknownHostException {
        // Defaults
        int port = 8080;

        String host = null; // bind to all interfaces by default
        List<File> rootDirs = new ArrayList<>();
        Map<String, String> options = new HashMap<>();
        boolean quiet = false;
        String cors = null;

        // Parse command-line, with short and long versions of the options.
        for (int i = 0; i < args.length; ++i) {
            if ("-h".equalsIgnoreCase(args[i]) || "--host".equalsIgnoreCase(args[i])) {
                host = args[i + 1];
            } else if ("-p".equalsIgnoreCase(args[i]) || "--port".equalsIgnoreCase(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            } else if ("-q".equalsIgnoreCase(args[i]) || "--quiet".equalsIgnoreCase(args[i])) {
                quiet = true;
            } else if ("-d".equalsIgnoreCase(args[i]) || "--dir".equalsIgnoreCase(args[i])) {
                rootDirs.add(new File(args[i + 1]).getAbsoluteFile());
            } else if (args[i].startsWith("--cors")) {
                cors = "*";
                int equalIdx = args[i].indexOf('=');
                if (equalIdx > 0) {
                    cors = args[i].substring(equalIdx + 1);
                }
            } else if ("--licence".equalsIgnoreCase(args[i])) {
                System.out.println(SimpleWebServer.LICENCE + "\n");
            } else if (args[i].startsWith("-X:")) {
                int dot = args[i].indexOf('=');
                if (dot > 0) {
                    String name = args[i].substring(0, dot);
                    String value = args[i].substring(dot + 1);
                    options.put(name, value);
                }
            }
        }

        if (rootDirs.isEmpty()) {
            rootDirs.add(new File(".").getAbsoluteFile());
        }
        options.put("host", host);
        options.put("port", "" + port);
        options.put("quiet", String.valueOf(quiet));
        StringBuilder sb = new StringBuilder();
        for (File dir : rootDirs) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            try {
                sb.append(dir.getCanonicalPath());
            } catch (IOException ignored) {
            }
        }
        options.put("home", sb.toString());
        ServiceLoader<WebServerPluginInfo> serviceLoader = ServiceLoader.load(WebServerPluginInfo.class);
        for (WebServerPluginInfo info : serviceLoader) {
            String[] mimeTypes = info.getMimeTypes();
            for (String mime : mimeTypes) {
                String[] indexFiles = info.getIndexFilesForMimeType(mime);
                if (!quiet) {
                    System.out.print("# Found plugin for Mime type: \"" + mime + "\"");
                    if (indexFiles != null) {
                        System.out.print(" (serving index files: ");
                        for (String indexFile : indexFiles) {
                            System.out.print(indexFile + " ");
                        }
                    }
                    System.out.println(").");
                }
                registerPluginForMimeType(indexFiles, mime, info.getWebServerPlugin(mime), options);
            }
        }

        return new SimpleWebServer(host, port, rootDirs, quiet, cors);
    }

    public static SimpleWebServer start(String[] args) throws UnknownHostException, ServerStartException {
        SimpleWebServer server = createCommandLineInstance(args);
        server.start();
        return server;
    }

    protected static void registerPluginForMimeType(String[] indexFiles, String mimeType, WebServerPlugin plugin,
                                                    Map<String, String> commandLineOptions) {
        if (mimeType == null || plugin == null) {
            return;
        }

        if (indexFiles != null) {
            for (String filename : indexFiles) {
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) {
                    String extension = filename.substring(dot + 1).toLowerCase();
                    mimeTypes().put(extension, mimeType);
                }
            }
            SimpleWebServer.INDEX_FILE_NAMES.addAll(Arrays.asList(indexFiles));
        }
        SimpleWebServer.mimeTypeHandlers.put(mimeType, plugin);
        plugin.initialize(commandLineOptions);
    }

    private final boolean quiet;

    private final String cors;

    protected List<File> rootDirs;

    public SimpleWebServer(String host, int port, File wwwroot, boolean quiet, String cors)
            throws UnknownHostException {
        this(host, port, Collections.singletonList(wwwroot), quiet, cors);
    }

    public SimpleWebServer(String host, int port, File wwwroot, boolean quiet) throws UnknownHostException {
        this(host, port, Collections.singletonList(wwwroot), quiet, null);
    }

    public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet) throws UnknownHostException {
        this(host, port, wwwroots, quiet, null);
    }

    public SimpleWebServer(String host, int port, List<File> wwwroots, boolean quiet, String cors)
            throws UnknownHostException {
        super(host, port);
        this.quiet = quiet;
        this.cors = cors;
        this.rootDirs = new ArrayList<>(wwwroots);

        init();
    }

    private boolean canServeUri(String uri, File homeDir) {
        File f = new File(homeDir, uri);
        if (f.exists())
            return true;

        WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(getMimeTypeForFile(uri));
        return plugin != null && plugin.canServeUri(uri, homeDir);
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri) {
        StringBuilder newUri = new StringBuilder();
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if ("/".equals(tok)) {
                newUri.append("/");
            } else if (" ".equals(tok)) {
                newUri.append("%20");
            } else {
                try {
                    newUri.append(URLEncoder.encode(tok, "UTF-8"));
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri.toString();
    }

    private String findIndexFileInDirectory(File directory) {
        for (String fileName : SimpleWebServer.INDEX_FILE_NAMES) {
            File indexFile = new File(directory, fileName);
            if (indexFile.isFile()) {
                return fileName;
            }
        }
        return null;
    }

    protected Response getForbiddenResponse(String s) {
        return Response.newFixedLengthResponse(DefaultStatusCode.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
                "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return Response.newFixedLengthResponse(DefaultStatusCode.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse() {
        return Response.newFixedLengthResponse(DefaultStatusCode.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
                "Error 404, file not found.");
    }

    /**
     * Used to initialize and customize the server.
     */
    public void init() {
    }

    protected String listDirectory(String uri, File f) {
        String heading = "Directory " + uri;
        StringBuilder msg = new StringBuilder("<html><head><title>" + heading + "</title><style><!--\nspan.dirname" +
                " { font-weight: bold; }\nspan.filesize { font-size: 75%; }\n// -->\n</style></head><body><h1>"
                + heading + "</h1>");

        String goUpHtml = null;
        if (uri.length() > 1) {
            String u = uri.substring(0, uri.length() - 1);
            int slash = u.lastIndexOf('/');
            if (slash >= 0 && slash < u.length()) {
                goUpHtml = uri.substring(0, slash + 1);
            }
        }

        StringBuilder directoriesHtml = null;
        StringBuilder filesHtml = null;
        File[] files = f.listFiles();

        if (files != null && files.length > 0) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File that) {
                    // any of them directories? if so, is only one of them directory? if so, the directory one goes up
                    if (file.isDirectory() || that.isDirectory() && (file.isDirectory() != that.isDirectory()))
                        return file.isDirectory() ? 1 : -1;

                    return file.compareTo(that);
                }
            });

            for (File file : files) {
                if (file.isDirectory()) {
                    if (directoriesHtml == null)
                        directoriesHtml = new StringBuilder();

                    String dir = file.getName() + "/";
                    directoriesHtml.append("<li><a rel=\"directory\" href=\"").append(encodeUri(uri + dir))
                            .append("\"><span class=\"dirname\">").append(dir).append("</span></a></li>");
                } else {
                    if (filesHtml == null)
                        filesHtml = new StringBuilder();

                    filesHtml.append("<li><a href=\"").append(encodeUri(uri + file))
                            .append("\"><span class=\"filename\">").append(file).append("</span></a>");
                    long len = file.length();
                    filesHtml.append("&nbsp;<span class=\"filesize\">(");
                    // TODO: 24.04.2020 simplify this
                    if (len < 1024) {
                        filesHtml.append(len).append(" bytes");
                    } else if (len < 1024 * 1024) {
                        filesHtml.append(len / 1024).append(".").append(len % 1024 / 10 % 100).append(" KB");
                    } else {
                        filesHtml.append(len / (1024 * 1024)).append(".").append(len % (1024 * 1024) / 10000 % 100).append(" MB");
                    }
                    filesHtml.append(")</span></li>");
                }
            }
        }

        if (goUpHtml != null || directoriesHtml != null || filesHtml != null) {
            msg.append("<ul>");

            if (goUpHtml != null || directoriesHtml != null) {
                msg.append("<section class=\"directories\">");

                if (goUpHtml != null)
                    msg.append("<li><a rel=\"directory\" href=\"").append(goUpHtml)
                            .append("\"><span class=\"dirname\">..</span></a></li>");

                if (directoriesHtml != null)
                    msg.append(directoriesHtml);

                msg.append("</section>");
            }

            if (filesHtml != null)
                msg.append("<section class=\"files\">").append(filesHtml).append("</section>");

            msg.append("</ul>");
        }

        msg.append("</body></html>");
        return msg.toString();
    }

    public static Response newFixedLengthResponse(StatusCode status, String mimeType, String message) {
        Response response = Response.newFixedLengthResponse(status, mimeType, message);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    private Response respond(Map<String, String> headers, HTTPSession session, String uri) {
        // First let's handle CORS OPTION query
        Response r;
        if (cors != null && Method.OPTIONS.equals(session.getMethod())) {
            r = Response.newFixedLengthResponse(DefaultStatusCode.OK, MIME_PLAINTEXT, null, 0);
        } else {
            r = defaultRespond(headers, session, uri);
        }

        if (cors != null) {
            r = addCORSHeaders(headers, r, cors);
        }
        return r;
    }

    private Response defaultRespond(Map<String, String> headers, HTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        }

        boolean canServeUri = false;
        File homeDir = null;
        for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
            homeDir = this.rootDirs.get(i);
            canServeUri = canServeUri(uri, homeDir);
        }

        if (!canServeUri) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.
        File f = new File(homeDir, uri);
        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = newFixedLengthResponse(DefaultStatusCode.REDIRECT, NanoHTTPD.MIME_HTML,
                    "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if
            // none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile == null) {
                if (f.canRead()) {
                    // No index file, list the directory if it is readable
                    return newFixedLengthResponse(DefaultStatusCode.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                return respond(headers, session, uri + indexFile);
            }
        }
        String mimeTypeForFile = getMimeTypeForFile(uri);
        WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(mimeTypeForFile);
        Response response;
        if (plugin != null && plugin.canServeUri(uri, homeDir)) {
            response = plugin.serveFile(uri, headers, session, f, mimeTypeForFile);
            if (response instanceof InternalRewrite) {
                InternalRewrite rewrite = (InternalRewrite) response;
                return respond(rewrite.getHeaders(), session, rewrite.getUri());
            }
        } else {
            response = serveFile(uri, headers, f, mimeTypeForFile);
        }
        return response != null ? response : getNotFoundResponse();
    }

    @Override
    public Response serve(HTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        if (!this.quiet) {
            System.out.println(session.getMethod() + " '" + uri + "' ");

            Iterator<String> e = header.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
            }
            e = parms.keySet().iterator();
            while (e.hasNext()) {
                String value = e.next();
                System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
            }
        }

        for (File homeDir : this.rootDirs) {
            // Make sure we won't die of an exception later
            if (!homeDir.isDirectory()) {
                return getInternalErrorResponse("given path is not a directory (" + homeDir + ").");
            }
        }
        return respond(Collections.unmodifiableMap(header), session, uri);
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        try {
            // Calculate etag
            Response res;
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(DefaultStatusCode.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = Response.newFixedLengthResponse(DefaultStatusCode.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(DefaultStatusCode.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT,
                            "");
                    res.addHeader("Content-Range", "bytes */" + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                    res = newFixedLengthResponse(DefaultStatusCode.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified

                    res = newFixedLengthResponse(DefaultStatusCode.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    // supply the file
                    res = newFixedFileResponse(file, mime);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }

            return res;
        } catch (IOException e) {
            return getForbiddenResponse("Reading file failed.");
        }
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = Response.newFixedLengthResponse(DefaultStatusCode.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    protected Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
        resp.addHeader("Access-Control-Allow-Origin", cors);
        resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        resp.addHeader("Access-Control-Max-Age", "" + MAX_AGE);

        return resp;
    }

    private String calculateAllowHeaders(Map<String, String> queryHeaders) {
        // here we should use the given asked headers
        // but NanoHttpd uses a Map whereas it is possible for requester to send
        // several time the same header
        // let's just use default values for this version
        return System.getProperty(ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, DEFAULT_ALLOWED_HEADERS);
    }

    private final static String ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";

    private final static int MAX_AGE = 42 * 60 * 60;

    // explicitly relax visibility to package for tests purposes
    public final static String DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";

    public final static String ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";
}
