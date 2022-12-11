package org.ds2os.vsl.rest.client;

import java.net.URI;
import java.util.UUID;

import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.exception.VslException;

/**
 * REST transport client interface which acts as a factory for {@link RestTransportRequest}
 * instances.
 *
 * @author felix
 */
public interface RestTransportClient {

    /**
     * Create a GET request.
     *
     * @param uri
     *            the URI.
     * @return the {@link RestTransportRequest} instance.
     */
    RestTransportRequest doGet(URI uri);

    /**
     * Create a PUT request.
     *
     * @param uri
     *            the URI.
     * @return the {@link RestTransportRequest} instance.
     */
    RestTransportRequest doPut(URI uri);

    /**
     * Create a POST request.
     *
     * @param uri
     *            the URI.
     * @return the {@link RestTransportRequest} instance.
     */
    RestTransportRequest doPost(URI uri);

    /**
     * Register a callback internally, which might also establish the corresponding callback channel
     * asynchronously, if there is no existing callback channel.
     *
     * @param uri
     *            the URI where the callback is assigned (agent base url /callbacks).
     * @param callback
     *            the callback to register.
     * @return the {@link UUID} of the callback to use in REST requests.
     * @throws VslException
     *             If the callback channel cannot be established.
     */
    UUID registerCallback(URI uri, VslCallback callback) throws VslException;
}
