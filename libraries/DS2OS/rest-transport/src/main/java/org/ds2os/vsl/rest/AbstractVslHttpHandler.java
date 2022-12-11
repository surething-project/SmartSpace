package org.ds2os.vsl.rest;

import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.transport.AbstractTransportCallback;
import org.ds2os.vsl.core.transport.CallbackMethod;
import org.ds2os.vsl.core.transport.CallbackSender;
import org.ds2os.vsl.core.utils.Pipe;
import org.ds2os.vsl.core.utils.Stream;
import org.ds2os.vsl.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Abstract {@link HttpHandler} for the VSL REST transport.
 *
 * @author borchers
 * @author felix
 */
public abstract class AbstractVslHttpHandler extends AbstractVslRestHandler implements HttpHandler {

    /**
     * The SLF4J log.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVslHttpHandler.class);

    /**
     * Map of the connected {@link CallbackSender}s by client id.
     */
    private final Map<String, CallbackSender> callbackSenderByClientId;

    /**
     * Map of callback instances by callback id.
     */
    private final Map<UUID, Callback> callbackById;

    /**
     * Map of {@link CompletableFuture} containing the stream pipe by callbackId + serial.
     */
    private final Map<String, CompletableFuture<Pipe>> awaitedStreams;

    /**
     * Inject the {@link VslX509Authenticator} used for mapping X.509 certificates to Vsl
     * identities.
     *
     * @param authenticator
     *            the authenticator used by this handler.
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     */
    AbstractVslHttpHandler(final VslX509Authenticator authenticator, final RestTransportContext context) {
        super(authenticator, context);

        callbackSenderByClientId = new HashMap<String, CallbackSender>();
        callbackById = new HashMap<UUID, Callback>();
        awaitedStreams = new HashMap<String, CompletableFuture<Pipe>>();
    }

    @Override
    public final void handle(final HttpServletRequest request, final HttpServletResponse response, final VslIdentity vslIdentity) throws IOException {
        final String method = request.getMethod();
        String address = request.getPathInfo();
        if (address == null || address.equals("")) {
            address = "/";
        } else if (!address.startsWith("/")) {
            address = "/" + address;
        }
        final String query = request.getQueryString();
        String fullAddress = address;
        if (query != null) {
            fullAddress += "?" + query;
        }
        LOG.debug("Received {} request on URL {}, queryParameters: {}", method, address, query);

        try {
            if ("GET".equals(method)) {
                // Check, if the GET is awaited for a callback stream.
                final String accept = request.getHeader("accept");
                if (address.startsWith("/callback-stream/") 
                        && accept != null
                        && accept.equals(VslMimeTypes.BINARY)) {
                    handleAwaitedGETCallbackStream(address, response);
                    return;
                }

                doGet(address, query, new ReceivedHttpRequest(request, response), vslIdentity);
            } else if ("PUT".equals(method)) {
                // Check, if the PUT is an awaited callback stream.
                final String contentType = request.getContentType();
                if (address.startsWith("/callback-stream/") &&
                        contentType != null &&
                        contentType.equals(VslMimeTypes.BINARY)) {
                    handleAwaitedPUTCallbackStream(address, request, response);
                    return;
                }

                doPut(address, request, response, vslIdentity);
            } else if ("POST".equals(method)) {
                doPost(address, query, request, response, vslIdentity);
            } else if ("OPTIONS".equals(method)) {
                doOptions(fullAddress, request, response, vslIdentity);
            } else {
                doOtherMethod(fullAddress, method, request, response, vslIdentity);
            }
        } catch (final VslException e) {
            LOG.debug("Sending exception with error code " + e.getErrorCode(), e);
            handleError(e.getErrorCode(), e.getMessage(), response);
        }
    }

    /**
     * Registers an awaited stream for a stream operation in the {@link #awaitedStreams} Map
     * by creating a new Future for it. The key into the Map is created from the given parameters
     * with the help of {@link #awaitedStreamID(UUID, long)}.
     *
     * @param callbackId
     *      the id of the WebSocket callback that was used to trigger the stream operation on the service.
     * @param serial
     *      the serial number of the callback invocation.
     * @return
     *      the Future which can be completed, once the awaited stream arrives.
     */
    private CompletableFuture<Pipe> registerAwaitedStream(final UUID callbackId, final long serial) {
        final CompletableFuture<Pipe> future = new CompletableFuture<>();
        synchronized (awaitedStreams) {
            awaitedStreams.put(awaitedStreamID(callbackId, serial), future);
        }
        return future;
    }

