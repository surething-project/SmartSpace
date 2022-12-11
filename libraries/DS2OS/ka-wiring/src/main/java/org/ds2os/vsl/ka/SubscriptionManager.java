package org.ds2os.vsl.ka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.SubscriptionNotSupportedException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SubscriptionManager Implementation.
 *
 * @author liebald
 */
public class SubscriptionManager implements VslSubscriptionManager, VslSubscriber, Runnable {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionManager.class);

    /**
     * Flag that indicates whether remote subscription handling is enabled.
     */
    private boolean remoteSubcriptionsEnabled = false;

    /**
     * The local {@link VslAgentRegistryService}.
     */
    private VslAgentRegistryService agentRegistry = null;

    /**
     * The local {@link VslTransportManager}.
     */
    private VslTransportManager transportManager = null;

    /**
     * The local VslAgentName.
     */
    private VslAgentName agentName = null;

    /**
     * Map of subscribers from the localKA. External map is subscribedAddress: "Map of subscribers
     * to that address". "Map of subscribers to that address" is ID of subscriber:callback.
     */
    private final Map<String, Map<String, VslSubscriber>> localSubscribers;

    /**
     * Map of remote KA subscribers. External map is subscribedAddress: "Map of subscribers to that
     * address". "Map of subscribers to that address" is ID of subscriber:callback.
     */
    private final Map<String, Map<String, VslSubscriber>> remoteSubscribers;

    /**
     * Queue of notifications which need to be processed (sent out).
     */
    private final Queue<String> notificationQueue;

    /**
     * Constructor of the SubscriptionManager.
     */
    public SubscriptionManager() {
        localSubscribers = new HashMap<String, Map<String, VslSubscriber>>();
        remoteSubscribers = new HashMap<String, Map<String, VslSubscriber>>();
        notificationQueue = new ConcurrentLinkedQueue<String>();
    }

    @Override
    public final void notifySubscribers(final String changedAddress) throws VslException {
        notifySubscribers(Arrays.asList(changedAddress));
    }

    /**
     * Real notification process.
     *
     * @param addresses
     *            all addresses that changed.
     */
    private synchronized void realNotify(final Collection<String> addresses) {
        // LOGGER.debug("Check for existing subscriptions for {}", addresses);
        // only check parents up to service level.
        // LOGGER.debug("notify: {}", addresses.toString());
        final Map<String, List<VslSubscriber>> subs = new HashMap<String, List<VslSubscriber>>();

        // collect all affected local subscribers
        for (final String changedAddress : addresses) {
            final String address = AddressParser.makeWellFormedAddress(changedAddress);

            final Map<String, VslSubscriber> map = localSubscribers.get(address);
            if (map != null && map.size() > 0) {
                LOGGER.trace("sending notification for address: {} to local subscriber", address);
                subs.put(address, new LinkedList<VslSubscriber>(map.values()));
            }

        }
        // collect remote subscribers
        for (final String changedAddress : addresses) {
            final String address = AddressParser.makeWellFormedAddress(changedAddress);

            final Map<String, VslSubscriber> map = remoteSubscribers.get(address);
            if (map != null && map.size() > 0) {
                LOGGER.trace("sending notification for address: {} to remote subscriber", address);

                if (subs.containsKey(changedAddress)) {
                    // don't overwrite local subscribers
                    subs.get(changedAddress).addAll(new LinkedList<VslSubscriber>(map.values()));
                } else {
                    subs.put(changedAddress, new LinkedList<VslSubscriber>(map.values()));
                }
            }

        }
        // LOGGER.debug("notify {} subscribers", subs.size());
        // notify all affected subscribers.
        for (final Entry<String, List<VslSubscriber>> entry : subs.entrySet()) {
            for (final VslSubscriber vslSubscriber : entry.getValue()) {
                try {
                    vslSubscriber.notificationCallback(entry.getKey());
                } catch (final VslException e) {
                    // TODO: make exception for unusable callback and remove node from subscribers?
                    LOGGER.error("notification callback failed for address {}", entry.getKey(), e);
                }
            }
        }
    }

    @Override
    public final void notifySubscribers(final Collection<String> addresses) {
        synchronized (notificationQueue) {
            for (final String address : addresses) {
                notificationQueue.offer(address);
            }
            notificationQueue.notifyAll();
        }
    }

    @Override
    public final synchronized void addSubscription(final String subscribeAddress,
            final VslSubscriber subscriber, final VslAddressParameters params,
            final VslIdentity identity, final VslNode affectedNodes) throws VslException {
        final String address = AddressParser.makeWellFormedAddress(subscribeAddress);
        if (affectedNodes == null) {
            throw new SubscriptionNotSupportedException(
                    "given address doesn't exist or can not be read: " + address);
        }

        final Set<String> affectedAddresses = new HashSet<String>();
        // TODO: subscriptions might be ok if only write access on a node exists. The KOR would
        // filter these when using the normal get request for identifying all affected nodes.
        // TODO: need to filter the results (access) in case intermediary nodes cannot be
        // subscribed.
        affectedAddresses.add(address);
        for (Entry<String, VslNode> entry : affectedNodes.getAllChildren()) {
            affectedAddresses.add(address + "/" + entry.getKey());
        }

        LOGGER.info("Add new service subscription for addresses {}", affectedAddresses);

        for (String affectedAddress : affectedAddresses) {

            // If a remote KA subscribes, add him to remoteSubscribers.
            if (identity.isKA()) {
                Map<String, VslSubscriber> map = remoteSubscribers.get(affectedAddress);
                if (map == null) {
                    map = new HashMap<String, VslSubscriber>();
                    remoteSubscribers.put(affectedAddress, map);
                }
                map.put(identity.getClientId(), subscriber);
            } else {
                // if a local service subscribes, add him to local subscribers and check if a remote
                // subscription is necessary.
                Map<String, VslSubscriber> map = localSubscribers.get(address);
                if (map == null) {
                    map = new HashMap<String, VslSubscriber>();
                    localSubscribers.put(affectedAddress, map);
                }
                map.put(identity.getClientId(), subscriber);

                if (remoteSubcriptionsEnabled
                        && !affectedAddress.equals("/" + agentName.getAgentName())
                        && !affectedAddress.startsWith("/" + agentName.getAgentName() + "/")
                        && localSubscribers.containsKey(affectedAddress)
                        && localSubscribers.get(affectedAddress).size() == 1) {
                    // size==1 ensures that this part is only executed when a subscription to a
                    // remote address is the first one and there are not already some existing.
                    LOGGER.debug("Add service subscription for remote KA {}", address);
                    try {
                        subscribeToRemoteKA(affectedAddress, params);
                    } catch (final VslException e) {
                        // remove the subscription from the local KA if an error occurred when
                        // registering it and throw an exception.
                        // remotely:
                        localSubscribers.get(affectedAddress).remove(identity.getClientId());
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Adds a subscription of an address to the remote KA, if noone has already subscribed to it.
     *
     * @param address
     *            The address that should be subcribed.
     * @param params
     *            The parameters used for the subscription.
     * @throws VslException
     *             If a {@link VslException} occurs.
     */
    private void subscribeToRemoteKA(final String address, final VslAddressParameters params)
            throws VslException {
        // subscribe to remote KA:
        final VslRequestHandler requestHandler = getRemoteRequestHandler(address);
        if (requestHandler != null) {
            requestHandler.subscribe(address, this, params, null);
            LOGGER.info("added new subscription for address {} at remote KA", address);
        }
    }

    /**
     * Retrieve a requestHandler from the transport that can be used to communicate with the KA that
     * is responsible for the given address.
     *
     * @param address
     *            The address for which the {@link VslRequestHandler} is needed.
     * @return The {@link VslRequestHandler} to the remote KA.
     */
    private VslRequestHandler getRemoteRequestHandler(final String address) {
        final Collection<VslTransportConnector> transports = agentRegistry
                .getTransports(extractAgentId(address));
        final String[] urls = new String[transports.size()];
        int i = 0;
        for (final VslTransportConnector connector : transports) {
            urls[i] = connector.getURL();
            i++;
        }
        return transportManager.getTransportToKA(urls);
    }

    /**
     * Extract the agent id out of a fully resolved address.
     *
     * @param resolvedAddress
     *            the resolved address.
     * @return the agent id.
     */
    private String extractAgentId(final String resolvedAddress) {
        final int secondSlash = resolvedAddress.indexOf('/', 1);
        if (secondSlash < 0) {
            return resolvedAddress.substring(1);
        } else {
            return resolvedAddress.substring(1, secondSlash);
        }
    }

    @Override
    public final synchronized void removeSubscription(final String unsubscribeAddress,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        final String address = AddressParser.makeWellFormedAddress(unsubscribeAddress);
        LOGGER.debug("Remove service subscription for {}", address);
        // if a remote KA unsubscribes, simply remove him.
        // Otherwise check if the subscriptionManager can unsubscribe from a remote KA after a
        // service unsubscribed the given address.

        if (identity.isKA()) {
            for (String affectedAddress : getAffectedAddresses(address, remoteSubscribers.keySet(),
                    params)) {

                final Map<String, VslSubscriber> map = remoteSubscribers.get(affectedAddress);
                if (map != null) {
                    map.remove(identity.getClientId());
                    if (map.isEmpty()) {
                        remoteSubscribers.remove(affectedAddress);
                    }
                }
            }
        } else {
            for (String affectedAddress : getAffectedAddresses(address, localSubscribers.keySet(),
                    params)) {

                final Map<String, VslSubscriber> map = localSubscribers.get(affectedAddress);
                if (map != null) {

                    map.remove(identity.getClientId());
                    if (map.isEmpty()) {
                        localSubscribers.remove(affectedAddress);
                    }

                    if (remoteSubcriptionsEnabled
                            && !affectedAddress.equals("/" + agentName.getAgentName())
                            && !affectedAddress.startsWith("/" + agentName.getAgentName() + "/")
                            && !localSubscribers.containsKey(affectedAddress)) {
                        try {
                            unSubscribeFromRemoteKA(affectedAddress, params);
                        } catch (final VslException e) {
                            LOGGER.error("Couldn't unsubscribe from remote KA for address {}",
                                    affectedAddress, e);
                        }
                    }
                }
            }
        }

    }

    /**
     * Helper function to get all affected addresses from subscriberlists that are affected by an
     * operation based on the address parameters given.
     *
     * @param rootAddress
     *            The affected rootaddress (on which the operation was done).
     * @param currentlySubscribedAddresses
     *            The Set of currently subscribed addresses (of anyone).
     * @param params
     *            The parameters of the list.
     * @return Set of all affected addresses.
     */
    private synchronized Set<String> getAffectedAddresses(final String rootAddress,
            final Set<String> currentlySubscribedAddresses, final VslAddressParameters params) {
        Set<String> affected = new HashSet<String>();
        final String regex;
        if (params.getDepth() < 0) {
            regex = "^" + rootAddress.replace("/", "\\/") + "(\\/[A-Za-z0-9]*)*$";
        } else {
            regex = "^" + rootAddress.replace("/", "\\/") + "(\\/[A-Za-z0-9]*){0,"
                    + params.getDepth() + "}$";
        }

        for (String address : currentlySubscribedAddresses) {
            if (address.matches(regex)) {
                affected.add(address);
            }
        }
        return affected;
    }

    /**
     * Unsubscribe from remote KA for the given address.
     *
     * @param address
     *            The address that should be unsubscribed.
     * @param params
     *            The parameters used for the subscription.
     * @throws VslException
     *             If a {@link VslException} occurs.
     */
    private void unSubscribeFromRemoteKA(final String address, final VslAddressParameters params)
            throws VslException {
        final VslRequestHandler requestHandler = getRemoteRequestHandler(address);
        if (requestHandler != null) {
            requestHandler.unsubscribe(address, params, null);
            LOGGER.debug("Removed service subscription from remote KA {}", address);
        }
    }

    @Override
    public final synchronized void activate(final VslAgentRegistryService newAgentRegistry,
            final VslTransportManager newTransportManager, final VslAgentName newAgentName) {
        agentRegistry = newAgentRegistry;
        transportManager = newTransportManager;
        agentName = newAgentName;
        remoteSubcriptionsEnabled = agentRegistry != null && transportManager != null
                && agentName != null;

        final Thread notificationThread = new Thread(this, "notifications");
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    @Override
    public final void notificationCallback(final String address) throws VslException {
        LOGGER.trace("received notificationcallback from remote KA for address {}", address);
        notifySubscribers(address);
    }

    @Override
    public final void run() {
        try {
            while (true) {
                try {
                    final List<String> notifications = new ArrayList<String>();
                    synchronized (notificationQueue) {
                        while (notificationQueue.peek() == null) {
                            notificationQueue.wait();
                        }
                        while (notificationQueue.peek() != null) {
                            notifications.add(notificationQueue.poll());
                        }
                    }
                    realNotify(notifications);
                } catch (final RuntimeException e) {
                    LOGGER.error("RuntimeException in notification thread:", e);
                    Thread.sleep(100L);
                }
            }
        } catch (final InterruptedException e) {
            LOGGER.warn("Notification thread interrupted, no more notifications will be sent out.");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public final boolean isHandlingRemoteSubscriptions() {
        return remoteSubcriptionsEnabled;
    }
}
