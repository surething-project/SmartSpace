package org.ds2os.vsl.rest.jetty.server;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import org.ds2os.vsl.rest.HttpHandler;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Servlet for Jetty's WebSockets (used for callbacks).
 *
 * @author felix
 */
public final class JettyWebsocketServlet extends WebSocketServlet implements WebSocketCreator {

    /**
     * Servlets must be serializable.
     */
    private static final long serialVersionUID = -1727486009023935593L;

    @Override
    public void init() throws ServletException {
        super.init();

        if (getServletContext().getAttribute(JettyServlet.HTTP_HANDLER_ATTRIBUTE) == null) {
            throw new UnavailableException("Could not get HTTP handler.");
        }
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(RestTransportContext.IDLE_TIMEOUT);
        factory.getPolicy()
                .setMaxBinaryMessageSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);
        factory.getPolicy().setMaxTextMessageSize(RestTransportContext.WEBSOCKET_MAX_MESSAGE_SIZE);
        factory.setCreator(this);
    }

    @Override
    public Object createWebSocket(final ServletUpgradeRequest request,
            final ServletUpgradeResponse response) {
        final HttpHandler handler = (HttpHandler) getServletContext()
                .getAttribute(JettyServlet.HTTP_HANDLER_ATTRIBUTE);
        for (final String subProtocol : handler.getWebsocketProtocols()) {
            if (request.hasSubProtocol(subProtocol)) {
                final JettyWebsocket socket = new JettyWebsocket(handler, subProtocol,
                        request.getHeader(HttpHeader.ACCEPT.asString()));
                response.setAcceptedSubProtocol(subProtocol);
                response.setHeader(HttpHeader.CONTENT_TYPE.asString(), socket.getContentType());
                return socket;
            }
        }
        return null;
    }
}