    /**
     * Unregisters an awaited stream from the {@link #awaitedStreams} Map that has been previously
     * registered by a call to {@link #registerAwaitedStream(UUID, long)}.
     * The key into the Map is created from the given parameters with the help
     * of {@link #awaitedStreamID(UUID, long)}.
     *
     * @param callbackId
     *      the id of the WebSocket callback that was used to trigger the stream operation on the service.
     * @param serial
     *      the serial number of the callback invocation.
     */
    private void unregisterAwaitedStream(final UUID callbackId, final long serial) {
        synchronized (awaitedStreams) {
            awaitedStreams.remove(awaitedStreamID(callbackId, serial));
        }
    }

    /**
     * Helper which creates a string id from the given parameters.
     *
     * @param callbackId
     *      the id of the WebSocket callback that was used to trigger the stream operation on the service.
     * @param serial
     *      the serial number of the callback invocation.
     * @return
     *      the newly created stream id.
     */
    private String awaitedStreamID(final UUID callbackId, final long serial) {
        return callbackId.toString() + "-" + serial;
    }

    /**
     * Returns the Future for the awaited stream with the given id.
     *
     * @param id
     *      the id of the awaited stream.
     * @return
     *      Future for the awaited stream, or null.
     */
    private CompletableFuture<Pipe> getAwaitedStream(final String id) {
        synchronized (awaitedStreams) {
            return awaitedStreams.get(id);
        }
    }

    /**
     * Handles an incoming GET request on an awaited stream sent with setStream.
     * Connects the waiting callback thread with this thread by piping the stream data
     * to it, using the Future that has been registered by the callback.
     *
     * @param address
     *      the address of the GET request, which must contain the stream id.
     * @param response
     *      the {@link HttpServletResponse} object for sending an answer.
     * @throws IOException
     *      if an IOException occurs while handling the stream.
     */
    private void handleAwaitedGETCallbackStream(
            final String address,
            final HttpServletResponse response
    ) throws IOException {
        // The request is from a service that wants to receive a stream as a response
        // to a virtual node callback. Check, if it is awaited and hand the request
        // to the future.
        final CompletableFuture<Pipe> future = getAwaitedStream(address.substring("/callback-stream/".length()));
        if (future == null) {
            response.sendError(HttpServletResponse.SC_GONE, "request was not awaited");
            return;
        }

        // Set the status and content type of the response.
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(VslMimeTypes.BINARY);

        // Create the pipe and hand it to the Thread waiting on the request.
        // Then, stream from the pipe into the response.
        final Pipe pipe = new Pipe();
        try (final OutputStream respStream = response.getOutputStream()) {
            future.complete(pipe);
            // Ensure the response is committed, which causes the headers to be sent,
            // preventing further alteration and ensuring the client does not think
            // its request timed out.
            respStream.flush();

            Stream.copy(pipe.getSource(), respStream);
        } finally {
            // Only close the pipe's source, as the sink is closed by the receiver
            // of the future.
            pipe.getSource().close();
        }
    }

