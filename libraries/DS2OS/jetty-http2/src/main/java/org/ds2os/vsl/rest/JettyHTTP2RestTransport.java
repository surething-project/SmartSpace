package org.ds2os.vsl.rest;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.netutils.NetUtils;
import org.ds2os.vsl.rest.jetty.client.JettyHTTP2Client;
import org.ds2os.vsl.rest.jetty.server.JettyHTTP2Server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * KA module for instantiating a Jetty server with a REST transport.
 *
 * @author felix
 */
public final class JettyHTTP2RestTransport extends AbstractVslModule implements VslTransport {

    /**
     * The internal running {@link JettyHTTP2Server}.
     */
    private final JettyHTTP2Server jettyServer;

    /**
     * The internal initialized {@link JettyHTTP2Client} for requests to other KAs.
     */
    private final JettyHTTP2Client jettyClient;

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
    public JettyHTTP2RestTransport(final VslX509Authenticator sslAuthenticator,
            final VslRequestHandler requestHandler, final VslKORSyncHandler korSyncHandler,
            final VslKORUpdateHandler korUpdateHandler, final RestTransportContext context,
            final org.eclipse.jetty.server.Handler... otherHandlers) {
        // FIXME: better handling of otherHandlers
        this.context = context;

        final HttpHandler vslHttpHandler = new SimpleVslHttpHandler(sslAuthenticator,
                requestHandler, korSyncHandler, korUpdateHandler, context);

        this.jettyServer = new JettyHTTP2Server(context, vslHttpHandler, otherHandlers);
        this.jettyClient = new JettyHTTP2Client(context);
    }

    @Override
    public void activate() throws Exception {
        jettyClient.start();
        jettyServer.addHttps2Connector(
                NetUtils.getLocalAddresses(context.getConfig()).keySet(),
                context.getConfig().getPort()
        );
        jettyServer.start();
    }

    @Override
    public void shutdown() {
        jettyServer.stop();
        jettyClient.stop();
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
