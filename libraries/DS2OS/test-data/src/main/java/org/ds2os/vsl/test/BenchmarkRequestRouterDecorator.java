package org.ds2os.vsl.test;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;

/**
 * Decorator for an {@link AbstractRequestRouter} to add special /benchmark/ nodes per agent.
 *
 * @author felix
 */
public class BenchmarkRequestRouterDecorator extends AbstractRequestRouter {

    /**
     * The decorated {@link VslRequestHandler} instance.
     */
    private final VslRequestHandler decorated;

    /**
     * The {@link VslRequestHandler} for the /benchmark/ routes.
     */
    private final VslRequestHandler benchmarkHandler;

    /**
     * Create a new {@link BenchmarkRequestRouterDecorator} based on any
     * {@link AbstractRequestRouter}.
     *
     * @param requestRouter
     *            the request router to decorate.
     */
    public BenchmarkRequestRouterDecorator(final AbstractRequestRouter requestRouter) {
        super(requestRouter.getAgentName());
        decorated = requestRouter;
        benchmarkHandler = new BenchmarkRequestHandler();
    }

    /**
     * Resolves the address relative to the local agent's /benchmark/ route (or global /benchmark/,
     * which is also executed on the local agent). If the address is not pointing to the local
     * benchmark route, this method returns empty string.
     *
     * @param address
     *            the node address in the VSL.
     * @param identity
     *            the identity of the issuer of this operation.
     * @return the relative benchmark path or empty string.
     * @throws NodeNotExistingException
     *             If the address is not resolvable.
     */
    protected final String resolveBenchmark(final String address, final VslIdentity identity)
            throws NodeNotExistingException {
        String benchmarkRoute = "";

        final String resolvedAddress = resolveAddress(address, identity);
        if (resolvedAddress.startsWith(BenchmarkRequestHandler.BENCHMARK_ROUTE)) {
            benchmarkRoute = resolvedAddress
                    .substring(BenchmarkRequestHandler.BENCHMARK_ROUTE.length());
        }

        final String agentId = extractAgentId(resolvedAddress);
        final String agentPath = "/" + agentId + BenchmarkRequestHandler.BENCHMARK_ROUTE;
        if (getAgentId().equals(agentId) && resolvedAddress.startsWith(agentPath)) {
            benchmarkRoute = resolvedAddress.substring(agentPath.length());
        }

        return benchmarkRoute;
    }

    @Override
    public final String registerService(final VslServiceManifest manifest,
            final VslIdentity identity) throws VslException {
        return decorated.registerService(manifest, identity);
    }

    @Override
    public final void unregisterService(final VslIdentity identity) throws VslException {
        decorated.unregisterService(identity);
    }

    @Override
    public final VslNode get(final String address, final VslIdentity identity) throws VslException {
        return get(address, new AddressParameters(), identity);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            return decorated.get(address, params, identity);
        } else {
            return benchmarkHandler.get(benchmarkAddress, params, identity);
        }
    }

    @Override
    public final void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.set(address, knowledge, identity);
        } else {
            benchmarkHandler.set(benchmarkAddress, knowledge, identity);
        }
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            return decorated.getStream(address, identity);
        } else {
            return benchmarkHandler.getStream(benchmarkAddress, identity);
        }
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.setStream(address, stream, identity);
        } else {
            benchmarkHandler.setStream(benchmarkAddress, stream, identity);
        }
    }

    @Override
    public final void notify(final String address, final VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.notify(address, identity);
        } else {
            benchmarkHandler.notify(benchmarkAddress, identity);
        }
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        subscribe(address, subscriber, new AddressParameters(), identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.subscribe(address, subscriber, params, identity);
        } else {
            benchmarkHandler.subscribe(benchmarkAddress, subscriber, params, identity);
        }
    }

    @Override
    public final void unsubscribe(final String address, final VslIdentity identity)
            throws VslException {
        unsubscribe(address, new AddressParameters(), identity);
    }

    @Override
    public final void unsubscribe(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.unsubscribe(address, params, identity);
        } else {
            benchmarkHandler.unsubscribe(benchmarkAddress, params, identity);
        }
    }

    @Override
    public final void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.lockSubtree(address, lockHandler, identity);
        } else {
            benchmarkHandler.lockSubtree(benchmarkAddress, lockHandler, identity);
        }
    }

    @Override
    public final void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.commitSubtree(address, identity);
        } else {
            benchmarkHandler.commitSubtree(benchmarkAddress, identity);
        }
    }

    @Override
    public final void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.rollbackSubtree(address, identity);
        } else {
            benchmarkHandler.rollbackSubtree(benchmarkAddress, identity);
        }
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.registerVirtualNode(address, virtualNodeHandler, identity);
        } else {
            benchmarkHandler.registerVirtualNode(benchmarkAddress, virtualNodeHandler, identity);
        }
    }

    @Override
    public final void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        final String benchmarkAddress = resolveBenchmark(address, identity);
        if (benchmarkAddress.isEmpty()) {
            decorated.unregisterVirtualNode(address, identity);
        } else {
            benchmarkHandler.unregisterVirtualNode(benchmarkAddress, identity);
        }
    }
}