    /**
     * Handles an incoming PUT request on an awaited stream requested with getStream.
     * Connects the waiting callback thread with this thread by piping the stream data
     * to it, using the Future that has been registered by the callback.
     *
     * @param address
     *      the address of the GET request, which must contain the stream id.
     * @param request
     *      the {@link HttpServletRequest} object.
     * @param response
     *      the {@link HttpServletResponse} object for sending an answer.
     * @throws IOException
     *      if an IOException occurs while handling the stream.
     */
    private void handleAwaitedPUTCallbackStream(
            final String address,
            final HttpServletRequest request,
            final HttpServletResponse response
    ) throws IOException {
        // The request is from a service that sends a stream as a response
        // to a virtual node callback. Check, if it is awaited and hand the stream
        // to the future.
        final CompletableFuture<Pipe> future = getAwaitedStream(address.substring("/callback-stream/".length()));
        if (future == null) {
            response.sendError(HttpServletResponse.SC_GONE, "request was not awaited");
            return;
        }

        // Create the pipe and hand it to the Thread waiting on the request.
        // Then, stream the request into the pipe.
        final Pipe pipe = new Pipe();
        try (final InputStream stream = request.getInputStream()) {
            future.complete(pipe);
            Stream.copy(stream, pipe.getSink());
        } finally {
            // Only close the pipe's sink, as the source is closed by the receiver
            // of the future.
            pipe.getSink().close();
        }

        // Set the status of the response.
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public final Collection<String> getWebsocketProtocols() {
        final Collection<String> protocols = new ArrayList<String>();
        protocols.add(RestTransportContext.WEBSOCKET_PROTOCOL_V1);

        return Collections.unmodifiableCollection(protocols);
    }

    @Override
    public final void websocketConnected(final CallbackSender websocketSender,
            final VslIdentity vslIdentity) {
        LOG.debug("{} connected via websocket.", vslIdentity.getClientId());
        synchronized (callbackSenderByClientId) {
            callbackSenderByClientId.put(vslIdentity.getClientId(), websocketSender);
        }
    }

    @Override
    public final void websocketClosed(final int statusCode, final String reason,
            final VslIdentity vslIdentity) {
        LOG.debug("Websocket connection to {} closed for reason {}: {}", vslIdentity.getClientId(),
                statusCode, reason);
        synchronized (callbackSenderByClientId) {
            callbackSenderByClientId.remove(vslIdentity.getClientId());
        }
    }

    /**
     * Create a callback of the specified type for internal references.
     *
     * @param callbackId
     *            the callback id supplied by the client.
     * @param vslIdentity
     *            the client's identity.
     * @param clazz
     *            the desired callback implementation (must be an interface).
     * @param <T>
     *            type-safe actual callback type.
     * @return the callback instance which can be used.
     * @throws VslException
     *             If the callback creation fails.
     */
    @SuppressWarnings("unchecked")
    final <T extends VslCallback> T createCallback(final UUID callbackId,
            final VslIdentity vslIdentity, final Class<T> clazz) throws VslException {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException(
                    "The callback to create must be one of the VslCallback interfaces.");
        }
        final T result;
        try {
            synchronized (callbackById) {
                if (callbackById.containsKey(callbackId)) {
                    result = (T) callbackById.get(callbackId);
                    // TODO: check client id!
                } else {
                    final Callback newInst = new Callback(callbackId, vslIdentity.getClientId());
                    callbackById.put(callbackId, newInst);
                    result = (T) newInst;
                }
            }
            // TODO: could be more precise with ClassCastException etc.
        } catch (final RuntimeException e) {
            throw new VslException("Callback class cannot be instantiated: " + clazz.getName(), e) {
                private static final long serialVersionUID = 4048600720567138464L;

                @Override
                public byte getErrorCodeMajor() {
                    return 5;
                }

                @Override
                public byte getErrorCodeMinor() {
                    return 1;
                }
            };
        }
        return result;
    }

    /**
     * Process a PUT request on the specified VSL address.
     *
     * @param address
     *            the VSL address.
     * @param request
     *            the {@link HttpServletRequest} object.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws VslException
     *             If a VslException occurs during VSL set operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    protected abstract void doPut(String address, HttpServletRequest request,
            HttpServletResponse response, VslIdentity vslIdentity) throws VslException, IOException;

    /**
     * Process a POST request on the specified VSL address.
     *
     * @param address
     *            the VSL address.
     * @param query
     *            Query parameters of the request.
     * @param request
     *            the {@link HttpServletRequest} object.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws VslException
     *             If a VslException occurs during VSL operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    protected abstract void doPost(String address, String query, HttpServletRequest request,
            HttpServletResponse response, VslIdentity vslIdentity) throws VslException, IOException;

    /**
     * Process an OPTIONS request on the specified VSL address.
     *
     * @param address
     *            the VSL address.
     * @param request
     *            the {@link HttpServletRequest} object.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    protected abstract void doOptions(String address, HttpServletRequest request,
            HttpServletResponse response, VslIdentity vslIdentity) throws IOException;

    /**
     * Process a request with a method other than GET, PUT, POST, OPTIONS on the specified VSL
     * address.
     *
     * @param address
     *            the VSL address.
     * @param method
     *            the method of this request.
     * @param request
     *            the {@link HttpServletRequest} object.
     * @param response
     *            the {@link HttpServletResponse} object for sending an answer.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    protected abstract void doOtherMethod(String address, String method, HttpServletRequest request,
            HttpServletResponse response, VslIdentity vslIdentity) throws IOException;

    /**
     * Inner class for a transport-generated callback.
     *
     * @author felix
     */
    protected final class Callback extends AbstractTransportCallback {

