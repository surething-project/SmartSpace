package org.ds2os.vsl.rest;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.transport.PostOperation.OperationType;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.Base64;
import org.ds2os.vsl.core.utils.Stream;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.AlreadyLockedException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.client.AbstractRestConnector;
import org.ds2os.vsl.rest.client.RestTransportClient;
import org.ds2os.vsl.rest.client.RestTransportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * {@link VslRequestHandler} implementation for the REST transport.
 *
 * @see org.ds2os.vsl.rest.client.RestConnector
 * @author borchers
 * @author felix
 */
public final class RestKAToKAConnector extends AbstractRestConnector implements VslRequestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestKAToKAConnector.class);

    /**
     * The REST client to use.
     */
    private final RestTransportClient client;

    /**
     * Create a new {@link VslRequestHandler} from a {@link RestTransportClient}.
     *
     * @param client
     *            the {@link RestTransportClient}.
     * @param baseURL
     *            the base URL of the KA.
     * @param helper
     *            the REST transport helper.
     */
    public RestKAToKAConnector(final RestTransportClient client, final String baseURL,
            final RestTransportContext helper) {
        super(baseURL, helper);
        this.client = client;
    }

    /**
     * Identity the request with another identity for KA to KA communication.
     *
     * @param request
     *            the request where the authorization is added.
     * @param identity
     *            the identity to authorize.
     */
    private void identifyAs(final RestTransportRequest request, final VslIdentity identity) {
        if (identity == null) {
            return;
        }
        final String authorization;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            getRestTransportContext().getMapper(VslMimeTypes.JSON).writeValue(output, identity);
            authorization = "VSL " + Base64.encode(output.toByteArray());
        } catch (final IOException e) {
            // should not happen
            return;
        }
        request.setHeader("Authorization", authorization);
    }

    @Override
    public String registerService(final VslServiceManifest manifest, final VslIdentity identity)
            throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("service/register"));
        request.accept(getRestTransportContext().getContentTypePreference());
        identifyAs(request, identity);
        try {
            sendWithBody(request, manifest);

            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }

            return readResponse(request, httpCode, String.class);
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public void unregisterService(final VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("service/unregister"));
        identifyAs(request, identity);
        try {
            sendWithBody(request, new Object());
            final int httpCode = request.syncRequest();
            if (httpCode < 200 || httpCode >= 300) {
                throw toVslException(httpCode, request);
            }
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public VslNode get(final String address, final VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doGet(getURI(address));
        request.accept(getRestTransportContext().getContentTypePreference());
        identifyAs(request, identity);
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
    public VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doGet(getURI(address, params));
        request.accept(getRestTransportContext().getContentTypePreference());
        identifyAs(request, identity);
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
    public void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {
        final RestTransportRequest request = client.doPut(getURI(address));
        request.accept(getRestTransportContext().getContentTypePreference());
        identifyAs(request, identity);
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
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doGet(getURI(address));
        identifyAs(request, identity);
        request.accept(VslMimeTypes.BINARY);
        request.send();

        // Wait, until the response arrives, check its code and then
        // use the response stream as return value.
        final int httpCode = request.syncRequest();
        if (httpCode != HttpServletResponse.SC_OK) {
            throw toVslException(httpCode, request);
        }

        return request.getResponseStream();
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doPut(getURI(address));
        identifyAs(request, identity);

        try (final OutputStream reqStream = request.getRequestStream(VslMimeTypes.BINARY)) {
            request.send();
            Stream.copy(stream, reqStream);
        } catch (IOException e) {
            throw toVslException(e);
        }

        // Wait until the response arrived.
        final int httpCode = request.syncRequest();
        if (httpCode < 200 || httpCode >= 300) {
            throw toVslException(httpCode, request);
        }
    }

    /**
     * Helper to post a {@link PostOperation} on the node of the address.
     *
     * @param address
     *            the address.
     * @param operation
     *            the {@link PostOperation}.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If an exception occurs.
     */
    private void doPost(final String address, final PostOperation operation,
            final VslIdentity identity) throws VslException {
        doPost(address, new AddressParameters(), operation, identity);
    }

    /**
     * Helper to post a {@link PostOperation} on the node of the address.
     *
     * @param address
     *            the address.
     * @param operation
     *            the {@link PostOperation}.
     * @param params
     *            The parameters of the query.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If an exception occurs.
     */
    private void doPost(final String address, final VslAddressParameters params,
            final PostOperation operation, final VslIdentity identity) throws VslException {
        final RestTransportRequest request = client.doPost(getURI(address, params));
        request.accept(getRestTransportContext().getContentTypePreference());
        identifyAs(request, identity);
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

    @Override
    public void notify(final String address, final VslIdentity identity) throws VslException {
        doPost(address, new PostOperation(OperationType.NOTIFY), identity);
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), subscriber);
        doPost(address, new PostOperation(OperationType.SUBSCRIBE, callbackUUID), identity);
    }

    @Override
    public void unsubscribe(final String address, final VslIdentity identity) throws VslException {
        doPost(address, new PostOperation(OperationType.UNSUBSCRIBE), identity);
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), subscriber);
        doPost(address, params, new PostOperation(OperationType.SUBSCRIBE, callbackUUID), identity);
    }

    @Override
    public void unsubscribe(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        doPost(address, params, new PostOperation(OperationType.UNSUBSCRIBE), identity);
    }

    @Override
    public void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws AlreadyLockedException, VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), lockHandler);
        doPost(address, new PostOperation(OperationType.LOCK_SUBTREE, callbackUUID), identity);
    }

    @Override
    public void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        doPost(address, new PostOperation(OperationType.COMMIT_SUBTREE), identity);
    }

    @Override
    public void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        doPost(address, new PostOperation(OperationType.ROLLBACK_SUBTREE), identity);
    }

    @Override
    public void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        final UUID callbackUUID = client.registerCallback(getCallbacksURI(), virtualNodeHandler);
        doPost(address, new PostOperation(OperationType.REGISTER_VIRTUAL_NODE, callbackUUID),
                identity);
    }

    @Override
    public void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        doPost(address, new PostOperation(OperationType.UNREGISTER_VIRTUAL_NODE), identity);
    }

}
