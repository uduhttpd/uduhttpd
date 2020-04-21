package org.nanohttpd.protocols.http.response;

import java.io.IOException;
import java.io.OutputStream;

public class ChunkedStreams {
    public static final String DELIMITER = "\r\n";
    public static final byte[] DATA_DELIMITER = DELIMITER.getBytes();
    public static final byte[] DATA_DELIMITER_FINISH = (0 + DELIMITER + DELIMITER).getBytes();

    public static void writeInChunks(OutputStream outputStream, byte[] data, int offset, int length)
            throws IOException {
        if (offset >= 0 && length >= 0 && offset + length <= data.length && offset + length >= 0) {
            outputStream.write(Integer.toHexString(length).getBytes());
            outputStream.write(DATA_DELIMITER);
            outputStream.write(data, offset, length);
            outputStream.write(DATA_DELIMITER);
        } else
            throw new IndexOutOfBoundsException();
    }
}
