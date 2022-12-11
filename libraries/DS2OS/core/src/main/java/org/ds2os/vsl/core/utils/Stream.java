package org.ds2os.vsl.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A utility class providing methods for streams.
 */
public final class Stream {

    /**
     * Size of the default buffer in bytes used in
     * {@link #copy(InputStream, OutputStream)}.
     */
    private static final int copyDefaultBufferSize = 8 * 1024; // 8KB

    /**
     * Calls {@link #copy(InputStream, OutputStream, int)} with
     * a default buffer size of {@link #copyDefaultBufferSize}.
     *
     * @param from
     *      the {@link InputStream} source.
     * @param to
     *      the {@link OutputStream} sink.
     * @return
     *      the total number of bytes copied.
     * @throws IOException
     *      if one of the streams fails.
     */
    public static long copy(InputStream from, OutputStream to) throws IOException {
        return copy(from, to, copyDefaultBufferSize);
    }

    /**
     * Calls {@link #copy(InputStream, OutputStream, byte[])} with
     * a newly allocated buffer with the given size.
     *
     * @param from
     *      the {@link InputStream} source.
     * @param to
     *      the {@link OutputStream} sink.
     * @param bufferSize
     *      the size of the buffer used for copying.
     * @return
     *      the total number of bytes copied.
     * @throws IOException
     *      if buffer has an invalid length or one of the streams fails.
     */
    public static long copy(InputStream from, OutputStream to, int bufferSize) throws IOException {
        if (bufferSize <= 0) {
            throw new IOException("bufferSize must be positive");
        }
        return copy(from, to, new byte[bufferSize]);
    }

    /**
     * Copies the contents of stream 'from' to the sink 'to' using the provided buffer.
     * The writes to the buffer are all immediately flushed.
     *
     * @param from
     *      the {@link InputStream} source.
     * @param to
     *      the {@link OutputStream} sink.
     * @return
     *      the total number of bytes copied.
     * @throws IOException
     *      if buffer has an invalid length or one of the streams fails.
     */
    public static long copy(InputStream from, OutputStream to, byte[] buf) throws IOException {
        if (buf.length <= 0) {
            throw new IOException("buffer must have positive length");
        }

        // Stores the total number of bytes copied.
        long total = 0L;
        // Stores the number of read bytes.
        int read;
        while(true) {
            read = from.read(buf);
            if (read == -1) {
                return total;
            }

            to.write(buf, 0, read);
            to.flush();
            total += read;
        }
    }
}
