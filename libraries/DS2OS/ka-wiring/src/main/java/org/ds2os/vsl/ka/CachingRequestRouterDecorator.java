package org.ds2os.vsl.ka;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decorator for the {@link RequestRouter} which utilizes caching.
 *
 * @author borchers
 * @author liebald
 */
public class CachingRequestRouterDecorator extends AbstractRequestRouter {

    /**
     * The decorated {@link VslRequestHandler} that is extended with caching.
     */
    private final VslRequestHandler requestHandler;

    /**
     * The {@link VslNodeCache} used to decorate the {@link VslRequestHandler}.
     */
    private final VslNodeCache cache;

    /**
     * Constructor for a RequestRouter ({@link VslRequestHandler}) decorated with caching.
     *
     * @param agentName
     *            the local agent name configuration.
     * @param requestHandler
     *            The decorated {@link VslRequestHandler} that is extended with caching.
     * @param cache
     *            The {@link VslNodeCache} used to decorate the {@link VslRequestHandler}.
     */
    public CachingRequestRouterDecorator(final VslAgentName agentName,
            final VslRequestHandler requestHandler, final VslNodeCache cache) {
        super(agentName);
        this.requestHandler = requestHandler;
        this.cache = cache;
    }

    @Override
    public final String registerService(final VslServiceManifest manifest,
            final VslIdentity identity) throws VslException {
        return requestHandler.registerService(manifest, identity);
    }

    @Override
    public final void unregisterService(final VslIdentity identity) throws VslException {
        requestHandler.unregisterService(identity);
    }

    @Override
    public final VslNode get(final String address, final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        VslNode result = cache.getCachedNode(resolvedAddress, identity);
        if (result != null) {
            return result;
        }
        // the normal request router resolves again, better use the orignal address when delegating
        // to him in order to avoid confusion.
        result = requestHandler.get(address, identity);
        cache.cacheNode(resolvedAddress, result);
        return result;
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        VslNode result = cache.getCachedNode(resolvedAddress, identity);
        if (result != null) {
            return result;
        }
        // the normal request router resolves again, better use the orignal address when delegating
        // to him in order to avoid confusion.
        result = requestHandler.get(address, params, identity);
        cache.cacheNode(resolvedAddress, result);
        return result;
    }

    @Override
    public final void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        cache.handleSet(resolvedAddress, knowledge);
        requestHandler.set(address, knowledge, identity);
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        return requestHandler.getStream(address, identity);
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        requestHandler.setStream(address, stream, identity);
    }

    @Override
    public final void notify(final String address, final VslIdentity identity) throws VslException {
        // TODO: this may not cover all notifications.
        cache.handleNotification(address);
        requestHandler.notify(address, identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        requestHandler.subscribe(address, subscriber, identity);
    }

    @Override
    public final void unsubscribe(final String address, final VslIdentity identity)
            throws VslException {
        requestHandler.unsubscribe(address, identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        requestHandler.subscribe(address, subscriber, params, identity);

    }

    @Override
    public final void unsubscribe(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        requestHandler.unsubscribe(address, params, identity);

    }

    @Override
    public final void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws VslException {
        requestHandler.lockSubtree(address, lockHandler, identity);
    }

    @Override
    public final void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        requestHandler.commitSubtree(address, identity);
    }

    @Override
    public final void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        requestHandler.rollbackSubtree(address, identity);
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        requestHandler.registerVirtualNode(address, virtualNodeHandler, identity);
    }

    @Override
    public final void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        requestHandler.unregisterVirtualNode(address, identity);
    }
}
