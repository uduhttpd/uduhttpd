package org.nanohttpd.fileupload;

import org.apache.commons.fileupload.UploadContext;
import org.nanohttpd.protocols.http.HTTPSession;

import java.io.IOException;
import java.io.InputStream;

public class DefaultUploadContext implements UploadContext {
    private final HTTPSession mSession;

    public DefaultUploadContext(HTTPSession session) {
        mSession = session;
    }

    @Override
    public long contentLength() {
        try {
            return Long.parseLong(mSession.getHeaders().get("content-length"));
        } catch (NumberFormatException var4) {
            return -1L;
        }
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return mSession.getHeaders().get("content-type");
    }

    @Override
    public int getContentLength() {
        return (int) contentLength();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return mSession.getInputStream();
    }
}