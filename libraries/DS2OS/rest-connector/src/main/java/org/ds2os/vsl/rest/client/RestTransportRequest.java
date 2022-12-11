package org.ds2os.vsl.rest.client;

import java.io.InputStream;
import java.io.OutputStream;

import org.ds2os.vsl.exception.VslException;

/**
 * Interface of a REST transport request.
 *
 * @author felix
 */
public interface RestTransportRequest {

    /**
     * Set acceptable content types for the response.
     *
     * @param contentTypes
     *            the accepted content types.
     */
    void accept(String... contentTypes);

    /**
     * Set a request header.
     *
     * @param header
     *            the header name.
     * @param value
     *            the header value.
     */
    void setHeader(String header, String value);

    /**
     * Get the stream to which the request body can be written.
     *
     * @param contentType
     *            the content type of the request which will be sent.
     * @return the {@link OutputStream} to write the request body.
     */
    OutputStream getRequestStream(String contentType);

    /**
     * Get the stream from which the response body can be read.
     *
     * @return the {@link InputStream} to read the response body.
     */
    InputStream getResponseStream();

    /**
     * Get the content type of the response.
     *
     * @return the content type string or null if it was not included.
     */
    String getResponseType();

    /**
     * Send the request, making it impossible to add further headers. Must be executed before
     * streams are used.
     */
    void send();

    /**
     * Synchronous execution of the request.
     *
     * @return the HTTP status code.
     * @throws VslException
     *             If the request fails, for example with a timeout or because the KA is unreachable
     *             or incompatible.
     */
    int syncRequest() throws VslException;
}
