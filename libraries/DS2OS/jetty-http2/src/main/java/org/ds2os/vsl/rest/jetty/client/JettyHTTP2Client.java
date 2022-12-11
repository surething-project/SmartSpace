package org.ds2os.vsl.rest.jetty.client;

import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.jetty.JettyContext;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.LoggerFactory;

/**
 * Generic Jetty-based HTTP/2 client for VSL REST transports.
 *
 * @author borchers
 * @author felix
 */
public class JettyHTTP2Client extends JettyClient {
    static {
        LOGGER = LoggerFactory.getLogger(JettyHTTP2Client.class);
    }

    /**
     * Create a new Jetty client instance for executing requests.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     */
    public JettyHTTP2Client(final RestTransportContext context) {
        super(context);

        // Extend the SSL context with HTTP/2 requirements.
        final SslContextFactory sslContextFactory = new JettyContext(context)
                .getSslContextFactory();
        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setUseCipherSuitesOrder(true);

        // Create the HTTP Client.
        jettyClient = new HttpClient(
                new HttpClientTransportOverHTTP2(new HTTP2Client()),
                sslContextFactory
        );
        jettyClient.setFollowRedirects(false);
        jettyClient.setIdleTimeout(RestTransportContext.IDLE_TIMEOUT);

        // Create the WebSocket Client.
        websocketClient = new WebSocketClient(sslContextFactory, jettyClient.getExecutor(), null);
        websocketClient.setMaxIdleTimeout(RestTransportContext.IDLE_TIMEOUT);
        websocketClient.setMaxBinaryMessageBufferSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);
        websocketClient.setMaxTextMessageBufferSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);
    }
}
