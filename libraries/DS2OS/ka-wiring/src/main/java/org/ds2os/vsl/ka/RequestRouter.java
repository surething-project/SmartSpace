package org.ds2os.vsl.ka;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.config.VslAnomalyDetectionConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeNotLockedException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * {@link VslRequestHandler} which routes requests to remote KAs or the local KA depending on the
 * address.
 *
 * @author borchers
 * @author felix
 */
public final class RequestRouter extends AbstractRequestRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestRouter.class);

    /**
     * The {@link VslRequestHandler} of the local KOR for local requests.
     */
    private final VslRequestHandler localKOR;

    /**
     * The {@link VslTransportManager} for retrieving transports to remote KAs.
     */
    private final VslTransportManager transportManager;

    /**
     * The {@link VslAgentRegistryService} for information about other connected agents.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * The {@link VslSubscriptionManager} for subscriptions.
     */
    private final VslSubscriptionManager subscriptionManager;

    /*
     * This class does not only route the accesses, it also allows to monitor them using the class
     * AccessObserver. The descriptions of the accesses can be saved or send to the sphinx to block
     * them.
     */
    /**
     * The observer to monitor the accesses.
     */
    private final AccessObserver anomalyDetectionService;

    /**
     * The {@link VslAnomalyDetectionConfig} for anomaly detection configuration.
     */
    private final VslAnomalyDetectionConfig anomalyDetectionConfig;

    /**
     * Create a new {@link RequestRouter} instance.
     *
     * @param agentName
     *            the local agent name configuration.
     * @param localKOR
     *            the {@link VslRequestHandler} of the local KOR for local requests.
     * @param transportManager
     *            the {@link VslTransportManager} for retrieving transports to remote KAs.
     * @param agentRegistryService
     *            the {@link VslAgentRegistryService} for information about other connected agents.
     * @param subscriptionManager
     *            the {@link VslSubscriptionManager} for subscriptions.
     * @param anomalyDetectionConfig
     *            the {@link VslAnomalyDetectionConfig} for anomaly detection configuration.
     */
    public RequestRouter(final VslAgentName agentName, final VslRequestHandler localKOR,
            final VslTransportManager transportManager,
            final VslAgentRegistryService agentRegistryService,
            final VslSubscriptionManager subscriptionManager,
            final VslAnomalyDetectionConfig anomalyDetectionConfig) {
        super(agentName);
        this.localKOR = localKOR;
        this.transportManager = transportManager;
        this.agentRegistry = agentRegistryService;
        this.subscriptionManager = subscriptionManager;
        this.anomalyDetectionConfig = anomalyDetectionConfig;
        anomalyDetectionService = new AccessObserver(getAgentId(), this);
    }

    /**
     * Get a {@link VslRequestHandler} to a remote agent from the transport manager.
     *
     * @param agentId
     *            the agent id of the remote agent.
     * @return the {@link VslRequestHandler} to this remote agent.F
     * @throws VslException
     *             If the remote agent id is unknown or unreachable.
     */
    private VslRequestHandler getRemoteRequestHandler(final String agentId) throws VslException {
        final Collection<VslTransportConnector> transports = agentRegistry.getTransports(agentId);
        final String[] urls = new String[transports.size()];
        int i = 0;
        for (final VslTransportConnector connector : transports) {
            urls[i] = connector.getURL();
            i++;
        }
        final VslRequestHandler requestHandler = transportManager.getTransportToKA(urls);
        if (requestHandler == null) {
            // TODO: real exception
            throw new VslException(
                    "Could not get remote KA " + agentId + " for urls: " + Arrays.toString(urls)) {
                private static final long serialVersionUID = -5818882271673327778L;

                @Override
                public byte getErrorCodeMajor() {
                    return 5;
                }

                @Override
                public byte getErrorCodeMinor() {
                    return 3;
                }
            };
        } else {
            return requestHandler;
        }
    }

    @Override
    public String registerService(final VslServiceManifest manifest, final VslIdentity identity)
            throws VslException {
        // calling the logger of the Sphinx
        logDifferentCalls(identity, "registerService",
                "/" + getAgentId() + "/" + identity.getClientId());

        return localKOR.registerService(manifest, identity);
    }

    @Override
    public void unregisterService(final VslIdentity identity) throws VslException {

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "unregisterService",
                "/" + getAgentId() + "/" + identity.getClientId());

        localKOR.unregisterService(identity);
    }

    @Override
    public VslNode get(final String address, final VslIdentity identity) throws VslException {
        return get(address, new AddressParameters(), identity);
    }

    @Override
    public VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        final VslNode node;
        // LOGGER.debug("address: {}, resolvedAddress: {}, agentID: {}",address,resolvedAddress,
        // agentId);
        // get on root node is handled by local ka)

        if (address.equals("/") || getAgentId().equals(agentId)) {
            node = localKOR.get(resolvedAddress, params, identity);
        } else if (agentId.isEmpty()) {
            // redundant with address=="/"?
            throw new NoPermissionException("Cannot get /.");
        } else {
            // TODO: Access check!
            final VslRequestHandler remoteKA = getRemoteRequestHandler(agentId);
            node = remoteKA.get(resolvedAddress, params, identity);
        }

        // calling the logger of the Sphinx
        logAccess(identity, "read", resolvedAddress, node);

        return node;
    }

    @Override
    public void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logAccess(identity, "write", resolvedAddress, knowledge);

        if (getAgentId().equals(agentId)) {
            localKOR.set(resolvedAddress, knowledge, identity);
        } else if (agentId.isEmpty()) {
            throw new NoPermissionException("Cannot set /.");
        } else {
            // TODO: Access check!
            final VslRequestHandler remoteKA = getRemoteRequestHandler(agentId);
            remoteKA.set(resolvedAddress, knowledge, identity);
        }
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        final InputStream stream;

        if (address.equals("/") || getAgentId().equals(agentId)) {
            stream = localKOR.getStream(resolvedAddress, identity);
        } else if (agentId.isEmpty()) {
            // redundant with address=="/"?
            throw new NoPermissionException("Cannot get /.");
        } else {
            // TODO: Access check!
            stream = getRemoteRequestHandler(agentId).getStream(resolvedAddress, identity);
        }

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "getStream", resolvedAddress);

        return stream;
    }


    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "setStream", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.setStream(resolvedAddress, stream, identity);
        } else if (agentId.isEmpty()) {
            throw new NoPermissionException("Cannot setStream /.");
        } else {
            // TODO: Access check!
            getRemoteRequestHandler(agentId).setStream(address,stream,identity);
        }
    }

    @Override
    public void notify(final String address, final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        if (anomalyDetectionConfig.isAnomalyDetectionEnabled()) {
            // calling the logger of the Sphinx
            boolean allowance = anomalyDetectionService.logCallBack(resolvedAddress);
            if (!allowance && anomalyDetectionConfig.isBlockOnAnomaly()) {
                throw new NoPermissionException("Access denied by Anomaly detection");
            }
        }

        if (getAgentId().equals(agentId)) {
            localKOR.notify(resolvedAddress, identity);
        } else {
            // TODO: dispatch notifications with this method?
            throw new NoPermissionException(
                    "Notify can only be called locally on the service's own tree.");
        }

    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        subscribe(address, subscriber, new AddressParameters(), identity);
    }

    @Override
    public void unsubscribe(final String address, final VslIdentity identity) throws VslException {
        unsubscribe(address, new AddressParameters(), identity);
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {

        // calling the logger of the Sphinx
        logSubscription(identity, "subscribe", resolveAddress(address, identity),
                params.toString());

        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);
        if (getAgentId().equals(agentId)) {
            localKOR.subscribe(resolvedAddress, subscriber, params, identity);
        } else if (subscriptionManager.isHandlingRemoteSubscriptions()) {
            // TODO: check access rights.
            final VslNode affectedNodes = get(resolvedAddress, params, identity);
            subscriptionManager.addSubscription(resolvedAddress, subscriber, params, identity,
                    affectedNodes);
        } else {
            throw new InvalidOperationException("Remote subscriptions are not enabled");
        }

    }

    @Override
    public void unsubscribe(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logSubscription(identity, "unsubscribe", resolveAddress(address, identity),
                params.toString());

        if (getAgentId().equals(agentId)) {
            localKOR.unsubscribe(resolvedAddress, params, identity);
        } else if (subscriptionManager.isHandlingRemoteSubscriptions()) {
            subscriptionManager.removeSubscription(resolvedAddress, params, identity);
        } else {
            throw new InvalidOperationException("Remote subscriptions are not enabled");
        }

    }

    @Override
    public void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "lockSubtree", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.lockSubtree(resolvedAddress, lockHandler, identity);
        } else if (agentId.isEmpty()) {
            throw new NoPermissionException("Cannot lock /.");
        } else {
            // TODO: Access check!
            final VslRequestHandler remoteKA = getRemoteRequestHandler(agentId);
            remoteKA.lockSubtree(resolvedAddress, lockHandler, identity);
        }
    }

    @Override
    public void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "commitSubtree", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.commitSubtree(resolvedAddress, identity);
        } else if (agentId.isEmpty()) {
            throw new NodeNotLockedException("Cannot commit /.");
        } else {
            // TODO: Access check!
            final VslRequestHandler remoteKA = getRemoteRequestHandler(agentId);
            remoteKA.commitSubtree(resolvedAddress, identity);
        }
    }

    @Override
    public void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "rollbackSubtree", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.rollbackSubtree(resolvedAddress, identity);
        } else if (agentId.isEmpty()) {
            throw new NodeNotLockedException("Cannot commit /.");
        } else {
            // TODO: Access check!
            final VslRequestHandler remoteKA = getRemoteRequestHandler(agentId);
            remoteKA.rollbackSubtree(resolvedAddress, identity);
        }
    }

    @Override
    public void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "registerVirtualNode", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.registerVirtualNode(resolvedAddress, virtualNodeHandler, identity);
        } else {
            throw new NoPermissionException(
                    "registerVirtualNode can only be called locally on the service's own tree.");
        }
    }

    @Override
    public void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        final String resolvedAddress = resolveAddress(address, identity);
        final String agentId = extractAgentId(resolvedAddress);

        // calling the logger of the Sphinx
        logDifferentCalls(identity, "unregisterVirtualNode", resolvedAddress);

        if (getAgentId().equals(agentId)) {
            localKOR.unregisterVirtualNode(resolvedAddress, identity);
        } else {
            throw new NoPermissionException(
                    "unregisterVirtualNode can only be called locally on the service's own tree.");
        }
    }

    /**
     * Tests various operations for anomalies. Throws an {@link NoPermissionException} if the
     * operation is anomalous and should be blocked.
     *
     * @param identity
     *            The {@link VslIdentity} used for the operation.
     * @param operation
     *            The operation to test.
     * @param address
     *            The address the operation operates on.
     * @throws NoPermissionException
     *             Exception thrown when the operation is anomalous
     */
    private void logDifferentCalls(final VslIdentity identity, final String operation,
            final String address) throws NoPermissionException {
        if (anomalyDetectionConfig.isAnomalyDetectionEnabled()) {
            boolean allowance = anomalyDetectionService.logVariousStuff(identity, operation,
                    address);
            if (!allowance && anomalyDetectionConfig.isBlockOnAnomaly()) {
                throw new NoPermissionException("Access denied by Anomaly detection");
            }
        }
    }

    /**
     * Tests get/set operations for anomalies. Throws an {@link NoPermissionException} if the
     * operation is anomalous and should be blocked.
     *
     * @param identity
     *            The {@link VslIdentity} used for the operation.
     * @param operation
     *            The operation to test.
     * @param address
     *            The address the operation operates on.
     * @throws NoPermissionException
     *             Exception thrown when the operation is anomalous.
     * @param node
     *            The {@link VslNode} that was requested or set.
     */
    private void logAccess(final VslIdentity identity, final String operation, final String address,
            final VslNode node) throws NoPermissionException {
        if (anomalyDetectionConfig.isAnomalyDetectionEnabled()) {
            boolean allowance = anomalyDetectionService.logAccess(identity, operation, address,
                    node);
            if (!allowance && anomalyDetectionConfig.isBlockOnAnomaly()) {
                throw new NoPermissionException("Access denied by Anomaly detection");
            }
        }
    }

    /**
     * Tests subscription operations for anomalies. Throws an {@link NoPermissionException} if the
     * operation is anomalous and should be blocked.
     *
     * @param identity
     *            The {@link VslIdentity} used for the operation.
     * @param operation
     *            The operation to test.
     * @param address
     *            The address the operation operates on.
     * @throws NoPermissionException
     *             Exception thrown when the operation is anomalous.
     * @param value
     *            The parameters of the subscription.
     */
    private void logSubscription(final VslIdentity identity, final String operation,
            final String address, final String value) throws NoPermissionException {

        if (anomalyDetectionConfig.isAnomalyDetectionEnabled()) {
            boolean allowance = anomalyDetectionService.logSubscriptions(identity, operation,
                    address, value);
            if (!allowance && anomalyDetectionConfig.isBlockOnAnomaly()) {
                throw new NoPermissionException("Access denied by Anomaly detection");
            }
        }
    }

}
