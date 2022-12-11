package org.ds2os.vsl.rest.jetty.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.transport.AbstractCallbackSender;
import org.ds2os.vsl.core.transport.CallbackInvocationMessage;
import org.ds2os.vsl.core.transport.CallbackResponseListener;
import org.ds2os.vsl.core.transport.CallbackResponseMessage;
import org.ds2os.vsl.core.transport.CallbackSender;
import org.ds2os.vsl.exception.CallbackInvocationException;
import org.ds2os.vsl.exception.FailedAuthenticationException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.HttpHandler;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link CallbackSender} with an {@link WebSocketListener} for Jetty
 * websockets.
 *
 * @author felix
 */
public class JettyWebsocket extends AbstractCallbackSender
        implements WebSocketListener, CallbackSender {

    /**
     * The SLF4J log.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JettyWebsocket.class);

    /**
     * The {@link HttpHandler}.
     */
    private final HttpHandler handler;

    /**
     * The session of the websocket.
     */
    private Session session;

    /**
     * The identity of the connected client.
     */
    private VslIdentity identity;

    /**
     * The content type of the websocket.
     */
    private VslMapper mapper;

    /**
     * Default constructor.
     *
     * @param handler
     *            the {@link HttpHandler}.
     * @param subProtocol
     *            the selected sub protocol.
     * @param acceptHeader
     *            the accept header of the HTTP request.
     */
    public JettyWebsocket(final HttpHandler handler, final String subProtocol,
            final String acceptHeader) {
        this.handler = handler;
        if (acceptHeader == null) {
            // FIXME: too hardcoded stuff!
            mapper = handler.getMapper(VslMimeTypes.JSON);
        } else {
            mapper = handler.getMapper(acceptHeader);
        }
    }

    /**
     * Get the HTTP content type of this websocket.
     *
     * @return the content type.
     */
    public String getContentType() {
        if (mapper == null) {
            return "";
        } else {
            return mapper.getContentType();
        }
    }

    @Override
    public final void onWebSocketConnect(final Session sess) {
        if (mapper == null) {
            sess.getUpgradeResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
            sess.getUpgradeResponse().setStatusReason("No acceptable content type supported.");
            sess.getUpgradeResponse().setSuccess(false);
            return;
        }

        try {
            final ServletUpgradeRequest request = (ServletUpgradeRequest) sess.getUpgradeRequest();

            if (request.getCertificates() == null) {
                // FIXME: dirty workaround for Safari
                identity = new ServiceIdentity("service2", "service2");
                LOG.debug(
                        "WebSocket workaround for Safari: falling back to dummy idenity service2");
            } else {
                identity = handler.authenticate(request.getCertificates(),
                        request.getHeader("Authorization"));
            }
        } catch (final FailedAuthenticationException e) {
            try {
                sess.getUpgradeResponse().sendForbidden(e.getMessage());
            } catch (final IOException e2) {
                // ignore
                return;
            }
            return;
        }
        session = sess;
        handler.websocketConnected(this, identity);
    }

    @Override
    public final void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        final InputStream input = new ByteArrayInputStream(payload, offset, len);
        try {
            final CallbackResponseMessage response = mapper.readValue(input,
                    CallbackResponseMessage.class);
            input.close();
            final CallbackResponseListener responseListener = getResponseListener(
                    response.getCallbackId());
            if (responseListener != null) {
                responseListener.receivedResponse(response);
            }
        } catch (final IOException e) {
            onWebSocketError(e);
        }
    }

    @Override
    public final void onWebSocketText(final String message) {
        try {
            final InputStream input = new ByteArrayInputStream(
                    message.getBytes(mapper.getContentEncoding()));
            final CallbackResponseMessage response;
            try {
                response = mapper.readValue(input, CallbackResponseMessage.class);
            } finally {
                input.close();
            }
            final CallbackResponseListener responseListener = getResponseListener(
                    response.getCallbackId());
            if (responseListener != null) {
                responseListener.receivedResponse(response);
            }
        } catch (final IOException e) {
            onWebSocketError(e);
        }
    }

    @Override
    public final void onWebSocketClose(final int statusCode, final String reason) {
        handler.websocketClosed(statusCode, reason, identity);
    }

    @Override
    public final void onWebSocketError(final Throwable cause) {
        if (session != null) {
            final String reason;
            if (cause.getMessage() == null) {
                reason = cause.getClass().toString();
            } else {
                reason = cause.getClass() + ": " + cause.getMessage();
            }
            synchronized (session) {
                session.close(1011, reason);
            }
            // TODO: check if this is redundant
            onWebSocketClose(1011, reason);
        }
    }

    @Override
    public final void invokeCallback(final CallbackInvocationMessage invocationMessage)
            throws VslException {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                mapper.writeValue(output, invocationMessage);
            } finally {
                output.close();
            }
            if ("".equals(mapper.getContentEncoding())) {
                synchronized (session) {
                    session.getRemote().sendBytes(ByteBuffer.wrap(output.toByteArray()));
                }
            } else {
                final String stringData = Charset.forName(mapper.getContentEncoding())
                        .decode(ByteBuffer.wrap(output.toByteArray())).toString();
                synchronized (session) {
                    session.getRemote().sendString(stringData);
                }
            }
        } catch (final IOException e) {
            throw new CallbackInvocationException("IOException during callback invocation.", e);
        }
    }
}
