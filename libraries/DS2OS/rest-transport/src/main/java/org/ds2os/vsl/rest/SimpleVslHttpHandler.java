package org.ds2os.vsl.rest;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.impl.HandshakeData;
import org.ds2os.vsl.core.impl.KAIdentity;
import org.ds2os.vsl.core.impl.KORUpdateRequest;
import org.ds2os.vsl.core.impl.ServiceManifest;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.transport.ExceptionMessage;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.Stream;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/**
 * Jetty handler for the VSL REST transport with a single content mapper.
 *
 * @author borchers
 * @author felix
 */
public final class SimpleVslHttpHandler extends AbstractVslHttpHandler {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SimpleVslHttpHandler.class);

    /**
     * The {@link VslRequestHandler} to which the requests are passed.
     */
    private final VslRequestHandler vslHandler;

    /**
     * The {@link VslKORSyncHandler} to which KOR sync requests are passed.
     */
    private final VslKORSyncHandler korSyncHandler;

    /**
     * The {@link VslKORUpdateHandler} to which KOR update requests are passed.
     */
    private final VslKORUpdateHandler korUpdateHandler;

    /**
     * Constructor with an {@link VslX509Authenticator}, a {@link VslRequestHandler}, a
     * {@link VslKORSyncHandler}, {@link VslKORUpdateHandler} and a {@link VslMapper}.
     *
     * @param authenticator
     *            the authenticator used by this handler.
     * @param handler
     *            the request handler to which the VSL requests are passed.
     * @param syncHandler
     *            the KOR sync handler to which KOR sync requests are passed.
     * @param updateHandler
     *            the KOR update handler to which KOR update requests are passed.
     * @param helper
     *            the REST transport helper.
     */
    public SimpleVslHttpHandler(final VslX509Authenticator authenticator,
            final VslRequestHandler handler, final VslKORSyncHandler syncHandler,
            final VslKORUpdateHandler updateHandler, final RestTransportContext helper) {
        super(authenticator, helper);
        vslHandler = handler;
        korSyncHandler = syncHandler;
        korUpdateHandler = updateHandler;
    }

    /**
     * Check the content type request header for conformance with this handler.
     *
     * @param request
     *            the {@link HttpServletRequest} object.
     * @return true iff the request is valid for this handler.
     */
    private VslMapper checkContentType(final HttpServletRequest request) {
        if (request.getContentType() == null) {
            return null;
        }
        final String contentType = request.getContentType().split(";")[0];
        LOG.debug("Content-type: {}", contentType);
        final VslMapper mapper = getRestTransportContext().getMapper(contentType);

        // TODO: advanced encoding checking...
        // final String encoding = request.getCharacterEncoding();
        // if (encoding != null) {
        // LOG.debug("Character encoding: {}", encoding);
        // if (!"".equals(getMapper().getContentEncoding())
        // && !getMapper().getContentEncoding().equals(encoding)) {
        // return false;
        // }
        // }

        return mapper;
    }

    @Override
    protected void doGet(final String address, final String query,
            final ReceivedRestRequest request, final VslIdentity vslIdentity)
            throws VslException, IOException {
        final VslNode result;

        if (!address.startsWith("/vsl/")) {
            request.sendError(HttpServletResponse.SC_NOT_FOUND, "No /vsl prefix in URL");
            return;
        }
        final String vslAddress = address.substring("/vsl".length());

        // Check, if a stream has been requested.
        final String accept = request.getAccept();
        if (accept.equals(VslMimeTypes.BINARY)) {
            // Request the stream from the handler and copy it back to our response.
            try (
                    final InputStream input = vslHandler.getStream(vslAddress, vslIdentity);
                    final OutputStream output = request.respond(HttpServletResponse.SC_OK, accept)
            ) {
                Stream.copy(input, output);
            }
            return;
        }

        if (query != null) {
            result = vslHandler.get(vslAddress,
                    new AddressParameters(AddressParser.getParametersFromURIQuery(query)),
                    vslIdentity);
        } else {
            result = vslHandler.get(vslAddress, vslIdentity);
        }

        final VslMapper mapper = getMapper(accept);
        if (mapper == null) {
            throw new IOException("No content mapper for accept string: " + accept);
        }
        String contentType = mapper.getContentType();
        if (!"".equals(mapper.getContentEncoding())) {
            contentType += "; " + mapper.getContentEncoding();
        }

        try (OutputStream output = request.respond(HttpServletResponse.SC_OK, contentType)) {
            mapper.writeValue(output, result);
        }
    }

    // @Override
    // protected void doGet(final String address, final String query, final HttpServletRequest
    // request,
    // final HttpServletResponse response, final VslIdentity vslIdentity)
    // throws VslException, IOException {
    // final VslNode result;
    //
    // if (!address.startsWith("/vsl/")) {
    // response.sendError(HttpServletResponse.SC_NOT_FOUND, "No /vsl prefix in URL");
    // return;
    // }
    // final String vslAddress = address.substring("/vsl".length());
    // if (query != null) {
    // result = vslHandler.get(vslAddress,
    // new AddressParameters(AddressParser.getParametersFromURIQuery(query)),
    // vslIdentity);
    // } else {
    // result = vslHandler.get(vslAddress, vslIdentity);
    // }
    // response.setStatus(HttpServletResponse.SC_OK);
    //
    // final VslMapper mapper = getMapper(request.getHeader("accept"));
    // if (mapper == null) {
    // throw new IOException(
    // "No content mapper for accept string: " + request.getHeader("accept"));
    // }
    // response.setContentType(mapper.getContentType());
    // if (!"".equals(mapper.getContentEncoding())) {
    // response.setCharacterEncoding(mapper.getContentEncoding());
    // }
    // final OutputStream output = response.getOutputStream();
    // try {
    // mapper.writeValue(output, result);
    // } finally {
    // output.close();
    // }
    // }

    @Override
    protected void doPut(final String address, final HttpServletRequest request,
            final HttpServletResponse response, final VslIdentity vslIdentity)
            throws VslException, IOException {
        if (!address.startsWith("/vsl/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No /vsl prefix in URL");
            return;
        }
        final String vslAddress = address.substring("/vsl".length());

        // Check, if a stream has been sent.
        if (request.getContentType().equals(VslMimeTypes.BINARY)) {
            try (final InputStream stream = request.getInputStream()) {
                vslHandler.setStream(vslAddress, stream, vslIdentity);
            }

            // Send back the response code.
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        final VslMapper mapper = checkContentType(request);
        if (mapper == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }
        final InputStream input = request.getInputStream();
        final VslNode node;
        try {
            node = mapper.readValue(input, VslNode.class);
        } catch (final IOException e) {
            LOG.debug("IOException during PUT parsing:", e);
            String message = e.getMessage();
            final int firstLineBreak = message.indexOf('\n');
            if (firstLineBreak > 0) {
                message = message.substring(0, firstLineBreak);
            }
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
            return;
        } finally {
            input.close();
        }

        // perform set operation and respond without content
        LOG.debug("Set value: {}, {}", vslAddress, node.getValue());
        vslHandler.set(vslAddress, node, vslIdentity);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(final String address, final String query,
            final HttpServletRequest request, final HttpServletResponse response,
            final VslIdentity vslIdentity) throws VslException, IOException {
        final VslMapper mapper = checkContentType(request);
        if (mapper == null) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        try (final InputStream input = request.getInputStream()) {
            LOG.debug("{} {}", address, query);
            if ("/service/register".equals(address)) {
                performRegisterService(input, response, vslIdentity, mapper);
            } else if ("/service/unregister".equals(address)) {
                performUnregisterService(input, response, vslIdentity, mapper);
            } else if ("/ka/handshake".equals(address)) {
                performKAHandshake(input, response, vslIdentity, mapper);
            } else if ("/ka/requestKorUpdate".equals(address)) {
                performKORUpdateRequest(input, response, vslIdentity, mapper);
            } else if (address.startsWith("/vsl/")) {
                final String vslAddress = address.substring("/vsl".length());
                final PostOperation operation = mapper.readValue(input, PostOperation.class);
                performOperation(vslAddress, query, operation, response, vslIdentity);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No /vsl prefix in URL");
            }
        } catch (final IOException e) {
            String message = e.getMessage();
            final int firstLineBreak = message.indexOf('\n');
            if (firstLineBreak > 0) {
                message = message.substring(0, firstLineBreak);
            }
            LOG.error("Bad Request:", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
        }
    }

    /**
     * Perform the registration of a service.
     *
     * @param input
     *            the {@link InputStream} to read additional data.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @param mapper
     *            the {@link VslMapper} to use with the provided content type.
     * @throws VslException
     *             If a VslException occurs during VSL operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    private void performRegisterService(final InputStream input, final HttpServletResponse response,
            final VslIdentity vslIdentity, final VslMapper mapper)
            throws VslException, IOException {
        final VslServiceManifest manifest = mapper.readValue(input, ServiceManifest.class);
        final String result;
        try {
            result = vslHandler.registerService(manifest, vslIdentity);
        } catch (final VslException | RuntimeException e) {
            LOG.warn("Exception in register service:", e);
            throw e;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mapper.getContentType());
        mapper.writeValue(response.getOutputStream(), result);
    }

    /**
     * Perform the deregistration of a service.
     *
     * @param input
     *            the {@link InputStream} to read additional data.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @param mapper
     *            the {@link VslMapper} to use with the provided content type.
     * @throws VslException
     *             If a VslException occurs during VSL operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    private void performUnregisterService(final InputStream input,
            final HttpServletResponse response, final VslIdentity vslIdentity,
            final VslMapper mapper) throws VslException, IOException {
        // need to read empty object to prevent CSRF
        mapper.readValue(input, Object.class);

        // perform
        vslHandler.unregisterService(vslIdentity);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * Perform the handshake between two KAs.
     *
     * @param input
     *            the {@link InputStream} to read additional data.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @param mapper
     *            the {@link VslMapper} to use with the provided content type.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    private void performKAHandshake(final InputStream input, final HttpServletResponse response,
            final VslIdentity vslIdentity, final VslMapper mapper)
            throws IOException {
        if (!(vslIdentity instanceof KAIdentity)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "KA operation issued from service");
            return;
        }
        final VslHandshakeData handshakeRequest = mapper.readValue(input, HandshakeData.class);
        final VslHandshakeData handshakeResult;
        try {
            handshakeResult = korSyncHandler.handleHandshakeRequest(handshakeRequest);

        } catch (final RuntimeException e) {
            LOG.warn("Exception in KORSyncHandler:", e);
            throw e;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mapper.getContentType());
        mapper.writeValue(response.getOutputStream(), handshakeResult);
    }

    /**
     * Perform the handling of a request for a KOR update.
     *
     * @param input
     *            the {@link InputStream} to read additional data.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @param mapper
     *            the {@link VslMapper} to use with the provided content type.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    private void performKORUpdateRequest(final InputStream input,
            final HttpServletResponse response, final VslIdentity vslIdentity,
            final VslMapper mapper) throws IOException {
        if (!(vslIdentity instanceof KAIdentity)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "KA operation issued from service");
            return;
        }
        final KORUpdateRequest request = mapper.readValue(input, KORUpdateRequest.class);
        final VslKORUpdate korUpdate = korUpdateHandler.getKORUpdateFromHash(request.getHashFrom());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mapper.getContentType());
        mapper.writeValue(response.getOutputStream(), korUpdate);
    }

    /**
     * Perform the {@link PostOperation} on the specified VSL address.
     *
     * @param address
     *            the VSL address.
     * @param query
     *            Query parameters of the operation.
     * @param operation
     *            the {@link PostOperation} which was received.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws VslException
     *             If a VslException occurs during VSL operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    private void performOperation(final String address, final String query,
            final PostOperation operation, final HttpServletResponse response,
            final VslIdentity vslIdentity) throws VslException, IOException {
        switch (operation.getOperation()) {
        case NOTIFY:
            vslHandler.notify(address, vslIdentity);
            break;
        case SUBSCRIBE:
            if (query != null) {
                vslHandler.subscribe(address,
                        createCallback(operation.getCallbackId(), vslIdentity, VslSubscriber.class),
                        new AddressParameters(AddressParser.getParametersFromURIQuery(query)),
                        vslIdentity);
            } else {
                vslHandler.subscribe(address,
                        createCallback(operation.getCallbackId(), vslIdentity, VslSubscriber.class),
                        vslIdentity);
            }
            break;
        case UNSUBSCRIBE:
            if (query != null) {
                vslHandler.unsubscribe(address,
                        new AddressParameters(AddressParser.getParametersFromURIQuery(query)),
                        vslIdentity);
            } else {
                vslHandler.unsubscribe(address, vslIdentity);
            }
            break;
        case REGISTER_VIRTUAL_NODE:
            vslHandler.registerVirtualNode(address, createCallback(operation.getCallbackId(),
                    vslIdentity, VslVirtualNodeHandler.class), vslIdentity);
            break;
        case UNREGISTER_VIRTUAL_NODE:
            vslHandler.unregisterVirtualNode(address, vslIdentity);
            break;
        case LOCK_SUBTREE:
            vslHandler.lockSubtree(address,
                    createCallback(operation.getCallbackId(), vslIdentity, VslLockHandler.class),
                    vslIdentity);
            break;
        case COMMIT_SUBTREE:
            vslHandler.commitSubtree(address, vslIdentity);
            break;
        case ROLLBACK_SUBTREE:
            vslHandler.rollbackSubtree(address, vslIdentity);
            break;
        default:
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doOptions(final String address, final HttpServletRequest request,
            final HttpServletResponse response, final VslIdentity vslIdentity) {
        if (request.getHeader("Origin") != null) {
            response.setHeader("Access-Control-Allow-Headers", "content-type");
            response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, PUT, POST");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Vary", "Origin");
        }
        response.setHeader("Allow", "GET, HEAD, PUT, POST, OPTIONS");

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doOtherMethod(final String address, final String method,
            final HttpServletRequest request, final HttpServletResponse response,
            final VslIdentity vslIdentity) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    public void handleError(final int status, final String message,
            final HttpServletResponse response) throws IOException {
        response.setStatus(status);
        final VslMapper mapper = getRestTransportContext().getMapper(VslMimeTypes.JSON);
        if (mapper == null) {
            throw new IOException("no mapper available for content type " + VslMimeTypes.JSON);
        }

        response.setContentType(mapper.getContentType());
        if (!"".equals(mapper.getContentEncoding())) {
            response.setCharacterEncoding(mapper.getContentEncoding());
        }

        try (OutputStream output = response.getOutputStream()) {
            // FIXME: find a way to get the real exception!
            final ExceptionMessage exception = new ExceptionMessage(status, message,
                    VslException.class.getName());
            LOG.debug("Sending exception ({}): {}", status, message);
            mapper.writeValue(output, exception);
        }
    }
}