        /**
         * The client id of this callback's owner.
         */
        private final String clientId;

        /**
         * Create a new callback for the callback id. Only one callback should be created per
         * callback id and it should be unique among all client ids.
         *
         * @param callbackId
         *            the callback id.
         * @param clientId
         *            the client id.
         */
        private Callback(final UUID callbackId, final String clientId) {
            super(callbackId, getRestTransportContext().getConfig().getCallbackTimeout());

            this.clientId = clientId;
        }

        @Override
        public CallbackSender getCallbackSender() throws VslException {
            final CallbackSender sender;
            synchronized (callbackSenderByClientId) {
                sender = callbackSenderByClientId.get(clientId);
            }
            if (sender == null) {
                throw new CallbackUnreachableException(clientId);
            }

            return sender;
        }

        @Override
        public InputStream getStream(String address, VslIdentity identity) throws VslException {
            // The stream will be sent in a new HTTP request.
            // Therefore, register the callback id for the awaited stream.
            final long serial = getNextSerial();
            final CompletableFuture<Pipe> future = registerAwaitedStream(callbackId, serial);

            // Inform the service that it should send the stream.
            //LOG.debug("sending VGET_STREAM request for id {}", awaitedStreamID(callbackId, serial));
            invokeCallback(CallbackMethod.VGET_STREAM, address, serial, identity, null);

            // Wait here for the incoming request from the service.
            final int timeout = getRestTransportContext().getConfig().getCallbackTimeout();
            InputStream stream = null;
            try {
                stream = future.get(timeout, TimeUnit.MILLISECONDS).getSource();

                // Discard the first dummy byte.
                int dummy = stream.read();
                if (dummy == -1) {
                    // Stream was closed already.
                    throw new StreamClosedException("while reading dummy byte");
                } else if (dummy != 0) {
                    throw new UnexpectedErrorException(
                            "stream with id " + callbackId + "-" + serial + " had unexpected dummy byte " + dummy);
                }

                return stream;
            } catch (InterruptedException | ExecutionException | IOException e) {
                closeStream(stream);
                throw new UnexpectedErrorException(e.getMessage());
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException(
                        timeout, TimeUnit.MILLISECONDS, "awaited request for id " + awaitedStreamID(callbackId, serial));
            } finally {
                // Always clear up the awaited stream.
                unregisterAwaitedStream(callbackId, serial);
            }
        }

        @Override
        public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
            // When this method returns, the open request the stream is coming from will be closed!
            // Therefore, ensure the stream is completely written before returning.

            // The stream must be sent in a new HTTP request.
            // Therefore, register the callback id for the awaited request.
            final long serial = getNextSerial();
            final CompletableFuture<Pipe> future = registerAwaitedStream(callbackId, serial);

            // Inform the service that it should send a new request to receive the stream.
            invokeCallback(CallbackMethod.VSET_STREAM, address, serial, identity, null);

            // Wait here for the incoming request from the service.
            final int timeout = getRestTransportContext().getConfig().getCallbackTimeout();
            try (final OutputStream sink = future.get(timeout, TimeUnit.MILLISECONDS).getSink()) {
                Stream.copy(stream, sink);
            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new UnexpectedErrorException(e.getMessage());
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException(
                        timeout, TimeUnit.MILLISECONDS, "awaited request for id " + awaitedStreamID(callbackId, serial));
            } finally {
                // Always clear up the awaited stream.
                unregisterAwaitedStream(callbackId, serial);
            }
        }

        /**
         * Helper to close an {@link InputStream} and catch the potential {@link IOException}.
         *
         * @param stream
         *      the stream to be closed.
         */
        private void closeStream(final InputStream stream) {
            if (stream == null) {
                return;
            }

            try {
                stream.close();
            } catch (IOException e) {
                LOG.error("could not close stream: {}", e.getMessage());
            }
        }
    }
}
