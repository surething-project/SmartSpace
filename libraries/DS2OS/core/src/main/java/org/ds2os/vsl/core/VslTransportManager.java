package org.ds2os.vsl.core;

import java.util.Collection;

/**
 * Transport manager which provides access to the transport modules for specific transport
 * interaction.
 *
 * @author jay, felix
 */
public interface VslTransportManager {

    /**
     * Get all connectors of all registered transports.
     *
     * @return Collection of {@link VslTransportConnector}.
     */
    Collection<VslTransportConnector> getAllTransportConnectors();

    /**
     * Get a transport for sending requests to another KA by the remote KA's URL(s). The best
     * suitable transport module is choosen automatically.
     *
     * @param remoteURLs
     *            URLs of the remote KA to which one transport with request handling is returned.
     * @return a properly configured {@link VslRequestHandler} of the best suitable transport.
     */
    VslRequestHandler getTransportToKA(String... remoteURLs);

    /**
     * Get a transport for KA sync requests to another KA by the remote KA's URL(s). The best
     * suitable transport module is choosen automatically.
     *
     * @param remoteURLs
     *            URLs of remote KA that are available for connection via selected local transport.
     * @return {@link VslKASyncConnector} object implemented by a transport.
     */
    VslKASyncConnector getKASyncConnector(String... remoteURLs);

    /**
     * The method returns all {@link VslAlivePingSender} objects in iterator manner.
     *
     * @return Collection of {@link VslAlivePingSender}.
     */
    Collection<VslAlivePingSender> getAlivePingSenders();

    /**
     * The method returns a collection of needed {@link VslKORUpdateSender} instances in order to
     * contact all connectedKAs.
     *
     * @param connectedKAs
     *            the connected KAs which should be reached.
     * @return Collection of {@link VslKORUpdateSender}.
     */
    Collection<VslKORUpdateSender> getKORUpdateSenders(VslKAInfo... connectedKAs);
}
