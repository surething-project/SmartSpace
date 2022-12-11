package org.ds2os.vsl.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Map VSL data objects to serialized content (e.g. for HTTP transport) using a specific MIME
 * content type and encoding.
 *
 * @author felix
 */
public interface VslMapper {

    /**
     * Get the MIME content type of this mapper.
     *
     * @return the content type.
     */
    String getContentType();

    /**
     * Get the content encoding of this mapper. The special encoding "binary" describes a binary
     * encoding.
     *
     * @return the charset used for encoding string content or "binary".
     */
    String getContentEncoding();

    /**
     * Writes a value to an {@link OutputStream}. Usually the stream is closed by this operation.
     *
     * @param <T>
     *            the type of the value.
     * @param output
     *            the output stream where the output is written.
     * @param value
     *            the value to serialize.
     * @throws IOException
     *             If an I/O error occurs.
     */
    <T> void writeValue(OutputStream output, T value) throws IOException;

    /**
     * Read a value from an {@link InputStream}. Usually the stream is closed by this operation.
     *
     * @param <T>
     *            the type of the value.
     * @param input
     *            the input stream to read input from.
     * @param valueType
     *            the class to instantiate for the value.
     * @return the value.
     * @throws IOException
     *             If an I/O error occurs.
     */
    <T> T readValue(InputStream input, Class<T> valueType) throws IOException;
}
