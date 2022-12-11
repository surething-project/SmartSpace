package org.ds2os.vsl.core.bridge;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Bridge which provide a {@link VslConnector} interface with a {@link VslRequestHandler}.
 *
 * @author borchers
 * @author felix
 */
public class RequestHandlerToConnectorBridge implements VslParametrizedConnector {

    /**
     * The injected request handler.
     */
    private final VslRequestHandler requestHandler;

    /**
     * The identity used for the requests.
     */
    private final VslIdentity identity;

    /**
     * The {@link VslNodeFactory} exposed by the connector.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * The VSL address where this connector is registered, if it is registered.
     */
    private String registeredAddress;

    /**
     * Constructor with injected request handler and identity.
     *
     * @param requestHandler
     *            the {@link VslRequestHandler} which is bridged.
     * @param identity
     *            the {@link VslIdentity} used for the requests.
     * @param nodeFactory
     *            the {@link VslNodeFactory} exposed by the connector.
     */
    public RequestHandlerToConnectorBridge(final VslRequestHandler requestHandler,
            final VslIdentity identity, final VslNodeFactory nodeFactory) {
        this.requestHandler = requestHandler;
        this.identity = identity;
        this.nodeFactory = nodeFactory;
        this.registeredAddress = "";
    }

    @Override
    public final VslNodeFactory getNodeFactory() {
        return nodeFactory;
    }

    @Override
    public final String getRegisteredAddress() {
        return registeredAddress;
    }

    @Override
    public final String registerService(final VslServiceManifest manifest) throws VslException {
        final String address = requestHandler.registerService(manifest, identity);
        registeredAddress = address;
        return address;
    }

    @Override
    public final void unregisterService() throws VslException {
        requestHandler.unregisterService(identity);
        registeredAddress = "";
    }

    @Override
    public final VslNode get(final String address) throws VslException {
        return requestHandler.get(address, identity);
    }

    @Override
    public final void set(final String address, final VslNode knowledge) throws VslException {
        requestHandler.set(address, knowledge, identity);
    }

    @Override
    public InputStream getStream(String address) throws VslException {
        return requestHandler.getStream(address, identity);
    }

    @Override
    public void setStream(String address, InputStream stream) throws VslException {
        requestHandler.setStream(address, stream, identity);
    }

    @Override
    public final void notify(final String address) throws VslException {
        requestHandler.notify(address, identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber)
            throws VslException {
        requestHandler.subscribe(address, subscriber, identity);
    }

    @Override
    public final void unsubscribe(final String address) throws VslException {
        requestHandler.unsubscribe(address, identity);
    }

    @Override
    public final void lockSubtree(final String address, final VslLockHandler lockHandler)
            throws VslException {
        requestHandler.lockSubtree(address, lockHandler, identity);
    }

    @Override
    public final void commitSubtree(final String address) throws VslException {
        requestHandler.commitSubtree(address, identity);
    }

    @Override
    public final void rollbackSubtree(final String address) throws VslException {
        requestHandler.rollbackSubtree(address, identity);
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler) throws VslException {
        requestHandler.registerVirtualNode(address, virtualNodeHandler, identity);
    }

    @Override
    public final void unregisterVirtualNode(final String address) throws VslException {
        requestHandler.unregisterVirtualNode(address, identity);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params)
            throws VslException {
        return requestHandler.get(address, params, identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params) throws VslException {
        requestHandler.subscribe(address, subscriber, params, identity);
    }

    @Override
    public final void unsubscribe(final String address, final VslAddressParameters params)
            throws VslException {
        requestHandler.unsubscribe(address, params, identity);
    }
}
