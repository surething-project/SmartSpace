package org.ds2os.vsl.rest.jetty.client;

import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.transport.*;
import org.ds2os.vsl.exception.CallbackInvocationException;
import org.ds2os.vsl.exception.UnexpectedErrorException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.client.CallbackRegistry;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Jetty WebSocket connection for callbacks.
 *
 * @author felix
 */
public final class JettyWebsocketClient implements WebSocketListener, Runnable {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JettyWebsocketClient.class);

    /**
     * The URI to which this WebSocket is connected.
     */
    private final URI uri;

    /**
     * The {@link RestTransportContext}.
     */
    private final RestTransportContext context;

    /**
     * The callback registry.
     */
    private final CallbackRegistry callbackRegistry;

    /**
     * Thread for pinging the current session frequently (active keepalive).
     */
    // TODO: own Thread is not a good solution.
    private final Thread pingThread;

    /**
     * The session of the WebSocket.
     */
    private Session session;

    /**
     * The lock used for the non final session.
     */
    private final Object sessionLock = new Object();

    /**
     * The content type used for the messages.
     */
    private String contentType;

    /**
     * The handler for certain messages of the WebSocket.
     */
    private final WebSocketClientHandler clientHandler;

    /**
     * Construct a new Jetty WebSocket with a connection to the URI.
     *
     * @param uri
     *            the URI this WebSocket will be connected to.
     * @param context
     *            the {@link RestTransportContext}.
     * @param callbackRegistry
     *            the callback registry.
     * @param clientHandler
     *            the WebSocket handler.
     */
    public JettyWebsocketClient(
            final URI uri,
            final RestTransportContext context,
            final CallbackRegistry callbackRegistry,
            final WebSocketClientHandler clientHandler
    ) {
        this.uri = uri;
        this.context = context;
        this.callbackRegistry = callbackRegistry;
        this.clientHandler = clientHandler;
        this.pingThread = new Thread(this);
    }

    @Override
    public void onWebSocketConnect(final Session sess) {
        session = sess;
        final String responseContentType = sess.getUpgradeResponse()
                .getHeader(HttpHeader.CONTENT_TYPE.asString());
        if (responseContentType == null || "".equals(responseContentType)) {
            // FIXME: do something
            LOG.error("No content type in WebSocket upgrade response!");
            contentType = VslMimeTypes.JSON;
        } else {
            contentType = responseContentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        }
        if (!pingThread.isAlive()) {
            pingThread.start();
        }
        LOG.debug("WebSocket connection to {} is ready.", uri);
    }

    @Override
    public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        final VslMapper mapper = context.getMapper(contentType);
        if (mapper == null) {
            LOG.error("Could not find suitable mapper for content type {}.", contentType);
            return;
        }

        final CallbackInvocationMessage invocationMessage;
        try (InputStream input = new ByteArrayInputStream(payload, offset, len)) {
            invocationMessage = mapper.readValue(input, CallbackInvocationMessage.class);
        } catch (final IOException e) {
            LOG.error("Could not read callback invocation message.", e);
            return;
        }
        onInvocationMessage(invocationMessage);
    }

    @Override
    public void onWebSocketText(final String message) {
        final VslMapper mapper = context.getMapper(contentType);
        if (mapper == null) {
            LOG.error("Could not find suitable mapper for content type {}.", contentType);
            return;
        }

        final CallbackInvocationMessage invocationMessage;
        try (InputStream input = new ByteArrayInputStream(message.getBytes(mapper.getContentEncoding()))) {
                invocationMessage = mapper.readValue(input, CallbackInvocationMessage.class);
        } catch (final IOException e) {
            LOG.error("Could not read callback invocation message.", e);
            return;
        }
        onInvocationMessage(invocationMessage);
    }

    /**
     * Helper which is internally invoked once an invocation message is received.
     *
     * @param invocationMessage
     *            the {@link CallbackInvocationMessage}.
     */
    private void onInvocationMessage(final CallbackInvocationMessage invocationMessage) {
        LOG.debug("incoming callback, id {}, serial {}, method {}",
                invocationMessage.getCallbackId(),
                invocationMessage.getSerial(),
                invocationMessage.getInvokedMethod());
        try {
            final VslCallback callback = callbackRegistry
                    .getCallback(invocationMessage.getCallbackId());
            if (callback == null) {
                throw new CallbackInvocationException("No callback registered for callback UUID "
                        + invocationMessage.getCallbackId());
            }

            final CallbackMethod method = invocationMessage.getInvokedMethod();
            if (method == CallbackMethod.VGET_STREAM) {
                handleVGetStreamMessage(callback, method, invocationMessage);
            } else if (method == CallbackMethod.VSET_STREAM) {
                handleVSetStreamMessage(callback, method, invocationMessage);
            } else {
                // Send the response back with no data, or the VslNode for a VGET.
                sendResponse(
                        invocationMessage,
                        null,
                        CallbackHelper.invokeCallbackMethod(callback, invocationMessage)
                );
            }
        } catch (final VslException e) {
            LOG.error("Callback invocation threw exception:", e);
            sendResponse(invocationMessage, e, null);
        }
    }

    /**
     * Handles an incoming VGET_STREAM message. It sends back a positive response and then
     * calls the corresponding method on the WebSocket handler.
     *
     * @param callback
     *      the registered callback for this method.
     * @param method
     *      the method of the WebSocket message.
     * @param message
     *      the original callback invocation message which is replied to.
     * @throws VslException
     *      if a VslException occurs.
     */
    private void handleVGetStreamMessage(
            final VslCallback callback,
            final CallbackMethod method,
            final CallbackInvocationMessage message
    ) throws VslException {
        if (clientHandler == null) {
            throw new CallbackInvocationException("no websocket client handler has been provided");
        }

        // Send a positive message back for the WebSocket request and then
        // send a new HTTP PUT request with the stream.
        // This way, the callback socket is not blocked.

        final VslVirtualNodeHandler handler = CallbackHelper.callbackCast(
                callback, VslVirtualNodeHandler.class, method
        );

        sendResponse(message, null, null);

        // Send the stream. The stream is closed in setStreamForCallback().
        clientHandler.setStreamForCallback(
                handler.getStream(message.getAddress(), message.getIdentity()),
                message.getCallbackId(),
                message.getSerial()
        );
    }

    /**
     * Handles an incoming VSET_STREAM message. It sends back a positive response and then
     * calls the corresponding method on the WebSocket handler.
     *
     * @param callback
     *      the registered callback for this method.
     * @param method
     *      the method of the WebSocket message.
     * @param message
     *      the original callback invocation message which is replied to.
     * @throws VslException
     *      if a VslException occurs.
     */
    private void handleVSetStreamMessage(
            final VslCallback callback,
            final CallbackMethod method,
            final CallbackInvocationMessage message
    ) throws VslException {
        if (clientHandler == null) {
            throw new CallbackInvocationException("no websocket client handler has been provided");
        }

        // Send a positive message back for the WebSocket request and then
        // send a new HTTP GET request to receive the stream.
        // This way, the callback socket is not blocked.
        final VslVirtualNodeHandler handler = CallbackHelper.callbackCast(
                callback, VslVirtualNodeHandler.class, method
        );

        sendResponse(message, null, null);

        final InputStream stream = clientHandler.getStreamFromCallback(message.getCallbackId(), message.getSerial());

        // The first byte sent is a dummy byte needed to prevent timeouts.
        // Discard it.
        try {
            int dummy = stream.read();
            if (dummy == -1) {
                // Stream was closed already.
                return;
            }

            if (dummy != 0) {
                // Close stream!
                stream.close();
                throw new UnexpectedErrorException(
                        "stream with id " + message.getCallbackId() + "-" + message.getSerial() + " had unexpected " +
                                "dummy byte " + dummy);
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            try { stream.close(); }
            catch (IOException f) { msg += "\nCould not close stream: " + f.getMessage(); }
            throw new UnexpectedErrorException(msg);
        }

        // Call the handler, which also closes the stream.
        handler.setStream(message.getAddress(), stream, message.getIdentity());
    }

    /**
     * Send a response to the server.
     *
     * @param invocationMessage
     *            the original callback invocation message which is replied to.
     * @param exception
     *            optionally an exception which occurred.
     * @param data
     *            optionally Vsl node data to include in the response (only for VGET).
     */
    private void sendResponse(final CallbackInvocationMessage invocationMessage,
            final VslException exception, final VslNode data) {
        final VslMapper mapper = context.getMapper(contentType);
        if (mapper == null) {
            LOG.error("Could not find suitable mapper for content type {}.", contentType);
            return;
        }

        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            mapper.writeValue(
                    output,
                    new CallbackResponseMessage(
                            invocationMessage.getCallbackId(),
                            invocationMessage.getSerial(),
                            exception == null ? null : new ExceptionMessage(exception),
                            data
                    )
            );

            if ("".equals(mapper.getContentEncoding())) {
                synchronized (sessionLock) {
                    session.getRemote().sendBytes(ByteBuffer.wrap(output.toByteArray()));
                }
            } else {
                final String stringData = Charset.
                        forName(mapper.getContentEncoding()).
                        decode(ByteBuffer.wrap(output.toByteArray())).
                        toString();
                synchronized (sessionLock) {
                    session.getRemote().sendString(stringData);
                }
            }
        } catch (final IOException e) {
            onWebSocketError(e);
        }
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        pingThread.interrupt();
        LOG.debug("WebSocket connection to {} closed with status {}: {}", uri, statusCode, reason);
    }

    @Override
    public void onWebSocketError(final Throwable cause) {
        if (session != null) {
            LOG.error("Closing WebSocket connection because of error:", cause);
            final String reason;
            if (cause.getMessage() == null) {
                reason = cause.getClass().toString();
            } else {
                reason = cause.getClass() + ": " + cause.getMessage();
            }
            synchronized (sessionLock) {
                session.close(1003, reason);
            }
            // TODO: check if this is redundant
            onWebSocketClose(1003, reason);
        }
    }

    @Override
    public void run() {
        while (session.isOpen()) {
            try {
                Thread.sleep(session.getIdleTimeout() / 2);
                synchronized (sessionLock) {
                    session.getRemote().sendPing(ByteBuffer.allocate(0));
                }
            } catch (final IOException e) {
                onWebSocketError(e);
            } catch (final InterruptedException e) {
                LOG.debug("WebSocket ping thread got interrupted.");
                return;
            }
        }
    }
}
