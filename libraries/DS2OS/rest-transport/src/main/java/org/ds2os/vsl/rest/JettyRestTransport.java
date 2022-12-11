package org.ds2os.vsl.rest;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslKASyncConnector;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslTransport;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.netutils.NetUtils;
import org.ds2os.vsl.rest.jetty.client.JettyClient;
import org.ds2os.vsl.rest.jetty.server.JettyServer;

/**
 * KA module for instantiating a Jetty server with a REST transport.
 *
 * @author felix
 */
public final class JettyRestTransport extends AbstractVslModule implements VslTransport {

    /**
     * The internal running {@link JettyServer}.
     */
    private final JettyServer jettyServer;

    /**
     * The internal initialized {@link JettyClient} for requests to other KAs.
     */
    private final JettyClient jettyClient;

    /**
     * The {@link RestTransportContext} with common configuration and tools.
     */
    private final RestTransportContext context;

    /**
     * Default constructor.
     *
     * @param sslAuthenticator
     *            the {@link VslX509Authenticator}.
     * @param requestHandler
     *            the {@link VslRequestHandler} for handling requests.
     * @param korSyncHandler
     *            the {@link VslKORSyncHandler} for handling KOR sync requests.
     * @param korUpdateHandler
     *            the {@link VslKORUpdateHandler} for handling KOR update requests.
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     */
    public JettyRestTransport(final VslX509Authenticator sslAuthenticator,
            final VslRequestHandler requestHandler, final VslKORSyncHandler korSyncHandler,
            final VslKORUpdateHandler korUpdateHandler, final RestTransportContext context,
            final org.eclipse.jetty.server.Handler... otherHandlers) {
        // FIXME: better handling of otherHandlers
        this.context = context;

        final HttpHandler vslHttpHandler = new SimpleVslHttpHandler(sslAuthenticator,
                requestHandler, korSyncHandler, korUpdateHandler, context);

        this.jettyServer = new JettyServer(context, vslHttpHandler, otherHandlers);
        this.jettyClient = new JettyClient(context);
    }

    @Override
    public void activate() throws Exception {
        jettyClient.start();
        jettyServer.addHttpsConnector(NetUtils.getLocalAddresses(context.getConfig()).keySet(),
                context.getConfig().getPort());
        jettyServer.start();
    }

    @Override
    public void shutdown() {
        jettyClient.stop();
        jettyServer.stop();
        try {
            jettyServer.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Collection<VslTransportConnector> getConnectors() {
        final List<VslTransportConnector> connectors = new ArrayList<VslTransportConnector>();
        for (final URL url : jettyServer.getLocalURLs()) {
            connectors.add(new TransportConnector(url.toString()));
        }
        return connectors;
    }

    @Override
    public VslRequestHandler createRequestHandler(final String... remoteURLs) {
        for (final String url : remoteURLs) {
            if (url.startsWith("https://")) {
                // TODO: reachability and compatibility check!
                return new RestKAToKAConnector(jettyClient, url, context);
            }
        }
        return null;
    }

    @Override
    public VslKASyncConnector createKASyncConnector(final String... remoteURLs) {
        for (final String url : remoteURLs) {
            if (url.startsWith("https://")) {
                // TODO: reachability and compatibility check!
                return new RestKASyncConnector(jettyClient, url, context);
            }
        }
        return null;
    }
}
