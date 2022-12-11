package org.ds2os.vsl.rest.jetty.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ds2os.vsl.rest.HttpHandler;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.BufferUtil;

/**
 * Wraps a {@link HttpHandler} in a Jetty handler.
 *
 * @author felix
 */
public final class JettyErrorHandler extends ErrorHandler {

    /**
     * The wrapped HTTP handler.
     */
    private final HttpHandler handler;

    /**
     * Create a new Jetty handler which wraps the {@link HttpHandler}.
     *
     * @param wrapHandler
     *            the handler to wrap for Jetty.
     */
    public JettyErrorHandler(final HttpHandler wrapHandler) {
        handler = wrapHandler;
    }

    @Override
    public void handle(final String target, final Request baseRequest,
            final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final String method = request.getMethod();
        baseRequest.setHandled(true);
        if (!HttpMethod.GET.is(method) && !HttpMethod.POST.is(method)) {
            return;
        }

        final String message;
        if (response instanceof Response) {
            final String reason = ((Response) response).getReason();
            if (reason == null) {
                message = HttpStatus.getMessage(response.getStatus());
            } else {
                message = reason;
            }
        } else {
            message = HttpStatus.getMessage(response.getStatus());
        }
        handler.handleError(response.getStatus(), message, response);
    }

    @Override
    public ByteBuffer badMessageError(final int status, final String reason,
            final HttpFields headerFields) {
        final String realReason;
        if (reason == null) {
            realReason = HttpStatus.getMessage(status);
        } else {
            realReason = reason;
        }
        headerFields.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
        return BufferUtil.toBuffer("Bad Message " + status + ", reason: " + realReason);
    }
}
