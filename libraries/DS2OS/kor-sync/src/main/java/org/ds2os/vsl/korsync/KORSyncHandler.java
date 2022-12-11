package org.ds2os.vsl.korsync;

import java.sql.Date;
import java.sql.Time;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslKASyncConnector;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslKORUpdateSender;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.config.VslKORSyncConfig;
import org.ds2os.vsl.core.impl.HandshakeData;
import org.ds2os.vsl.core.impl.KAInfo;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.korsync.updateCache.KorUpdateCache;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link VslKORSyncHandler} interface.
 *
 * @author liebald
 * @author felix
 * @author Johannes Straßer
 *
 */
public class KORSyncHandler extends AbstractVslModule implements VslKORSyncHandler, Runnable {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(KORSyncHandler.class);

    /**
     * Transport manager variable to be used for invoking specific transport.
     */
    private final VslTransportManager transportManager;

    /**
     * Update handler for KOR.
     */
    private final VslKORUpdateHandler korUpdateHandler;

    /**
     * Reference to the Agent Registry service.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * Access to the config of the KORsync.
     */
    private final VslKORSyncConfig config;

    /**
     * {@link VslConnector} for communication with the overlay network.
     */
    private final VslParametrizedConnector korSyncConnector;

    /**
     * {@link KorUpdateCache} to cache updates which couldn't be applied directly.
     */
    private final KorUpdateCache updateCache;

    /**
     * The thread responsible for the handshake initialization.
     */
    private Thread handShakerThread;

    /**
     * The address of the agentRegistryService.
     */
    private String agentRegistryAddress;

    /**
     * Lock to ensure only one handshake at a time happens.
     */
    private final Lock handshakeLock;

    /**
     * Map for caching groupkeys that couldn't be delivered on the handshake.
     */
    private final Map<String, VslNode> groupKeyCache = new HashMap<String, VslNode>();

    /**
     * lock to make sure only one korupdate is processed at the same time.
     */
    private final Lock handleKORUpdateLock = new ReentrantLock();

    /**
     * Constructor.
     *
     * @param transportManager
     *            {@link VslTransportManager} object for Transport Manager.
     * @param updateHandler
     *            {@link VslKORUpdateHandler} object for KOR updates.
     * @param agentRegistry
     *            {@link VslAgentRegistryService} object to access the Agent Registry service
     * @param korSyncConnector
     *            {@link VslConnector} for communication with the overlay network.
     * @param config
     *            {@link VslKORSyncConfig} object to access the configuration of the korsync.
     */
    public KORSyncHandler(final VslTransportManager transportManager,
            final VslKORUpdateHandler updateHandler, final VslAgentRegistryService agentRegistry,
            final VslParametrizedConnector korSyncConnector, final VslKORSyncConfig config) {
        this.transportManager = transportManager;
        this.korUpdateHandler = updateHandler;
        this.agentRegistry = agentRegistry;
        this.config = config;
        this.korSyncConnector = korSyncConnector;
        this.updateCache = new KorUpdateCache(config);

        handshakeLock = new ReentrantLock();
    }

    @Override
    public final synchronized void activate() throws Exception {
        agentRegistryAddress = korSyncConnector.get("/search/type/system/agentRegistryService")
                .getValue().split("//")[0];
        korUpdateHandler.setKORSyncHandler(this);
        updateCache.activate();
        handShakerThread = new Thread(this, "Handshake thread");
        handShakerThread.setDaemon(true);
        handShakerThread.start();
    }

    @Override
    public final synchronized void shutdown() {
        handShakerThread.interrupt();
        updateCache.shutdown();
    }

