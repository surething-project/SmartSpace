package org.ds2os.vsl.ka;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ds2os.vsl.core.VslAlivePingSender;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslKASyncConnector;
import org.ds2os.vsl.core.VslKORUpdateSender;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslTransport;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTransportManager;

/**
 * Transport manager implementation.
 *
 * @author felix
 */
public class TransportManager implements VslTransportManager {

    /**
     * Set of all added transport instances.
     */
    private final Set<VslTransport> allTransports;

    /**
     * All registered transports mapped by their connectors.
     */
    private final ConcurrentMap<VslTransportConnector, VslTransport> transportMap;

    /**
     * Set of all alive ping senders.
     */
    private final Set<VslAlivePingSender> alivePingSenders;

    /**
     * Set of all kor update senders.
     */
    private final Set<VslKORUpdateSender> korUpdateSenders;

    /**
     * Default constructor.
     */
    public TransportManager() {
        allTransports = new HashSet<VslTransport>();
        transportMap = new ConcurrentHashMap<VslTransportConnector, VslTransport>();
        alivePingSenders = new HashSet<VslAlivePingSender>();
        korUpdateSenders = new HashSet<VslKORUpdateSender>();
    }

    /**
     * Register a new transport to the transport manager. Localhost/loopback IPs are NOT accepted,
     * since they should only be allowed locally.
     *
     * @param transport
     *            the transport to register.
     */
    public final void registerTransport(final VslTransport transport) {
        for (final VslTransportConnector connector : transport.getConnectors()) {
            if (transport instanceof VslAlivePingSender) {
                alivePingSenders.add((VslAlivePingSender) transport);
            }
            if (transport instanceof VslKORUpdateSender) {
                korUpdateSenders.add((VslKORUpdateSender) transport);
            }

            // TODO: proper synchronization?
            allTransports.add(transport);
            transportMap.put(connector, transport);
        }
    }

    @Override
    public final Collection<VslTransportConnector> getAllTransportConnectors() {
        return Collections.unmodifiableCollection(transportMap.keySet());
    }

    @Override
    public final VslRequestHandler getTransportToKA(final String... remoteURLs) {
        // TODO: currently, no real preference is implemented - only picks first available transport
        for (final VslTransport transport : allTransports) {
            final VslRequestHandler handler = transport.createRequestHandler(remoteURLs);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    @Override
    public final VslKASyncConnector getKASyncConnector(final String... remoteURLs) {
        // TODO: currently, no real preference is implemented - only picks first available transport
        for (final VslTransport transport : allTransports) {
            final VslKASyncConnector connector = transport.createKASyncConnector(remoteURLs);
            if (connector != null) {
                return connector;
            }
        }
        return null;
    }

    @Override
    public final Collection<VslAlivePingSender> getAlivePingSenders() {
        return Collections.unmodifiableCollection(alivePingSenders);
    }

    @Override
    public final Collection<VslKORUpdateSender> getKORUpdateSenders(
            final VslKAInfo... connectedKAs) {
        // FIXME: honor connected KAs and deduplication!
        return Collections.unmodifiableCollection(korUpdateSenders);
    }

    /**
     * We can't use java.net.URL because we use unofficial protocol names...
     *
     * @param url
     *            the URL to parse.
     * @return the protocol name.
     */
    protected final String getURLProtocol(final String url) {
        final String[] parts = url.split("://");
        if (parts.length > 1) {
            return parts[0];
        } else {
            return url;
        }
    }
}
