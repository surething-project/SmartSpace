package org.ds2os.vsl.core;

import java.util.Collection;

/**
 * Description of a transport module.
 *
 * @author felix
 */
public interface VslTransport {

    /**
     * Get all connectors of this transport.
     *
     * @return Collection of {@link VslTransportConnector}.
     */
    Collection<VslTransportConnector> getConnectors();

    /**
     * Creates a new {@link VslRequestHandler} connected to one of the specified remote URLs.
     *
     * @param remoteURLs
     *            the remote URLs of the intended target.
     * @return a connected {@link VslRequestHandler} or null if this transport can't connect to any
     *         of the remote URLs.
     */
    VslRequestHandler createRequestHandler(String... remoteURLs);

    /**
     * Creates a new {@link VslKASyncConnector} connected to one of the specified remote URLs.
     *
     * @param remoteURLs
     *            the remote URLs of the intended target.
     * @return a connected {@link VslKASyncConnector} or null if this transport can't connect to any
     *         of the remote URLs.
     */
    VslKASyncConnector createKASyncConnector(String... remoteURLs);
}