    @Override
    public final synchronized void run() {
        int counter = 1;
        while (!Thread.currentThread().isInterrupted()) {

            try {

                // Try to deliver groupkeys that couldn't be delivered earlier.

                if (!groupKeyCache.isEmpty()) {
                    deliverCachedGroupKeys();
                }

                // It is possible that an agent is both in the connected and unconnected list,
                // leading to this message. Therefore call the cleanDoubleConnectedAgents to fix
                // this. Can be removed when the agentregistry works more atomic.
                if (counter++ % 5 == 0) {
                    agentRegistry.cleanDoubleConnectedAgents();
                }

                // give other threads the chance to gain the lock by waiting a few milliseconds
                try {
                    wait(500);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                handshakeLock.lock();
                final String[] unconnectedAgents = agentRegistry.getUnConnectedAgentIds();
                // if there are no unconnected agents, wait for a while and check again

                if (unconnectedAgents.length == 0) {
                    // LOGGER.debug("No unconnected Agents found.");
                    continue;
                }

                if (!agentRegistry.isLeader()) {
                    // if we are not the leader, wait for a longer time and check again. (Leader may
                    // change, but only rarely, so no need for frequent checks.)
                    LOGGER.debug("Not the leader, waiting for leader to initiate handshake.");
                    continue;
                }

                for (final String agentID : unconnectedAgents) {
                    VslNode agent = null;
                    try {
                        agent = korSyncConnector.get(
                                agentRegistryAddress + "/unconnectedKAs/" + agentID,
                                new AddressParameters().withDepth(-1)
                                        .withNodeInformationScope(NodeInformationScope.COMPLETE));
                    } catch (final VslException e) {
                        LOGGER.error("Exception on reading address {}",
                                agentRegistryAddress + "/unconnectedKAs/" + agentID, e);
                        continue;
                    }

                    // Check if we are the smaller network

                    int remoteNetworkSize = -1;
                    try {
                        remoteNetworkSize = Integer.parseInt(agent.getChild("numKAs").getValue());
                    } catch (final NumberFormatException e) {
                        LOGGER.debug("COuldn't parse remoteNetwork size as integer: {}");
                        continue;
                        // can happen if the model was instantiated but not yet populated, so the
                        // numKA value is an empty string.
                        // in this case wait a bit and try again.

                    }
                    if (remoteNetworkSize <= 0) {
                        LOGGER.debug("remote network size <=0");
                        continue;
                    }

                    // Only handshake if the other node's numKA is stagnant and this is not the
                    // first AlivePing from it

                    if (agent.getVersion() <= 3) { // || numKAs != alivePing.getNumKAs()) {
                        LOGGER.info(
                                "Waiting for environment to stabilize..."
                                        + " (remote agent version {} must be > 3)",
                                agent.getVersion());
                        continue;
                    }

                    final int localNetworkSize = agentRegistry.getNetworkSize();
                    if (remoteNetworkSize < localNetworkSize) {
                        LOGGER.info("Awaiting smaller network to initiate Handshake.");
                        continue;
                    }

                    final String remoteGroupID = agent.getChild("groupID").getValue();
                    // Check if we know all KAs of the other network
                    // removed, we don't need to know all other KAs. if the other one is not the
                    // leader he will deny the handshake.
                    // TODO: remove completely as soon as it is clear wheather removing this
                    // criterium has no side effects.
                    // if (!agentRegistry.isGroupReachable(remoteGroupID, remoteNetworkSize)) {
                    // LOGGER.info("Waiting for AlivePings from missing KAs.");
                    // continue;
                    // }

                    // Get the other leader's ID
                    final String otherLeader = agentRegistry.getLeader(remoteGroupID);
                    // If both networks have the same size, the one with the leader with the
                    // lexicographically lower agentID starts the handshake
                    if (remoteNetworkSize == localNetworkSize
                            && otherLeader.compareTo(config.getAgentName()) > 0) {
                        LOGGER.info("Networks have the same size, waiting for "
                                + "the other leader to initiate handshake");
                        continue;
                    }

                    // Start handshaking
                    LOGGER.info("Initiating Group Merge with agent {}", otherLeader);
                    // Do unicast handshake with other leader.
                    final HashSet<VslTransportConnector> remoteTransports = agentRegistry
                            .getTransports(otherLeader);
                    if (!remoteTransports.isEmpty()) {
                        initHandshake(otherLeader, remoteTransports);
                        LOGGER.debug("Group merge done with {}", otherLeader);
                        break;
                    } else {
                        LOGGER.debug("remote transports of {} empty", otherLeader);
                    }
                    // if a handshake was successful wait a bit and then continue polling for
                    // unconnected agents to check if a new handshake is necessary
                }

            } finally {
                handshakeLock.unlock();
            }
        }
        LOGGER.debug("KA sync process was interrupted!");
    }

    /**
     * Initialize a handshake upon a received alive ping message.
     *
     * @param remoteAgent
     *            The ID of the agent that we want to handshake with
     * @param transports
     *            The collection of transports of the agent that we want to handshake with
     */
    private synchronized void initHandshake(final String remoteAgent,
            final Collection<VslTransportConnector> transports) {

        try {
            LOGGER.debug("handshake started");
            // based on supported transports get a sync connector from transportManager.
            final VslKASyncConnector kaSyncConnector = transportManager
                    .getKASyncConnector(getSupportetTransportUrls(transports));

            if (kaSyncConnector == null) {
                // Handshake failed
                LOGGER.error(
                        "KORsync handshake failed: could not get a KASyncConnector"
                                + ", supportedTransports: {}",
                        Arrays.asList(getSupportetTransportUrls(transports)));
                return;
            }
            VslHandshakeData remoteHandshake;
            final Collection<VslKAInfo> connectedKAs = getConnectedKAInfo(remoteAgent);
            try {
                // send the handshake and wait for the response.
                LOGGER.debug("Send handshakeData to remoteKA");
                remoteHandshake = kaSyncConnector
                        .doHandshake(new HandshakeData(connectedKAs, null, null));
                if (remoteHandshake == null) {
                    LOGGER.debug("Retrieved null as remote HandshakeData, aborting handshake"
                            + " (possibly another handshake in progress.)");
                    return;
                }
                LOGGER.debug("retrieved HandshakeData from remote KA");
            } catch (final VslException e) {
                // Handshake failed
                LOGGER.error("KORsync handshake failed: ", e);
                return;
            }

            handshakeUpdate(remoteHandshake, connectedKAs);

        } catch (final RuntimeException e) {
            LOGGER.error("RuntimeException in HandshakeInitHandler: ", e);
        }
    }

    /**
     * Helper function to extract all URLs of transports of the given Collection of transports.
     *
     * @param transports
     *            Collection of {@link VslTransportConnector}
     * @return array of transport URLs
     */
    private String[] getSupportetTransportUrls(final Collection<VslTransportConnector> transports) {
        final Collection<String> supportedTransportUrls = new LinkedList<String>();
        for (final VslTransportConnector transportObj : transports) {
            supportedTransportUrls.add(transportObj.getURL());
        }
        return supportedTransportUrls.toArray(new String[supportedTransportUrls.size()]);
    }

    @Override
    public final VslHandshakeData handleHandshakeRequest(final VslHandshakeData handshakeRequest) {
        if (!handshakeLock.tryLock()) {
            return null;
        }
        if (!agentRegistry.isLeader()) {
            LOGGER.debug(
                    "retrieved handshakerequest but this agent is not the leader of his group.");
            return null;
        }
        LOGGER.debug("got lock");
        // ugly, better way to get the remoteKA than assuming its the first in the list?
        final String remoteKA = handshakeRequest.getKaInfo().iterator().next().getAgentId();
        LOGGER.debug("Received incoming handshakerequest from {}", remoteKA);

        // create the new multicastKey
        // TODO optional: use the key of the larger group?
        final String tlsString = agentRegistry
                .getMulticastGroupKeyString(agentRegistry.getMulticastGroupKeyHash());
        final KeyStringParser s = new KeyStringParser(tlsString);
        final byte[] newKey = s.createKey();
        final String newKeyString = DatatypeConverter.printBase64Binary(newKey);
        final Collection<VslKAInfo> connectedKAs = getConnectedKAInfo(remoteKA);

        handshakeUpdate(new HandshakeData(handshakeRequest.getKaInfo(), newKeyString, tlsString),
                connectedKAs);

        handshakeLock.unlock();

        return new HandshakeData(connectedKAs, newKeyString, tlsString);
    }

    /**
     * Get information about all locally connected KAs. Also includes the own KAInfo. Excludes the
     * remoteKA if already known.
     *
     * @param remoteKA
     *            The name of the KA we are handshaking with, this will be excluded from the list of
     *            connectedKAs if it is in it.
     * @return Collection of VslKAInfo.
     */
    private Collection<VslKAInfo> getConnectedKAInfo(final String remoteKA) {
        final Collection<VslKAInfo> allKAInfo = new HashSet<VslKAInfo>();

        // also include the own KAInfo
        allKAInfo
                .add(new KAInfo(config.getAgentName(), transportManager.getAllTransportConnectors(),
                        korUpdateHandler.getCurrentKORHash()));
        allKAInfo.addAll(agentRegistry.getConnectedKAInfo());

        // remove the remote KA from the list if it exists
        for (final Iterator<VslKAInfo> iterator = allKAInfo.iterator(); iterator.hasNext();) {
            final VslKAInfo vslKAInfo = iterator.next();
            if (vslKAInfo.getAgentId().equals(remoteKA)) {
                iterator.remove();
            }
        }
        return allKAInfo;
    }

    @Override
    public final void handleKORUpdate(final VslKORUpdate korUpdate) {
        // TODO Handle a KOR update from a KA which is part of the network ->
        // will be passed on to the KOR. Only must handle the case when KOR
        // can't process the update because it is too old.
        handleKORUpdateLock.lock();
        try {
            if (korUpdate.getAgentName() == null
                    || korUpdate.getAgentName().equals(config.getAgentName())) {
                // LOGGER.debug("Didn't handle KORUpdate, either from own agent or null.");
                return;
            }
            LOGGER.debug("handleKORUpdate from: {}", korUpdate.getAgentName());

            final String currentHashOfRemoteKA = korUpdateHandler
                    .getCurrentKORHashOf("/" + korUpdate.getAgentName());

            if (korUpdate.getHashTo().equals(currentHashOfRemoteKA)) {
                // if we already have the hash the update would update to, the
                // update doesn't need to be applied.
                LOGGER.debug("Local Structure already up to date, update not applied.");
                return;
            } else if (korUpdate.getHashFrom().isEmpty()
                    || korUpdate.getHashFrom().equals(currentHashOfRemoteKA)) {
                // a full update can simply be applied.
                korUpdateHandler.applyKORUpdate(korUpdate);
                LOGGER.debug("applied KOR update from {}", korUpdate.getAgentName());
                checkForCachedUpdates(korUpdate.getAgentName());

            } else if (!korUpdate.getHashFrom().equals(currentHashOfRemoteKA)) {
                updateCache.add(korUpdate);
                LOGGER.debug("hash mismatch: hashfrom: {}, localHash: {}, stored in UpdateCache",
                        korUpdate.getHashFrom(), currentHashOfRemoteKA);

            }
        } finally {
            handleKORUpdateLock.unlock();
        }
    }

    @Override
    public final void checkKORUpdate(final String agentID, final String currentHash) {
        // TODO: maybe run as external thread, we do to many db lookups per aliveping.
        String currentKorHashOfRemoteAgent = korUpdateHandler.getCurrentKORHashOf("/" + agentID);
        if (!currentKorHashOfRemoteAgent.equals(currentHash)) {
            LOGGER.debug(
                    "CurrentHash of {}, locally stored hash is: {}, received Hash is: "
                            + "{}, trying to retrieve an update.",
                    agentID, currentKorHashOfRemoteAgent, currentHash);
            final VslKASyncConnector kaSyncConnector = transportManager.getKASyncConnector(
                    getSupportetTransportUrls(agentRegistry.getTransports(agentID)));
            try {
                korUpdateHandler.applyKORUpdate(
                        kaSyncConnector.requestUpdate(agentID, "currentKorHashOfRemoteAgent"));
            } catch (VslException e) {
                LOGGER.error("Error checking for remote KOR update:", e);
            }
        }
    }

    /**
     * Checks if any stored updates can be applied and does so if possible.
     *
     * @param agentID
     *            The agent to check for cached updates.
     */
    private void checkForCachedUpdates(final String agentID) {
        VslKORUpdate update = updateCache.getUpdate(agentID,
                korUpdateHandler.getCurrentKORHashOf("/" + agentID));
        while (update != null) {
            korUpdateHandler.applyKORUpdate(update);
            update = updateCache.getUpdate(agentID,
                    korUpdateHandler.getCurrentKORHashOf("/" + agentID));
        }

    }

    @Override
    public final void sendIncrementalUpdate(final VslKORUpdate update) {
        final Collection<VslKAInfo> connectedKAs = agentRegistry.getConnectedKAInfo();
        LOGGER.debug("sending incremental update. added: {}, removed: {}",
                update.getAddedNodes().entrySet(), update.getRemovedNodes());
        final Collection<VslKORUpdateSender> updateSenders = transportManager
                .getKORUpdateSenders(connectedKAs.toArray(new KAInfo[connectedKAs.size()]));
        LOGGER.debug("Got {} updateSenders for {} connected KAs", updateSenders.size(),
                connectedKAs.size());

        for (final VslKORUpdateSender updateSender : updateSenders) {
            LOGGER.debug("send for updateSender");
            updateSender.sendKORUpdate(update);
        }
    }

    /**
     * Updates the KOR of all affected KAs after a handshake was successful.
     *
     * @param remoteHandshake
     *            The remote handshake {@link VslHandshakeData}.
     * @param connectedKAs
     *            All {@link VslKAInfo} for all connected KAs.
     */
    final void handshakeUpdate(final VslHandshakeData remoteHandshake,
            final Collection<VslKAInfo> connectedKAs) {

        try {
            LOGGER.debug("handshakeUpdate started");
            // get the current multicast key of my group
            final byte[] oldKeyHash = DatatypeConverter
                    .parseHexBinary(agentRegistry.getMulticastGroupKeyHash());
            // set the new key on all connected KAs
            updateGroupKey(remoteHandshake.getNewTLSString(),
                    DatatypeConverter.parseBase64Binary(remoteHandshake.getNewGroupKey()),
                    connectedKAs);
            String agents = "";
            for (VslKAInfo b : connectedKAs) {
                agents += b.getAgentId() + ",";
            }
            LOGGER.debug("new groupKey distributed to " + agents);
            // move new KAs to connected KAs
            for (final VslKAInfo vslKAInfo : remoteHandshake.getKaInfo()) {
                agentRegistry.addAgentToConnectedKAList(vslKAInfo.getAgentId(),
                        new Time(System.currentTimeMillis()), new Time(0));
            }
            LOGGER.debug("added agents to connectedKAList");
            // KORsync is finished afterwards, updates follow but are not part of the handshake.
            // KORSyncHandler.handshakeFinished();

            // query all new KAs for their KOR structure update
            final Collection<VslKORUpdate> updates = new HashSet<VslKORUpdate>();
            LOGGER.debug("Started update Process after korSyncHandshake.");
            for (final VslKAInfo kaInfo : remoteHandshake.getKaInfo()) {
                boolean updateNeeded = true;
                // ignore updates for our own KA if this happens.
                if (kaInfo.getAgentId().equals(config.getAgentName())) {
                    continue;
                }
                // check if the remote KA is already connected locally.
                for (final VslKAInfo connectedKAInfo : connectedKAs) {
                    if (kaInfo.getAgentId().equals(connectedKAInfo.getAgentId())
                            && kaInfo.getKorHash().equals(
                                    korUpdateHandler.getCurrentKORHashOf(kaInfo.getAgentId()))) {
                        // KA is connected locally and the hash is up to date ->nothing to do
                        updateNeeded = false;
                        LOGGER.debug("Agent {} already connected locally"
                                + " and up to date, nothing to do.", kaInfo.getAgentId());
                        break;
                    }
                }

                if (!updateNeeded) {
                    continue;
                }

                final VslKASyncConnector kaSyncConnector = transportManager
                        .getKASyncConnector(getSupportetTransportUrls(kaInfo.getTransports()));
                try {
                    updates.add(kaSyncConnector.requestUpdate(kaInfo.getAgentId(), ""));
                } catch (final Exception e) {
                    // TODO what to do? for now simply ignore and try the next one
                    // When receiving an aliveping the ka will request a new update
                    LOGGER.error("Requesting an update after korSyncHandshake failed, "
                            + "HANDLING NOT IMPLEMENTED", e);
                }

            }

            // now send the updates to the other KAs of the old network. (multicast)
            LOGGER.debug("gathered all updates after korsyncHandshake (#:{}),"
                    + " start applying and disseminating them.", updates.size());
            final Collection<VslKORUpdateSender> updatesenders = transportManager
                    .getKORUpdateSenders(connectedKAs.toArray(new VslKAInfo[connectedKAs.size()]));
            for (final VslKORUpdate korUpdate : updates) {
                korUpdateHandler.applyKORUpdate(korUpdate);
                LOGGER.debug("applied KOR update from {}", korUpdate.getAgentName());
                for (final VslKORUpdateSender updatesender : updatesenders) {
                    try {
                        updatesender.sendKORUpdate(korUpdate, oldKeyHash);
                    } catch (final KeyNotInKeystoreException e) {
                        LOGGER.error("Could not send KORUpdate: " + e.getMessage());
                    }
                }
            }

        } catch (final RuntimeException e) {
            LOGGER.error("RuntimeException in HandshakeUpdateHandler: ", e);
        }

    }

    /**
     * Sets a new groupKey to itself and to all agents in the connectedKAs list.
     *
     * @param tlsString
     *            The TLS String describing the usage of the key
     * @param key
     *            The key data
     * @param connectedKAs
     *            {@link VslKAInfo} for all connected KAs.
     */
    private void updateGroupKey(final String tlsString, final byte[] key,
            final Collection<VslKAInfo> connectedKAs) {
        final VslMutableNode node = korSyncConnector.getNodeFactory()
                .createMutableNode(tlsString + "," + DatatypeConverter.printHexBinary(key));
        node.setTimestamp(new Date(System.currentTimeMillis()));
        for (final VslKAInfo kaInfo : connectedKAs) {
            try {
                LOGGER.debug("Setting groupKey to /" + kaInfo.getAgentId()
                        + "/system/agentRegistryService" + "/newGroupKey");
                korSyncConnector.set(
                        "/" + kaInfo.getAgentId() + "/system/agentRegistryService" + "/newGroupKey",
                        node);
            } catch (final Exception e) {
                LOGGER.error(
                        "Could not send new groupKey to node {}, waiting a bit then trying again:",
                        kaInfo.getAgentId(), e);
                try {
                    wait(500);
                } catch (final InterruptedException e2) {
                    LOGGER.debug("Interrupted exception while waiting.");
                }

                try {
                    korSyncConnector.set("/" + kaInfo.getAgentId() + "/system/agentRegistryService"
                            + "/newGroupKey", node);
                } catch (final Exception e1) {
                    LOGGER.error(
                            "Could not send new groupKey to node {} in the second "
                                    + "try, storing it in a cache and trying again later.",
                            kaInfo.getAgentId(), e1);
                    groupKeyCache.put(kaInfo.getAgentId(), node);
                }
            }
        }
    }

    /**
     * Resends groupkeys that were cached earlier because they couldn't be delivered.
     */
    private void deliverCachedGroupKeys() {
        // remove outdated elements from the cache.
        final List<String> toRemove = new LinkedList<String>();
        for (final Entry<String, VslNode> entry : groupKeyCache.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue().getTimestamp().getTime() > 60000) {
                toRemove.add(entry.getKey());
            }
        }
        for (final String outdated : toRemove) {
            groupKeyCache.remove(outdated);
        }

        // Try to resend groupkeys.
        for (final Entry<String, VslNode> entry : groupKeyCache.entrySet()) {
            try {
                korSyncConnector.set(
                        "/" + entry.getKey() + "/system/agentRegistryService" + "/newGroupKey",
                        entry.getValue());
                LOGGER.debug("Group key set from cache.");
            } catch (final VslException e) {
                LOGGER.error("Could not send new groupKey to node {}", entry.getKey(), e);
            }
        }

    }

    /**
     * Helper sleeper method.
     *
     * @param time
     *            The time in milliseconds to sleep.
     * @throws InterruptedException
     *             Thrown if interrupted.
     */
    private void wait(final int time) throws InterruptedException {
        Thread.sleep(time);
    }

}
