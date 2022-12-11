package org.ds2os.vsl.agentregistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.config.VslAgentRegistryConfig;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for storing and accessing AlivePing and GroupKey information in the KOR.
 *
 * @author jay
 * @author Johannes Straßer
 * @author liebald
 * @author felix
 */
public class AgentRegistryService extends AbstractVslModule implements VslAgentRegistryService {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryService.class);

    /**
     * TODO To be changed. Leave at the moment.
     */
    private final VslParametrizedConnector connector;

    /**
     * The VslSymmetricKeyStore used by this instance.
     */
    private final VslSymmetricKeyStore keyStore;

    /**
     * Path for instance of the model rooted at agentRegistryService.
     */
    private final String servicePath;

    /**
     * The {@link VslNodeFactory} used to create {@link VslNode}s.
     */
    private final VslNodeFactory nodefactory;

    /**
     * Path for connected KA list in the model agentRegistryService.
     */
    private final String connectedKAPath;

    /**
     * Path for unconnected KA list in the model agentRegistryService.
     */
    private final String unConnectedKAPath;

    /**
     * Model ID to be adapted for the system service.
     */
    private static final String MODEL_ID = "/system/agentRegistryService";

    /**
     * The config of the local AgentRegistry.
     */
    private final VslAgentRegistryConfig config;

    /**
     * cleaner thread for stale agents.
     */
    private final AgentRegistryCleaner agentRegistryCleaner;

    /**
     * Service manifest object. At the moment, created dummy instance. TODO Use real manifest when
     * available
     */
    private static final VslServiceManifest DUMMY_MANIFEST = new VslServiceManifest() {

        @Override
        public String getModelId() {
            return MODEL_ID;
        }

        @Override
        public String getModelHash() {
            return "";
        }

        @Override
        public String getBinaryHash() {
            return "";
        }
    };

    /**
     * Public constructor.
     *
     * @param connector
     *            {@link VslConnector} object to register the service.
     * @param config
     *            The name of the local KA
     */
    public AgentRegistryService(final VslParametrizedConnector connector,
            final VslAgentRegistryConfig config) {
        this.connector = connector;
        this.keyStore = new SymmetricKeyStore();
        this.config = config;
        nodefactory = connector.getNodeFactory();
        servicePath = "/" + this.config.getAgentName() + "/system/agentRegistryService";
        connectedKAPath = servicePath + "/connectedKAs";
        unConnectedKAPath = servicePath + "/unconnectedKAs";
        agentRegistryCleaner = new AgentRegistryCleaner(config, this);

    }

    @Override
    public final void activate() {
        try {
            connector.registerService(DUMMY_MANIFEST);
        } catch (final VslException e) {
            LOGGER.debug("Error while registering the service. Error message : {}", e.getMessage());
        }

        try {
            connector.registerVirtualNode(servicePath + "/newGroupKey",
                    new GroupKeyNodeHandler(this));
        } catch (final VslException e) {
            LOGGER.error("The virtual node at {} could not be registered: {}",
                    servicePath + "/newGroupKey", e.getMessage());
        }
        agentRegistryCleaner.start();
    }

    @Override
    public final void shutdown() {
        /*
         * Data structures in the KOR are not removed by choice. They are to be persistent to
         * facilitate node restarts.
         */
        agentRegistryCleaner.interrupt();
        try {
            agentRegistryCleaner.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final boolean isAgentConnected(final String agentID) {
        try {
            final VslNode agentRecord = connector.get(connectedKAPath + "/" + agentID);
            if (agentRecord != null) {
                return true;
            }
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Could not find the agent ID : {} ", agentID);
            return false;
        } catch (final VslException e) {
            LOGGER.debug("Undefined exception while checking Agent ID. Error message: {}",
                    e.getMessage());
        }
        return false;
    }

    @Override
    public final boolean isGroupReachable(final String groupID, final int numKAsExpected) {
        int numKAs = 0;
        VslNode root;
        try {
            root = connector.get(this.unConnectedKAPath, new AddressParameters().withDepth(2));
            final String[] agentList = root.getChild("elements").getValue().split(";");
            for (final String agent : agentList) {
                if (root.getChild(agent).getChild("groupID").getValue().contentEquals(groupID)
                        && Integer.parseInt(
                                root.getChild(agent + "/numKAs").getValue()) == numKAsExpected) {
                    numKAs++;
                }
            }
        } catch (final VslException e) {
            LOGGER.warn("Could not read list of unconnected KAs: {}", e.getMessage());
            return false;
        }
        // LOGGER.error("expected: {} agents, currently: {}", numKAsExpected, numKAs);
        return numKAs >= numKAsExpected;
    }

    @Override
    public final boolean isLeader() {
        try {
            return connector.get(servicePath + "/isLeader").getValue().contentEquals("1");
        } catch (final VslException e) {
            LOGGER.error("Could not read isLeader from KOR: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public final String getLeader(final String groupID) {
        String result = "";
        VslNode agentRegistry;

        // Check the local ID, since it may not be stored as connected KA
        if (groupID.equals(getMulticastGroupKeyHash())
                && (result.isEmpty() || result.compareTo(config.getAgentName()) < 0)) {
            result = config.getAgentName();
        }
        try {
            agentRegistry = connector.get(servicePath, new AddressParameters().withDepth(-1));
            for (final String connectionType : new String[] { "unconnectedKAs", "connectedKAs" }) {
                final String[] agents = agentRegistry.getChild(connectionType + "/elements")
                        .getValue().split(";");
                for (final String agent : agents) {
                    if (agent.isEmpty()) {
                        continue;
                    }
                    if (agentRegistry.getChild(connectionType + "/" + agent + "/groupID").getValue()
                            .equals(groupID) && (result.isEmpty() || result.compareTo(agent) < 0)) {
                        result = agent;
                    }
                }
            }

        } catch (final VslException e1) {
            LOGGER.debug("Error retrieving the groupLeader for groupID {}: {}", groupID,
                    e1.getMessage());
        }
        return result;
    }

    @Override
    public final synchronized HashSet<VslTransportConnector> getTransports(final String agentID) {
        final HashSet<VslTransportConnector> connectors = new HashSet<VslTransportConnector>();
        VslNode transports = null;
        try {
            // Try to get transports from unconnected list
            transports = connector.get(unConnectedKAPath + "/" + agentID + "/supportedTransports",
                    new AddressParameters().withDepth(1));
        } catch (final NodeNotExistingException e) {
            try {
                // Try to get transports from connected list
                transports = connector.get(connectedKAPath + "/" + agentID + "/supportedTransports",
                        new AddressParameters().withDepth(1));
            } catch (final NodeNotExistingException e1) {
                LOGGER.debug("Could not get transports of agent {} with address {}: {}", agentID,
                        connectedKAPath + "/" + agentID + "/supportedTransports", e1.getMessage());
            } catch (final VslException e1) {
                LOGGER.error("Could not read transports from " + connectedKAPath + "/" + agentID
                        + ": {}", e1.getMessage());
            }
        } catch (final VslException e) {
            LOGGER.error(
                    "Could not read transports from " + unConnectedKAPath + "/" + agentID + ": {}",
                    e.getMessage());
        }

        if (transports != null) {
            final String elements = transports.getChild("elements").getValue();
            if (!elements.equals("")) {
                for (final String transport : elements.split(";")) {
                    connectors
                            .add(new TransportConnector(transports.getChild(transport).getValue()));
                }
            }
        }
        return connectors;
    }

    @Override
    public final synchronized String getAttribute(final String agentID,
            final String attributeName) {
        String attribute = null;
        try {
            // Try to get transports from unconnected list
            attribute = connector.get(unConnectedKAPath + "/" + agentID + "/" + attributeName)
                    .getValue();
        } catch (final NodeNotExistingException e) {
            try {
                // Try to get transports from connected list
                attribute = connector.get(connectedKAPath + "/" + agentID + "/" + attributeName)
                        .getValue();
            } catch (final NodeNotExistingException e1) {
                LOGGER.debug("Could not get attribute " + attributeName + " of agent " + agentID
                        + ": {}", e1.getMessage());
            } catch (final VslException e1) {
                LOGGER.error(
                        "Could not read attribute from " + connectedKAPath + "/" + agentID + ": {}",
                        e1.getMessage());
            }
        } catch (final VslException e) {
            LOGGER.error(
                    "Could not read attribute from " + unConnectedKAPath + "/" + agentID + ": {}",
                    e.getMessage());
        }
        return attribute;
    }

    @Override
    public final void storeAlivePingToConnectedKAs(final VslAlivePing receivedPing,
            final Date timestamp) {
        storeAlivePing(receivedPing, timestamp, this.connectedKAPath);

    }

    @Override
    public final void storeAlivePingToUnConnectedKAs(final VslAlivePing receivedPing,
            final Date timestamp) {
        storeAlivePing(receivedPing, timestamp, this.unConnectedKAPath);
    }

    /**
     * Updates the agentRecord in listPath using the given AlivePing and time stamp.
     *
     * @param receivedPing
     *            The Alive ping containing the new information
     * @param timestamp
     *            The time the AlivePing was received
     * @param listPath
     *            The path to the list of agentRecords
     */
    private synchronized void storeAlivePing(final VslAlivePing receivedPing, final Date timestamp,
            final String listPath) {
        try {

            final String rootNode = listPath + "/" + receivedPing.getAgentId();

            final VslMutableNode alivePingData = nodefactory.createMutableNode();

            /*
             * Update time stamp
             */
            alivePingData.putChild("timestamp",
                    nodefactory.createMutableNode(Long.toString(timestamp.getTime())));

            /*
             * Update groupID
             */
            alivePingData.putChild("groupID",
                    nodefactory.createMutableNode(receivedPing.getGroupID()));

            /*
             * Update korHash
             */
            alivePingData.putChild("korHash",
                    nodefactory.createMutableNode(receivedPing.getKorHash()));

            /*
             * Update numKAs
             */
            alivePingData.putChild("numKAs",
                    nodefactory.createMutableNode(Integer.toString(receivedPing.getNumKAs())));

            connector.set(rootNode, alivePingData);
            /*
             * Update transport information
             */
            final VslNode transports = connector.get(rootNode + "/supportedTransports",
                    new AddressParameters().withDepth(-1));
            // Store the transports (keys) and their addresses (values) we currently have
            final Map<String, String> storedTransports = new HashMap<String, String>();

            final String[] elements = transports.getChild("elements").getValue().split(";");
            if (!(elements.length == 1 && elements[0].isEmpty())) {
                for (int i = 0; i < elements.length; i++) {
                    storedTransports.put(transports.getChild(elements[i]).getValue(), elements[i]);
                }
            }

            final Collection<VslTransportConnector> connectors = receivedPing.getTransports();
            final Set<String> urlSet = new HashSet<String>(connectors.size());
            final Iterator<VslTransportConnector> connIterator = connectors.iterator();
            while (connIterator.hasNext()) {
                urlSet.add(connIterator.next().getURL());
            }

            // Compare received values with stored ones
            final Set<String> urlsToAdd = new HashSet<String>(urlSet);
            final Set<String> urlsToDelete = new HashSet<String>();

            for (final String storedURL : storedTransports.keySet()) {
                if (urlsToAdd.contains(storedURL)) {
                    urlsToAdd.remove(storedURL);
                } else {
                    urlsToDelete.add(storedURL);
                }
            }

            // Add new URLs
            final Iterator<String> additionIterator = urlsToAdd.iterator();
            while (additionIterator.hasNext()) {
                final String urlToAdd = additionIterator.next();
                // Add the transport to the transportList
                final String transportAddress = connector
                        .get(rootNode + "/supportedTransports" + "/add/system/transport")
                        .getValue();
                // Set the value
                connector.set(transportAddress, nodefactory.createImmutableLeaf(urlToAdd));
            }

            // Remove outdated URLs from the transportList
            final Iterator<String> deletionIterator = urlsToDelete.iterator();
            while (deletionIterator.hasNext()) {
                final String valueToDelete = deletionIterator.next();
                connector.get(rootNode + "/supportedTransports" + "/del/"
                        + storedTransports.get(valueToDelete));
            }

            /*
             * Check Size (removed for performance, assuming the operations went well. Otherwise the
             * next aliveping will update it.)
             */
            // transports = connector.get(rootNode + "/supportedTransports/elements");
            // int size;
            // if (transports.getValue().equals("")) {
            // size = 0;
            // } else {
            // size = transports.getValue().split(";").length;
            // }
            // if (size != urlSet.size()) {
            // LOGGER.warn("The transport list from a received AlivePing was not updated "
            // + "properly to the KOR. Length was " + size + " but should be "
            // + urlSet.size());
            // }

            LOGGER.debug("Successfully stored AlivePing in the list: " + listPath);

        } catch (final NodeNotExistingException e) {
            LOGGER.debug("AlivePing could not be stored in the KOR due to a non existing node: {}",
                    e.getMessage());
        } catch (final NodeAlreadyExistingException e) {
            LOGGER.debug(
                    "AlivePing could not be stored in the KOR due to already existing node : {}",
                    e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("AlivePing could not be stored in the KOR due to a VSLException : {}",
                    e.getMessage());
        }
    }

    @Override
    public final synchronized void addAgentToUnconnectedKAList(final String agentID,
            final Date timestamp) {
        addAgentToKAList(agentID, timestamp, new Date(0L), this.unConnectedKAPath);
    }

    @Override
    public final synchronized void addAgentToConnectedKAList(final String agentID,
            final Date timestamp, final Date certificateExpiration) {
        // TODO: synchronize access on connected/unconnected Agent lists

        // Check if we are still the leader
        if (config.getAgentName().compareTo(agentID) < 0) {
            try {
                connector.set(servicePath + "/isLeader", nodefactory.createImmutableLeaf("0"));
                LOGGER.debug("The KA " + agentID + " is the new leader of the multicast group.");
            } catch (final VslException e) {
                LOGGER.error("Could not set isLeader: {}", e.getMessage());
            }
        }

        // Add agent to connected list
        addAgentToKAList(agentID, timestamp, certificateExpiration, this.connectedKAPath);
        // TODO: it can happen that an agent is added to connected and unconnected at the same time
        // (until the timeout for the unconnected entry happens)
        // Try to remove agent from unconnected list
        try {
            connector.get(this.unConnectedKAPath + "/del/" + agentID);
        } catch (final VslException e) {
            LOGGER.debug("Generic VslException occurred while removing an AgentRecord"
                    + " from the unconnected KA list. Error message: {}", e.getMessage());
        }
    }

    /**
     * Creates a new agentRecord at the specified path. The record is filled with the given
     * parameters.
     *
     * @param agentID
     *            The ID of the new agent
     * @param timestamp
     *            The time the causing AlivePing was received
     * @param certificateExpiration
     *            The expiration date of the new agents certificate
     * @param listPath
     *            The path to the list of agentRecords
     */
    private synchronized void addAgentToKAList(final String agentID, final Date timestamp,
            final Date certificateExpiration, final String listPath) {
        // Try to add KA to list
        try {
            connector.get(listPath + "/add/system/agentRecord//" + agentID);
        } catch (final NodeAlreadyExistingException e) {
            LOGGER.debug("Tried adding an agent to {}, but it already existed: {}",
                    listPath + "/" + agentID, e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Failed to add new agentRecord to the list {} due to a VSLException: {} ",
                    listPath, e.getMessage());
        }

        // Update values (Regardless if the KA was on this list before or not)
        try {

            final VslMutableNode node = nodefactory.createMutableNode();

            node.putChild("agentID", nodefactory.createMutableNode(agentID));
            node.putChild("timestamp",
                    nodefactory.createMutableNode(Long.toString(timestamp.getTime())));
            // FIXME: unsafe!
            node.putChild("certificateExpiration",
                    nodefactory.createMutableNode(certificateExpiration.toString()));
            connector.set(listPath + "/" + agentID, node);
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Failed to update an agentRecord on the list {}"
                    + " due to the record not existing: {} ", listPath, e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug(
                    "Failed to update an agentRecord on the list {} due to a VSLException: {} ",
                    listPath, e.getMessage());
        }

    }

    @Override
    public final String getMulticastGroupKeyHash() {
        try {
            final VslNode currentKeyHash = connector.get(servicePath + "/currentMulticastGroupKey");
            return currentKeyHash.getValue();
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Current key not set. Error message: {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. Error message: {}", e.getMessage());
        }
        return "";
    }

    @Override
    public final String[] getAllMulticastGroupKeyHashes() {
        String[] result = null;
        try {
            result = connector.get(servicePath + "multicastGroupKeys/elements").getValue()
                    .split(";");
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Node not existing. {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. {}", e.getMessage());
        }
        return result;
    }

    @Override
    public final String getMulticastGroupKey(final String hash) {
        try {
            final VslNode key = connector
                    .get(servicePath + "/multicastGroupKeys/" + hash + "/keyData");
            return key.getValue();
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("No key with this hash was found. Error message: {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. Error message: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public final String getMulticastGroupKeyString(final String hash) {
        try {
            final VslNode key = connector
                    .get(servicePath + "/multicastGroupKeys/" + hash + "/keyString");
            return key.getValue();
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("No key with this hash was found. Error message: {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. Error message: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public final synchronized void setMulticastGroupKey(final String receivedMulticastKey,
            final String keyHash, final String keyString) {
        try {
            connector.get(servicePath + "/multicastGroupKeys/add/system/groupKey//" + keyHash);
            connector.set(servicePath + "/multicastGroupKeys/" + keyHash + "/keyData",
                    nodefactory.createImmutableLeaf(receivedMulticastKey));
            connector.set(servicePath + "/multicastGroupKeys/" + keyHash + "/keyString",
                    nodefactory.createImmutableLeaf(keyString));
            connector.set(servicePath + "/currentMulticastGroupKey",
                    nodefactory.createImmutableLeaf(keyHash));
            LOGGER.info("Key with hash " + keyHash + "was stored.");
        } catch (final NodeAlreadyExistingException e) {
            LOGGER.debug("A key with this hash is already in the KOR: {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. Error message: {} ", e.getMessage());
        }
    }

    @Override
    public final int getNetworkSize() {
        try {
            final String networkSize = connector.get(connectedKAPath + "/elements").getValue();
            // +1 for the local KA, which isn't part of the connectedKAs list
            if (networkSize != null) {
                if (networkSize.isEmpty()) {
                    return 1;
                }
                return networkSize.split(";").length + 1;
            }
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Node not existing. {}", e.getMessage());
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found. {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public final String[] getConnectedAgentIds() {
        String[] agentList = new String[0];
        try {
            agentList = connector.get(connectedKAPath + "/elements").getValue().split(";");
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Node not existing:", e);
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found:", e);
        }
        return agentList;
    }

    @Override
    public final String[] getUnConnectedAgentIds() {
        String[] agentList = new String[0];
        try {
            final String elements = connector.get(unConnectedKAPath + "/elements").getValue();
            if (!elements.isEmpty()) {
                agentList = elements.split(";");
            }
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("Node not existing:", e);
        } catch (final VslException e) {
            LOGGER.debug("Unhandled exception found:", e);
        }
        return agentList;
    }

    @Override
    public final VslSymmetricKeyStore getKeyStore() {
        return this.keyStore;
    }

    @Override
    public final Collection<VslKAInfo> getConnectedKAInfo() {
        final Set<VslKAInfo> allKAInfo = new HashSet<VslKAInfo>();
        try {
            final VslNode allConnectedKAs = connector.get(connectedKAPath,
                    new AddressParameters().withDepth(-1));
            for (final Map.Entry<String, VslNode> entry : allConnectedKAs.getDirectChildren()) {
                if ("add".equals(entry.getKey()) || "del".equals(entry.getKey())
                        || "elements".equals(entry.getKey())) {
                    continue;
                }
                allKAInfo.add(new AgentRecord(entry.getValue()));
            }
        } catch (final NodeNotExistingException e) {
            LOGGER.error("Node not existing:", e);
        } catch (final VslException e) {
            LOGGER.error("Unhandled exception found:", e);
        }
        return allKAInfo;
    }

    @Override
    public final synchronized void cleanStaleAgents(final long timeout) {
        final Date cutoff = new Date(System.currentTimeMillis() - timeout);
        for (final String listType : new String[] { connectedKAPath, unConnectedKAPath }) {
            VslNode listRoot;
            try {
                listRoot = connector.get(listType, new AddressParameters().withDepth(2));
                final String[] agents = listRoot.getChild("elements").getValue().split(";");
                for (final String agent : agents) {
                    if (agent.isEmpty()) {
                        continue;
                    }
                    if (new Date(Long.decode(listRoot.getChild(agent + "/timestamp").getValue()))
                            .before(cutoff)) {
                        try {
                            connector.get(listType + "/del/" + agent);

                            LOGGER.debug("Purging stale agent {}", agent);
                        } catch (final VslException e) {
                            LOGGER.error("Could not delete element {} from list {}: {}", agent,
                                    listType, e);
                        }
                    }
                }
            } catch (final VslException e) {
                LOGGER.error("Could not get elements of list {}: {}", listType, e);
            }
        }

        // Reset isLeader if necessary
        if (config.getAgentName().contentEquals(this.getLeader(this.getMulticastGroupKeyHash()))) {
            try {
                connector.set(servicePath + "/isLeader", nodefactory.createImmutableLeaf("1"));
                LOGGER.debug("This agent is the leader.");
            } catch (final VslException e) {
                LOGGER.error("Could not set /isLeader: {}", e);
            }
        }
    }

    @Override
    public final void cleanDoubleConnectedAgents() {
        final List<String> connected = Arrays.asList(getConnectedAgentIds());
        final String[] unconnected = getUnConnectedAgentIds();
        for (int i = 0; i < unconnected.length; i++) {
            if (connected.contains(unconnected[i])) {
                try {
                    LOGGER.error("Purging double stored agent {}", unconnected[i]);
                    connector.get(unConnectedKAPath + "/del/" + unconnected[i]);

                } catch (final VslException e) {
                    LOGGER.error("Could not delete element {} from list {}", unconnected[i],
                            "unconnected", e);
                }
            }
        }

    }
}
