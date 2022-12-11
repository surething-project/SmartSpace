package org.ds2os.vsl.rest.jetty.server;

import org.ds2os.vsl.rest.HttpHandler;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.InetAddress;
import java.util.Collection;

/**
 * Creates an embedded Jetty HTTP/2 server.
 *
 * @author felix
 * @author borchers
 */
public class JettyHTTP2Server extends JettyServer {

    /**
     * Create a new Jetty server instance without connectors.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     * @param handler
     *            the handler of all requests.
     * @param otherHandlers
     *            TODO: extra handlers hack.
     */
    public JettyHTTP2Server(final RestTransportContext context, final HttpHandler handler,
            final Handler... otherHandlers) {
        super(context, handler, otherHandlers);
    }

    /**
     * Add a new HTTPs connector to the server.
     * TODO: Once the Jetty team has implemented RFC8441 (https://github.com/eclipse/jetty.project/issues/3537) we can completely remove HTTP1.1, which leads to WebSockets being multiplexed over a single HTTP/2 stream, instead of using an additional TCP connection.
     *
     * @param listenAddrs
     *            the listen addresses.
     * @param port
     *            the port.
     */
    public final void addHttps2Connector(
            final Collection<InetAddress> listenAddrs,
            final int port
    ) {
        // Create an HTTPS configuration.
        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecurePort(port);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        httpsConfig.setSendServerVersion(false);
        httpsConfig.setSendXPoweredBy(false);

        // Create the HTTP1.1 Server Connection from it.
        final HttpConnectionFactory httpFactory = new HttpConnectionFactory(httpsConfig);

        // Create the HTTP/2 Server Connection from it.
        final HTTP2ServerConnectionFactory http2Factory = new HTTP2ServerConnectionFactory(httpsConfig);

        // Create the ALPN Server Connection and set its default protocol to http/2.
        final ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(httpFactory.getProtocol());

        // Extend the SSL context with HTTP/2 requirements, including the much faster
        // Conscrypt SSL provider that uses BoringSSL under the hood.
        final SslContextFactory sslContextFactory = context.getSslContextFactory();
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setUseCipherSuitesOrder(true);

        // For every listen address, add one connector to the server.
        for (final InetAddress listen : listenAddrs) {
            // Construct the connector.
            // Give the SSLConnection the ALPN default protocols as next protocol.
            final ServerConnector http2sConnector = new ServerConnector(
                    jetty,
                    new SslConnectionFactory(sslContextFactory, alpn.getProtocol()),
                    // The order of connectors is important!
                    // First, we use ALPN, followed by HTTP/2 and HTTP1.1.
                    alpn, http2Factory, httpFactory
            );

            // Set the host to the listen address's host.
            http2sConnector.setHost(listen.getHostAddress());
            http2sConnector.setPort(port);
            http2sConnector.setReuseAddress(true);
            http2sConnector.setIdleTimeout(RestTransportContext.IDLE_TIMEOUT);

            jetty.addConnector(http2sConnector);
            listenAddresses.add(listen);
        }

        LOGGER.info("Using HTTP/2 Server");
    }
}
