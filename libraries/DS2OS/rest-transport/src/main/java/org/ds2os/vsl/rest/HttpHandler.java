package org.ds2os.vsl.rest;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.transport.CallbackSender;

/**
 * Handles HTTP requests independent of the HTTP server implementation. It utilizes javax.servlet
 * components but only basic servlet functionality is provided in embedded environments (no
 * sessions, no user authentication, no context).
 *
 * @author felix
 */
public interface HttpHandler extends RestHandler {

    /**
     * Handle a generic {@link HttpServletRequest}.
     *
     * @param request
     *            the {@link HttpServletRequest} for inspecting request parameters and reading the
     *            request body.
     * @param response
     *            the {@link HttpServletResponse} for sending a response and writing the response
     *            body.
     * @param vslIdentity
     *            the {@link VslIdentity} of the requesting client.
     * @throws IOException
     *             If an I/O error occurs.
     * @throws ServletException
     *             If a servlet method throws an exception.
     */
    void handle(HttpServletRequest request, HttpServletResponse response, VslIdentity vslIdentity)
            throws IOException, ServletException;

    /**
     * Handle an HTTP error with the specified status and message. Should add HTTP header fields and
     * write a body using the HttpServletResponse object.
     *
     * @param status
     *            the HTTP status code.
     * @param message
     *            the error message to include in the output.
     * @param response
     *            the {@link HttpServletResponse} for setting headers and writing the response body.
     * @throws IOException
     *             If an I/O error occurs.
     */
    void handleError(int status, String message, HttpServletResponse response) throws IOException;

    /**
     * Get all supported websocket (sub-)protocols.
     *
     * @return a collection of the protocol names.
     */
    Collection<String> getWebsocketProtocols();

    /**
     * Called when a new websocket connected with the sender for invoking callbacks.
     *
     * @param websocketSender
     *            the {@link CallbackSender} for callback invocation.
     * @param vslIdentity
     *            the {@link VslIdentity} of the connected client.
     */
    void websocketConnected(CallbackSender websocketSender, VslIdentity vslIdentity);

    /**
     * Called when a websocket connection was closed from either side.
     *
     * @param statusCode
     *            the websocket status code of the closing.
     * @param reason
     *            the reason of the closing.
     * @param vslIdentity
     *            the {@link VslIdentity} of the previously connected client.
     */
    void websocketClosed(int statusCode, String reason, VslIdentity vslIdentity);
}
