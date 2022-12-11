package org.ds2os.vsl.core;

import java.util.Collection;

import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;

/**
 * Interface for the SubscriptionManager, which is responsible for managing subscriptions. (add
 * subscribers, execute callbacks on change notifications,...).
 *
 * @author liebald
 */
public interface VslSubscriptionManager {

    /**
     * This method is called when a node changed. The {@link VslSubscriptionManager} is responsible
     * for checking whether anyone subscribed to this node (or any parent) and call the registered
     * callback if this is the case.
     *
     * @param address
     *            the address in the VSL of the changed node.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void notifySubscribers(String address) throws VslException;

    /**
     * This method is called when a subtree changed (multiple nodes). The
     * {@link VslSubscriptionManager} is responsible for checking whether anyone subscribed to any
     * of the affected nodes (or any parents) and call the registered callback if this is the case.
     *
     * @param addresses
     *            the addresses in the VSL that changed.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void notifySubscribers(Collection<String> addresses) throws VslException;

    /**
     * Subscribe to changes of the knowledge at the given address (including changes in the subtree
     * of that address if present). A subscription will receive notifications for changes to the
     * direct address as well as all of the knowledge in its subtree. To successfully subscribe and
     * receive notifications, the requesting service needs at least reading rights on the requested
     * address.
     *
     * @param address
     *            the node address in the VSL.
     * @param subscriber
     *            the callback handler which will be notified about changes.
     * @param params
     *            The parameters to define which nodes are subscribed.
     * @param identity
     *            the identity of the issuer of this operation.
     * @param affectedNodes
     *            the affected node(s - if with children) used to determine children.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void addSubscription(String address, VslSubscriber subscriber, VslAddressParameters params,
            VslIdentity identity, VslNode affectedNodes) throws VslException;

    /**
     * Unsubscribe from an address so that no more notifications to that subtree are received by the
     * VslIdentity that issued the unsubscribe.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters to define which nodes are unsubscribed.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void removeSubscription(String address, VslAddressParameters params, VslIdentity identity)
            throws VslException;

    /**
     * Activates remote subscription handling.
     *
     * @param agentRegistry
     *            The local {@link VslAgentRegistryService}.
     * @param transportManager
     *            The local {@link VslTransportManager}.
     * @param agentName
     *            The local {@link VslAgentName}.
     */
    void activate(VslAgentRegistryService agentRegistry, VslTransportManager transportManager,
            VslAgentName agentName);

    /**
     * Is this subscription handler handling remote subscriptions as well?
     *
     * @return true iff it does.
     */
    boolean isHandlingRemoteSubscriptions();
}
