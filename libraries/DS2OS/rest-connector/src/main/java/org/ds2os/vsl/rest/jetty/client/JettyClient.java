package org.ds2os.vsl.rest.jetty.client;

import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.exception.UnexpectedErrorException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.client.CallbackRegistry;
import org.ds2os.vsl.rest.client.RestTransportClient;
import org.ds2os.vsl.rest.client.RestTransportRequest;
import org.ds2os.vsl.rest.jetty.JettyContext;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Generic Jetty-based HTTPs client for VSL REST transports.
 *
 * @author felix
 */
public class JettyClient implements RestTransportClient {

    /**
     * The SLF4J logger.
     */
    protected static Logger LOGGER = LoggerFactory.getLogger(JettyClient.class);

    /**
     * The Jetty {@link HttpClient} instance.
     */
    protected HttpClient jettyClient;

    /**
     * Synchronises the access to the {@link #jettyClient}.
     */
    private final Object clientLock = new Object();

    /**
     * The WebSocket client instance.
     */
    protected WebSocketClient websocketClient;

    /**
     * The handler for the {@link #websocketClient}.
     */
    private WebSocketClientHandler wsClientHandler;

    /**
     * The {@link RestTransportContext} with common configuration and tools.
     */
    private final RestTransportContext context;

    /**
     * The {@link CallbackRegistry} of this client.
     */
    private final CallbackRegistry callbackRegistry;

    /**
     * Map the {@link JettyWebsocketClient} instances by URI where they are connected (in case
     * connections to multiple agents are done, this map contains one entry per agent).
     */
    private final Map<URI, JettyWebsocketClient> callbackSockets;

    /**
     * Create a new Jetty client instance for executing requests.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     */
    public JettyClient(final RestTransportContext context) {
        this.context = context;

        final SslContextFactory sslContextFactory = new JettyContext(context)
                .getSslContextFactory();
        jettyClient = new HttpClient(sslContextFactory);
        jettyClient.setFollowRedirects(false);
        jettyClient.setIdleTimeout(RestTransportContext.IDLE_TIMEOUT);

        websocketClient = new WebSocketClient(sslContextFactory, jettyClient.getExecutor(), null);
        websocketClient.setMaxIdleTimeout(RestTransportContext.IDLE_TIMEOUT);
        websocketClient
                .setMaxBinaryMessageBufferSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);
        websocketClient
                .setMaxTextMessageBufferSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);

        callbackRegistry = new CallbackRegistry();
        callbackSockets = new HashMap<URI, JettyWebsocketClient>();
    }

    public void setWsClientHandler(final WebSocketClientHandler wsClientHandler) {
        this.wsClientHandler = wsClientHandler;
    }

    /**
     * Start the Jetty client.
     *
     * @throws IOException
     *             If the Jetty client startup failed.
     */
    public final void start() throws IOException {
        synchronized (clientLock) {
            if (!jettyClient.isRunning()) {
                LOGGER.info("Starting Jetty client...");
                try {
                    jettyClient.start();
                    if (!websocketClient.isRunning()) {
                        websocketClient.start();
                    }
                } catch (final Exception e) {
                    LOGGER.error("Failed to start Jetty client:", e);
                    throw new IOException("Jetty client startup failed.", e);
                }
                LOGGER.info("Jetty client is started.");
            }
        }
    }

    /**
     * Stop the Jetty client.
     */
    public final void stop() {
        synchronized (clientLock) {
            if (jettyClient.isRunning()) {
                LOGGER.info("Stopping Jetty client...");
                try {
                    if (websocketClient.isRunning()) {
                        websocketClient.stop();
                    }
                    jettyClient.stop();
                } catch (final Exception e) {
                    LOGGER.error("Failed to stop Jetty client:", e);
                }
            }
        }
    }

    @Override
    public final RestTransportRequest doGet(final URI uri) {
        return new JettyClientRequest(jettyClient, uri, HttpMethod.GET);
    }

    @Override
    public final RestTransportRequest doPut(final URI uri) {
        return new JettyClientRequest(jettyClient, uri, HttpMethod.PUT);
    }

    @Override
    public final RestTransportRequest doPost(final URI uri) {
        return new JettyClientRequest(jettyClient, uri, HttpMethod.POST);
    }

    @SuppressWarnings("serial")
    @Override
    public final UUID registerCallback(final URI uri, final VslCallback callback)
            throws VslException {
        final UUID callbackUUID;
        synchronized (callbackSockets) {
            if (!callbackSockets.containsKey(uri)) {
                final JettyWebsocketClient newClient = new JettyWebsocketClient(
                        uri, context, callbackRegistry, wsClientHandler
                );
                final ClientUpgradeRequest request = new ClientUpgradeRequest();
                final StringBuilder accept = new StringBuilder();
                for (final String mediaType : context.getContentTypePreference()) {
                    if (accept.length() > 0) {
                        accept.append(',');
                    }
                    accept.append(mediaType);
                }
                request.setHeader(HttpHeader.ACCEPT.asString(), accept.toString());
                request.setSubProtocols(RestTransportContext.WEBSOCKET_PROTOCOL_V1);
                try {
                    // FIXME: timeout configs!
                    websocketClient.connect(newClient, uri, request).get(5, TimeUnit.SECONDS);
                } catch (final IOException e) {
                    throw new UnexpectedErrorException("IOException during WebSocket connect to: " + uri, e);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new UnexpectedErrorException("InterruptedException during WebSocket connect to: " + uri, e);
                } catch (final ExecutionException e) {
                    throw new UnexpectedErrorException("ExecutionException during WebSocket connect to: " + uri, e);
                } catch (final TimeoutException e) {
                    throw new org.ds2os.vsl.exception.TimeoutException(5, TimeUnit.SECONDS, "WebSocket connect to: " + uri);
                }
                callbackSockets.put(uri, newClient);
            }
        }
        synchronized (callbackRegistry) {
            callbackUUID = callbackRegistry.registerCallback(callback);
        }
        return callbackUUID;
    }
}
