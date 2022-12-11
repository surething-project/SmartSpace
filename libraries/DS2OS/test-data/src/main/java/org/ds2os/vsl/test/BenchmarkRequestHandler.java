package org.ds2os.vsl.test;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.ka.SubscriptionManager;

import java.io.InputStream;

/**
 * {@link VslRequestHandler} which handles requests to the local /benchmark/ routes in memory. It
 * requires all addresses to be relative to /benchmark/ without leading /.
 *
 * @author felix
 */
public class BenchmarkRequestHandler implements VslRequestHandler {

    /**
     * The special benchmark route.
     */
    public static final String BENCHMARK_ROUTE = "/benchmark/";

    /**
     * Dedicated {@link SubscriptionManager} for the benchmark (without remote subscriptions or
     * knowledge checks from KOR).
     */
    private final VslSubscriptionManager benchmarkSubscriptionManager = new SubscriptionManager();

    /**
     * Default constructor.
     */
    public BenchmarkRequestHandler() {
        // HACK: needed this way to start notification thread
        benchmarkSubscriptionManager.activate(null, null, null);
    }

    /**
     * Switch between simple and big nodes.
     *
     * @param address
     *            the benchmark address.
     * @return true iff it is a big node.
     * @throws NodeNotExistingException
     *             If none of the two variants are used.
     */
    protected final boolean isBig(final String address) throws NodeNotExistingException {
        if ("simple".equals(address)) {
            return false;
        } else if ("big".equals(address)) {
            return true;
        } else {
            throw new NodeNotExistingException("Node with address " + address
                    + " does not exist in the BenchmarkRequestHandler");
        }
    }

    @Override
    public final String registerService(final VslServiceManifest manifest,
            final VslIdentity identity) throws VslException {
        throw new InvalidOperationException(
                "No service registration possible on the BenchmarkRequestHandler");
    }

    @Override
    public final void unregisterService(final VslIdentity identity) throws VslException {
        throw new InvalidOperationException(
                "No service registration possible on the BenchmarkRequestHandler");
    }

    @Override
    public final VslNode get(final String address, final VslIdentity identity) throws VslException {
        return get(address, new AddressParameters(), identity);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        if (isBig(address)) {
            if (params.getDepth() == 0) {
                switch (params.getNodeInformationScope()) {
                case COMPLETE:
                    return TestNodes.BIG_NODE;
                case METADATA:
                    return TestNodes.METADATA_NODE;
                case VALUE:
                default:
                    return TestNodes.BIG_DATA_NODE;
                }
            } else {
                switch (params.getNodeInformationScope()) {
                case COMPLETE:
                    return TestNodes.BIG_STRUCTURE;
                case METADATA:
                    return TestNodes.METADATA_STRUCTURE;
                case VALUE:
                default:
                    return TestNodes.BIG_DATA_STRUCTURE;
                }
            }
        } else {
            if (params.getDepth() == 0) {
                switch (params.getNodeInformationScope()) {
                case COMPLETE:
                    return TestNodes.SIMPLE_NODE;
                case METADATA:
                    return TestNodes.SIMPLE_METADATA_NODE;
                case VALUE:
                default:
                    return TestNodes.SIMPLE_DATA_NODE;
                }
            } else {
                switch (params.getNodeInformationScope()) {
                case COMPLETE:
                    return TestNodes.SIMPLE_STRUCTURE;
                case METADATA:
                    return TestNodes.SIMPLE_METADATA_STRUCTURE;
                case VALUE:
                default:
                    return TestNodes.SIMPLE_DATA_STRUCTURE;
                }
            }
        }
    }

    @Override
    public final void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {
        // isBig is just there to check node existence
        isBig(address);

        // do not do anything with knowledge (behave like /dev/null), but trigger subscriptions
        notify(address, identity);
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        throw new InvalidOperationException("getStream not implemented");
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        throw new InvalidOperationException("setStream not implemented");
    }

    @Override
    public final void notify(final String address, final VslIdentity identity) throws VslException {
        benchmarkSubscriptionManager.notifySubscribers(BENCHMARK_ROUTE + address);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        subscribe(address, subscriber, new AddressParameters(), identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        benchmarkSubscriptionManager.addSubscription(BENCHMARK_ROUTE + address, subscriber, params,
                identity, get(address, params, identity));
    }

    @Override
    public final void unsubscribe(final String address, final VslIdentity identity)
            throws VslException {
        unsubscribe(address, new AddressParameters(), identity);
    }

    @Override
    public final void unsubscribe(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        // isBig is just there to check node existence
        isBig(address);

        benchmarkSubscriptionManager.removeSubscription(BENCHMARK_ROUTE + address, params,
                identity);
    }

    @Override
    public final void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws VslException {
        // locking is ignored on the benchmark routes
    }

    @Override
    public final void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        // locking is ignored on the benchmark routes
    }

    @Override
    public final void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        // locking is ignored on the benchmark routes
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        throw new InvalidOperationException(
                BENCHMARK_ROUTE + " routes do not support virtual nodes");
    }

    @Override
    public final void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        throw new InvalidOperationException(
                BENCHMARK_ROUTE + " routes do not support virtual nodes");
    }
}
