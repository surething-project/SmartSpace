package org.ds2os.vsl.rest.client;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.transport.PostOperation.OperationType;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.Stream;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.jetty.client.WebSocketClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

/**
 * Generic REST connector operating on a {@link RestTransportClient}.
 *
 * @author borchers
 * @author felix
 */
public final class RestConnector extends AbstractRestConnector
        implements VslParametrizedConnector, WebSocketClientHandler {

    /**
     * The logger instance of this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RestConnector.class);

    /**
     * The used {@link RestTransportClient}.
     */
    private final RestTransportClient client;

    /**
     * The {@link VslNodeFactory} used by this connector.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * The VSL address where this connector is registered, if it is registered.
     */
    private String registeredAddress;

    /**
     * Create a new {@link VslConnector} from a {@link RestTransportClient}.
     *
     * @param client
     *            the {@link RestTransportClient}.
     * @param baseURL
     *            the base URL of the KA.
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     * @param nodeFactory
     *            the {@link VslNodeFactory} used by this connector.
     */
    public RestConnector(final RestTransportClient client, final String baseURL,
            final RestTransportContext context, final VslNodeFactory nodeFactory) {
        super(baseURL, context);
        this.client = client;
        this.nodeFactory = nodeFactory;
        this.registeredAddress = "";
    }

    @Override
    public VslNodeFactory getNodeFactory() {
        return nodeFactory;
    }

    @Override
    public String getRegisteredAddress() {
        return registeredAddress;
    }

    @Override
    public String registerService(final VslServiceManifest manifest) throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("service/register"));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            sendWithBody(request, manifest);

            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }

            final String address = readResponse(request, httpCode, String.class);
            registeredAddress = address;
            return address;
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public void unregisterService() throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("service/unregister"));
        try {
            sendWithBody(request, new Object());
            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }
            registeredAddress = "";
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public VslNode get(final String address, final VslAddressParameters params)
            throws VslException {
        final RestTransportRequest request = client.doGet(getURI(address, params));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            request.send();

            final int httpCode = request.syncRequest();
            if (httpCode != 200) {
                throw toVslException(httpCode, request);
            }

            return readResponse(request, httpCode, VslNode.class);
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public void set(final String address, final VslNode knowledge) throws VslException {
        final RestTransportRequest request = client.doPut(getURI(address));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            sendWithBody(request, knowledge);

            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public InputStream getStream(final String address) throws VslException {
        return getStream(getURI(address));
    }

    @Override
    public InputStream getStreamFromCallback(final UUID callbackId, final long serial) throws VslException {
        return getStream(getOperationURI("callback-stream/" + callbackId + "-" + serial));
    }

    /**
     * Internal helper method that implements the getStream functionality.
     * It requests the stream from the given node at the given uri and returns the handle to it.
     *
     * @param uri
     *      the uri to request the stream from.
     * @return
     *      the {@link InputStream} handle.
     * @throws VslException
     *      if a VslException occurs.
     */
    private InputStream getStream(final URI uri) throws VslException {
        final RestTransportRequest request = client.doGet(uri);
        request.accept(VslMimeTypes.BINARY);
        request.send();

        final int httpCode = request.syncRequest();
        if (httpCode != 200) {
            throw toVslException(httpCode, request);
        }

        return request.getResponseStream();
    }

    @Override
    public void setStream(final String address, final InputStream stream) throws VslException {
        setStream(getURI(address), stream);
    }

    @Override
    public void setStreamForCallback(
            final InputStream stream,
            final UUID callbackId,
            final long serial
    ) throws VslException {
        setStream(getOperationURI("callback-stream/" + callbackId.toString() + "-" + serial), stream);
    }

    /**
     * Internal helper method that implements the setStream functionality.
     * It starts a new thread, which handles the copying of the stream data from the
     * provided {@link InputStream} to the request. The given stream is also closed.
     *
     * @param uri
     *      the uri to send the request to.
     * @param stream
     *      the {@link InputStream} handle which provides the data to be sent.
     * @throws VslException
     *      If an VslException occurs.
     */
    private void setStream(final URI uri, final InputStream stream) throws VslException {
        final RestTransportRequest request = client.doPut(uri);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try (final OutputStream reqStream = request.getRequestStream(VslMimeTypes.BINARY)) {
                        request.send();

                        // Ensure the headers are sent right away, so that the receiver does not trigger a timeout.
                        // Unfortunately, this seems only possible right now if actual data is sent.
                        // Since we do not know when data is available through the given InputStream, we send
                        // ourselves one byte of data over the wire, that will be discarded at the destination.
                        reqStream.write(0);

                        // Now copy the input stream to the request stream.
                        Stream.copy(stream, reqStream);
                    } finally {
                        // Ensure to close the stream.
                        stream.close();
                    }

                    // Wait until the response arrived.
                    final int httpCode = request.syncRequest();
                    if (httpCode < 200 || httpCode >= 300) {
                        throw toVslException(httpCode, request);
                    }
                } catch (IOException e) {
                    LOG.error("Unexpected IOException: {}", e.getMessage());
                } catch (VslException e) {
                    LOG.error("VSLException: {}", e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Helper to post a {@link PostOperation} on the node of the address.
     *
     * @param address
     *            the address.
     * @param operation
     *            the {@link PostOperation}.
     * @param params
     *            Parameters of the operation.
     * @throws VslException
     *             If an exception occurs.
     */
    private void doPost(final String address, final PostOperation operation,
            final VslAddressParameters params) throws VslException {
        final RestTransportRequest request = client.doPost(getURI(address, params));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            sendWithBody(request, operation);

            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    /**
     * Helper to post a {@link PostOperation} on the node of the address.
     *
     * @param address
     *            the address.
     * @param operation
     *            the {@link PostOperation}.
     * @throws VslException
     *             If an exception occurs.
     */
    private void doPost(final String address, final PostOperation operation) throws VslException {
        doPost(address, operation, new AddressParameters());
    }

    @Override
    public void notify(final String address) throws VslException {
        doPost(address, new PostOperation(OperationType.NOTIFY));
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params) throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), subscriber);
        doPost(address, new PostOperation(OperationType.SUBSCRIBE, callbackUUID), params);
    }

    @Override
    public void unsubscribe(final String address, final VslAddressParameters params)
            throws VslException {
        doPost(address, new PostOperation(OperationType.UNSUBSCRIBE), params);
    }

    @Override
    public void lockSubtree(final String address, final VslLockHandler lockHandler)
            throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), lockHandler);
        doPost(address, new PostOperation(OperationType.LOCK_SUBTREE, callbackUUID));
    }

    @Override
    public void commitSubtree(final String address) throws VslException {
        doPost(address, new PostOperation(OperationType.COMMIT_SUBTREE));
    }

    @Override
    public void rollbackSubtree(final String address) throws VslException {
        doPost(address, new PostOperation(OperationType.ROLLBACK_SUBTREE));
    }

    @Override
    public void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler) throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), virtualNodeHandler);
        doPost(address, new PostOperation(OperationType.REGISTER_VIRTUAL_NODE, callbackUUID));
    }

    @Override
    public void unregisterVirtualNode(final String address) throws VslException {
        doPost(address, new PostOperation(OperationType.UNREGISTER_VIRTUAL_NODE));
    }

    @Override
    public VslNode get(final String address) throws VslException {
        return get(address, new AddressParameters());
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber)
            throws VslException {
        subscribe(address, subscriber, new AddressParameters());
    }

    @Override
    public void unsubscribe(final String address) throws VslException {
        unsubscribe(address, new AddressParameters());
    }
}
