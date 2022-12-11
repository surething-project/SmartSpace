package org.ds2os.vsl.kor;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.config.VslInitialConfig;
import org.ds2os.vsl.core.config.VslKORConfig;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.node.*;
import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.*;
import org.ds2os.vsl.kor.config.InterMediateConfig;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.ds2os.vsl.kor.lists.ListAddHandler;
import org.ds2os.vsl.kor.lists.ListDelHandler;
import org.ds2os.vsl.kor.lists.ListRootHandler;
import org.ds2os.vsl.kor.locking.Locker;
import org.ds2os.vsl.kor.locking.VslLocker;
import org.ds2os.vsl.kor.structureLogger.StructureLogger;
import org.ds2os.vsl.modelCache.ModelCache;
import org.ds2os.vsl.modelCache.VslModelCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class implements the Knowledge Object Register (KOR). The KOR holds the virtual
 * representation of the world the agent controls and manages. The addressing is done: "
 * /KA_ID/service_ID/innerNode/.../Node
 *
 * @author borchers
 * @author liebald
 */
public class KnowledgeRepository extends AbstractVslModule implements VslKnowledgeRepository {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeRepository.class);

    /**
     * The name of the local KA.
     */
    private final String localID;

    /**
     * The {@link VslTypeSearchProvider} used for the typeSearch.
     */
    private final VslTypeSearchProvider typeSearchProvider;

    /**
     * The {@link VslLocker} Object that takes care of locking.
     */
    private final VslLocker locker;

    /**
     * The {@link ModelCache} used for this KOR.
     */
    private final VslModelCache modelCache;

    /**
     * The backing NodeTree instance.
     */
    private final VslNodeTree myNodeTree;

    /**
     * The {@link StructureLogger} for the KOR which logs updates on the local KOR structure
     * (add/del nodes).
     */
    private final StructureLogger structureLog;

    /**
     * The {@link VslVirtualNodeManager} used by the KOR to handle virtualNodes.
     */
    private final VslVirtualNodeManager virtualNodeManager;

    /**
     * The configuration service.
     */
    private VslKORConfig configService;

    /**
     * The {@link VslKORSyncHandler} used to send incremental updates.
     */
    private VslKORSyncHandler korSyncHandler = null;

    /**
     * The {@link VslNodeDatabase} used for storing data.
     */
    private final VslNodeDatabase myNodeDatabase;

    /**
     * The {@link VslSubscriptionManager} the KOR will use to handle subscriptions.
     */
    private final VslSubscriptionManager subscriptionManager;

    /**
     * The {@link VslStatisticsProvider} for accessing the KA internal statistics mechanism.
     */
    private final VslStatisticsProvider statisticsProvider;

    /**
     * The {@link VslNodeFactory} used to create different types of VslNodes.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * Initializes the KOR.
     *
     * @param newVslVirtualNodeManager
     *            The {@link VslVirtualNodeManager} that the KOR will use in order to handle
     *            virtualNodes.
     * @param newSubscriptionManager
     *            The {@link VslSubscriptionManager} the KOR will use to handle subscriptions.
     * @param initialConfig
     *            The initial configuration.
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} for accessing the KA internal statistics
     *            mechanism.
     * @param typeSearch
     *            The {@link VslTypeSearchProvider} that must be updated when the VSL structur
     *            changes.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create different types of VslNodes.
     */
    public KnowledgeRepository(final VslVirtualNodeManager newVslVirtualNodeManager,
            final VslSubscriptionManager newSubscriptionManager,
            final VslInitialConfig initialConfig, final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearch, final VslNodeFactory nodeFactory) {
        this(newVslVirtualNodeManager, newSubscriptionManager, null, null, initialConfig, null,
                statisticsProvider, typeSearch, nodeFactory);
    }

    /**
     * Initializes the KOR. Parameters mostly for unit tests. For integration use the other
     * constructor.
     *
     * @param newVslVirtualNodeManager
     *            The {@link VslVirtualNodeManager} that the KOR will use in order to handle
     *            virtualNodes.
     * @param newSubscriptionManager
     *            The {@link VslSubscriptionManager} the KOR will use to handle subscriptions.
     * @param newNodeTree
     *            {@link VslNodeTree} instance to use for this KOR. Should only be specified for
     *            unit tests.
     * @param newModelCache
     *            {@link VslModelCache} instance to use for this KOR. Should only be specified for
     *            unit tests.
     * @param initialConfig
     *            The initial configuration.
     * @param newNodeDatabase
     *            {@link VslNodeDatabase} instance to use for this KOR. Should only be specified for
     *            unit test.
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} for accessing the KA internal statistics
     *            mechanism.
     * @param typeSearch
     *            The {@link VslTypeSearchProvider} that must be updated when the VSL structur
     *            changes.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create different types of VslNodes.
     */
    public KnowledgeRepository(final VslVirtualNodeManager newVslVirtualNodeManager,
            final VslSubscriptionManager newSubscriptionManager, final VslNodeTree newNodeTree,
            final VslModelCache newModelCache, final VslInitialConfig initialConfig,
            final VslNodeDatabase newNodeDatabase, final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearch, final VslNodeFactory nodeFactory) {
        typeSearchProvider = typeSearch;
        this.nodeFactory = nodeFactory;
        configService = new InterMediateConfig(initialConfig);
        localID = configService.getAgentName();
        this.statisticsProvider = statisticsProvider;
        if (newNodeDatabase == null) {
            if ("mongodb".equals(configService.getDatabaseType())) {
                myNodeDatabase = new MongodbDatabase(configService);
            } else {
                myNodeDatabase = new HSQLDatabase(configService, statisticsProvider);
            }
        } else {
            myNodeDatabase = newNodeDatabase;
        }
        locker = new Locker(myNodeDatabase, configService);
        if (newNodeTree == null) {
            myNodeTree = new NodeTree(localID, myNodeDatabase, locker, nodeFactory);
        } else {
            myNodeTree = newNodeTree;
        }
        if (newModelCache == null) {
            modelCache = ModelCache.getInstance();
        } else {
            modelCache = newModelCache;
        }
        virtualNodeManager = newVslVirtualNodeManager;
        subscriptionManager = newSubscriptionManager;
        structureLog = new StructureLogger(localID);
    }

    @Override
    public final void activate() throws Exception {
        myNodeDatabase.activate();
        locker.activate();
        myNodeTree.activate();
        registerLists("/" + localID); // initialize all lists that eventually already exists in a
        // persistent Database (re-register as virtual)
        structureLog
                .activate(myNodeDatabase.getHashOfSubtree("/" + localID, Arrays.asList("system")));
        updateTypeSearchProvider("/", false);
    }

    @Override
    public final void activate(final VslKORConfig newConfigService, final VslConnector con)
            throws Exception {
        activate();
        this.configService = newConfigService;
        modelCache.setConnector(con);
    }

    @Override
    public final void shutdown() {
        myNodeDatabase.shutdown();
    }

    @Override
    public final synchronized void addSubtreeFromModelID(final String fullAddress,
            final String modelID, final String creatorID, final String rootNodeName)
            throws ParentNotExistingException, NoPermissionException, RequiredDataMissingException,
            ModelNotFoundException, NodeNotExistingException, NodeAlreadyExistingException,
            InvalidModelException {

        final VslStatisticsDatapoint statistic = statisticsProvider
                .getStatistics(this.getClass(), "addSubtree").begin();
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(fullAddress);

        final LinkedHashMap<String, InternalNode> model = modelCache.getCompleteModelNodes(modelID,
                rootNodeName);
        // LOGGER.debug("addModel: {} {} {} {} {}", fullAddress, modelID, creatorID, rootNodeName,
        // wellFormedAddress);
        // A hashmap to store the added nodes of type /basic/list in order to register them with the
        // correct virtual node handlers after adding them to the tree. Also stores the
        // restrictions, which are needed.
        final HashMap<String, String> listNodeRestrictions = new HashMap<String, String>();
        final HashMap<String, List<String>> listNodesWriteIds = new HashMap<String, List<String>>();
        // check if the node already exists
        final String rootNodeAddress = wellFormedAddress + "/" + rootNodeName;
        if (myNodeDatabase.nodeExists(rootNodeAddress)) {
            LOGGER.info("Tried to add a new model at an already existing address");
            statistic.end();
            throw new NodeAlreadyExistingException(
                    "There already exists a node at " + rootNodeAddress);
        }
        final String oldHash = getCurrentKORHash();

        final VslMutableNode initialValues = nodeFactory.createMutableNode("");
        try {
            for (final Entry<String, InternalNode> s : model.entrySet()) {
                // LOGGER.debug(wellFormedAddress + "/" + s.getKey() + ": " + s.getValue());
                final String address = wellFormedAddress + "/" + s.getKey();

                myNodeTree.addNode(address, s.getValue().getType(), s.getValue().getReaderIDs(),
                        s.getValue().getWriterIDs(), s.getValue().getRestriction(),
                        s.getValue().getCacheParameters(), creatorID);

                if (address.equals(rootNodeAddress)) {
                    initialValues.setValue(s.getValue().getValue());
                } else {
                    final String relativeAddress = address.substring(rootNodeAddress.length() + 1);
                    String value = s.getValue().getValue();
                    if (value == null) {
                        value = "";
                    }
                    initialValues.putChild(relativeAddress, nodeFactory.createMutableNode(value));
                }
                // log that the structure changed.
                if ((address.startsWith("/" + localID + "/") || address.equals("/" + localID))
                        && !address.startsWith("/" + localID + "/system/")) {
                    // LOGGER.debug("structure: {}", address);
                    structureLog.logChangedAddress(address);
                }
                if (s.getValue().getType().contains("/basic/list")) {
                    listNodeRestrictions.put(wellFormedAddress + "/" + s.getKey(),
                            s.getValue().getRestriction());
                    listNodesWriteIds.put(wellFormedAddress + "/" + s.getKey(),
                            s.getValue().getWriterIDs());
                }
            }
            // for each list register the Handlers for the root node and the add and del nodes.
            for (final Entry<String, String> listAddress : listNodeRestrictions.entrySet()) {
                registerList(listAddress.getKey(), creatorID, listAddress.getValue(),
                        listNodesWriteIds.get(listAddress.getKey()));
            }
            // set the initial Values if given
            myNodeTree.setValue(wellFormedAddress + "/" + rootNodeName,
                    VslNodeTree.SYSTEM_USER_IDENTITY, initialValues);

            // after adding a new subtree update the current structure hash.
            if ((wellFormedAddress.startsWith("/" + localID + "/")
                    || wellFormedAddress.equals("/" + localID))
                    && !wellFormedAddress.startsWith("/" + localID + "/system/")) {
                structureLog.newLogpointHash(
                        myNodeDatabase.getHashOfSubtree("/" + localID, Arrays.asList("system")));
                triggerIncrementalKorUpdate(oldHash);
            }
            // update the typeSearchProvider
            updateTypeSearchProvider(wellFormedAddress, false);
        } catch (final Exception e) {
            LOGGER.error("ERROR when adding model to KOR: model {} at address {}", modelID,
                    wellFormedAddress, e);
            // remove all nodes under and including the root node of this model
            // we don't need to update the structure log, either nodes weren't added or they are
            // already stored as changed.
            removeNode(wellFormedAddress + "/" + model.entrySet().iterator().next().getKey());
        }
        // LOGGER.debug("Model:{} Hash: {}, {}", fullAddress + rootNodeName,
        // structureLog.getCurrentLogHash(), getCurrentKORHashOf("/"+localID));
        statistic.end();
    }

    /**
     * Updates the VslTypeSearchProvide with new addresses or removes deleted ones.
     *
     * @param address
     *            The address to update.
     * @param remove
     *            True if the node should be removed, false if it should be added.
     */
    private void updateTypeSearchProvider(final String address, final boolean remove) {

        if (!remove) {
            try {
                typeSearchProvider.addTypes(address, getStructureNodeFor(address, false));
            } catch (final NodeNotExistingException e) {
                LOGGER.debug("Couldn't find node for updating the TypeSearch: {}", address);
                typeSearchProvider.removeAddress(address);
            }
        } else {
            typeSearchProvider.removeAddress(address);
        }
    }

    /**
     * Helper function to broad-/multicast updates on the local KOR structure.
     *
     * @param fromHash
     *            The hash from where the incremental update should start.
     */
    private void triggerIncrementalKorUpdate(final String fromHash) {
        if (korSyncHandler == null) {
            return;
        }
        final VslKORUpdate update = getKORUpdateFromHash(fromHash);
        if (update.getHashFrom().isEmpty()) {
            return; // we don't want to send full updates, shouldn't happen anyways.
        }
        korSyncHandler.sendIncrementalUpdate(update);
    }

    // @Override
    // public final void applyKORUpdate(final List<VslKORUpdate> updates) {
    // for (final VslKORUpdate update : updates) {
    // applyKORUpdate(update);
    // }
    // }

    @Override
    public final void applyKORUpdate(final VslKORUpdate update) {
        LOGGER.debug("applying update for {}, I am {}", update.getAgentName(), localID);

        // add the new nodes
        for (final Entry<String, VslStructureNode> node : update.getAddedNodes().entrySet()) {
            if (node.getKey().startsWith("/" + localID + "/")) {
                continue;
            }
            // first we need to make sure they don't exist already, and if so delete them (childs
            // may
            // have changed, e.g. a list element with the same address but another type.)
            removeNodeWithoutException(node.getKey());

            try {
                myNodeTree.addNode(node.getKey(), node.getValue().getTypes(),
                        node.getValue().getReaderIds(), node.getValue().getWriterIds(),
                        node.getValue().getRestrictions(), node.getValue().getCacheParameters(),
                        node.getValue().getWriterIds().get(0));
                LOGGER.debug("added node: {}", node.getKey());
                for (final Entry<String, VslStructureNode> entry : node.getValue()
                        .getAllChildren()) {
                    myNodeTree.addNode(node.getKey() + "/" + entry.getKey(),
                            entry.getValue().getTypes(), entry.getValue().getReaderIds(),
                            entry.getValue().getWriterIds(), entry.getValue().getRestrictions(),
                            node.getValue().getCacheParameters(),
                            entry.getValue().getWriterIds().get(0));
                    LOGGER.debug("added node: {}", node.getKey() + "/" + entry.getKey());

                }
                // update the VslTypeSearchProvider
                updateTypeSearchProvider(node.getKey(), false);
            } catch (final Exception e) {
                LOGGER.debug("update failed to add a node: {}, error: {}", node.getKey(),
                        e.getMessage());
                removeNodeWithoutException(node.getKey());
            }
        }

        // remove all nodes that were flagged for removal
        for (final String address : update.getRemovedNodes()) {
            // ignore updates for the local ka
            if (address.startsWith("/" + localID + "/")) {
                continue;
            }
            removeNodeWithoutException(address);
        }
        LOGGER.debug("{} hash: {}, {} hash: {}", localID, getCurrentKORHash(),
                update.getAgentName(), getCurrentKORHashOf("/" + update.getAgentName()));
    }

    @Override
    public final void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        locker.commitSubtree(address, identity);
    }

    @Override
    public final VslNode get(final String address, final VslIdentity identity) throws VslException {
        VslNode originalQuery = null;
        String wellFormedAddress = AddressParser.makeWellFormedAddress(address);

        // for debugging only, e.g. get /agent1/-agent2 should return the structure on agent2
        // stored on the local agent1
        if (wellFormedAddress.startsWith("/" + localID + "/-")) {
            wellFormedAddress = "/"
                    + wellFormedAddress.substring(wellFormedAddress.indexOf("-") + 1);
            LOGGER.debug("Retrieve structure of {} on {}", wellFormedAddress, localID);
        }
        // If no access permission on initial get, the exception won't be stopped and simply
        // handed to the issuer of the get request.
        try {

            originalQuery = myNodeTree.get(wellFormedAddress, new AddressParameters(), identity);

        } catch (final NodeNotExistingException e) {

            // Node doesn't exist, check for virtual parent and ask his handler.
            final String virtualParent = virtualNodeManager.getFirstVirtualParent(address);
            if (virtualParent == null) {
                throw new NodeNotExistingException(e.getMessage());
            }

            // test access rights on virtual rootNode
            myNodeTree.get(virtualParent, new AddressParameters(), identity);
            return virtualNodeManager.getVirtualNodeHandler(virtualParent).get(address,
                    new AddressParameters(), identity);
        }
        // Access was ok, check if the node is virtual and do the callback if yes.
        // overwrites the original value from the KOR.
        if (virtualNodeManager.isVirtualNode(address)) {
            return virtualNodeManager.getVirtualNodeHandler(address).get(address,
                    new AddressParameters(), identity);
        }
        return originalQuery;
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        VslNode originalQuery = null;
        String wellFormedAddress = AddressParser.makeWellFormedAddress(address);

        // for debugging only, e.g. get /agent1/-agent2 should return the structure on agent2
        // stored on the local agent1
        if (wellFormedAddress.startsWith("/" + localID + "/-")) {
            wellFormedAddress = "/"
                    + wellFormedAddress.substring(wellFormedAddress.indexOf("-") + 1);
            LOGGER.debug("Retrieve structure of {} on {}", wellFormedAddress, localID);
        }
        // If no access permission on initial get, the exception won't be stopped and simply
        // handed to the issuer of the get request.
        try {
            originalQuery = myNodeTree.get(wellFormedAddress, params, identity);
        } catch (final NodeNotExistingException e) {

            // Node doesn't exist, check for virtual parent and ask his handler.
            final String virtualParent = virtualNodeManager.getFirstVirtualParent(address);
            if (virtualParent == null) {
                throw new NodeNotExistingException(e.getMessage());
            }

            // test access rights on virtual rootNode
            myNodeTree.get(virtualParent, params, identity);
            return virtualNodeManager.getVirtualNodeHandler(virtualParent).get(address, params,
                    identity);
        }
        // Access was ok, check if the node is virtual and do the callback if yes.
        // overwrites the original value from the KOR.
        if (virtualNodeManager.isVirtualNode(address)) {
            return virtualNodeManager.getVirtualNodeHandler(address).get(address, params, identity);
        }
        return originalQuery;
    }

    @Override
    public final String getCurrentKORHash() {
        return structureLog.getCurrentLogHash();
    }

    @Override
    public final String getCurrentKORHashOf(final String kaAddress) {
        if (!kaAddress.startsWith("/")) {
            return myNodeDatabase.getHashOfSubtree("/" + kaAddress, Arrays.asList("system"));
        } else {
            return myNodeDatabase.getHashOfSubtree(kaAddress, Arrays.asList("system"));
        }
    }

    @Override
    public final VslKORUpdate getKORUpdateFromHash(final String hashFrom) {
        structureLog.newLogpointHash(
                myNodeDatabase.getHashOfSubtree("/" + localID, Arrays.asList("system")));

        final String to = structureLog.getCurrentLogHash();
        final List<String> changedAddresses = structureLog.getChangeLogSincehash(hashFrom);

        final Set<String> removedNodes = new HashSet<String>();
        final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
        if (changedAddresses.size() == 0) {
            // if nothing changed, return an empty update.
            return new KORUpdate(hashFrom, to, addedNodes, removedNodes, localID);
        }
        String from = hashFrom;
        if (changedAddresses.get(0).equals("/" + localID)) {
            from = "";
        } else {
            from = hashFrom;
        }

        for (final Iterator<String> iterator = changedAddresses.iterator(); iterator.hasNext();) {
            final String address = iterator.next();
            VslStructureNodeImpl currentNode = null;
            try {
                currentNode = getStructureNodeFor(address, true);
                addedNodes.put(address, currentNode);
            } catch (final NodeNotExistingException e) {
                removedNodes.add(address);
            }
        }
        return new KORUpdate(from, to, addedNodes, removedNodes, localID);
    }

    /**
     * Helper function to get the VslStructureNode for a specific address. The /agent/system subtree
     * is ignored.
     *
     * @param address
     *            The root Address for which the StructureNode should be created.
     * @param excludeSystemSubtree
     *            If true the system Subtree is stripped from the results.
     * @return The VslStructureNode with the structure information.
     * @throws NodeNotExistingException
     *             Thrown if there is no node at the given address.
     */
    private VslStructureNodeImpl getStructureNodeFor(final String address,
            final boolean excludeSystemSubtree) throws NodeNotExistingException {
        VslStructureNodeImpl currentNode = null;
        for (final Entry<String, MetaNode> entry : myNodeTree.getMetaData(address, true)
                .entrySet()) {
            if (excludeSystemSubtree && entry.getKey().matches("^/[a-zA-Z0-9]+/system/.*")) {
                continue;
            }

            if (entry.getKey().equals(address)) {
                currentNode = new VslStructureNodeImpl(entry.getValue().getReaderIDs(),
                        entry.getValue().getWriterIDs(), entry.getValue().getRestriction(),
                        entry.getValue().getType(), entry.getValue().getCacheParameters());
            } else {
                int sub = 1;
                if (address.equals("/")) {
                    sub = 0;
                }
                currentNode.putChild(entry.getKey().substring(address.length() + sub),
                        new VslStructureNodeImpl(entry.getValue().getReaderIDs(),
                                entry.getValue().getWriterIDs(), entry.getValue().getRestriction(),
                                entry.getValue().getType(), entry.getValue().getCacheParameters()));
            }
        }
        if (currentNode == null) {
            throw new NodeNotExistingException("No structure data at " + address + " found.");
        }
        return currentNode;
    }

    @Override
    public final VslNodeTree getVslNodeTree() {
        return myNodeTree;
    }

    @Override
    public final void lockSubtree(final String address, final VslLockHandler lockHandler,
            final VslIdentity identity) throws AlreadyLockedException, VslException {
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);
        final MetaNode meta = myNodeTree.getMetaData(wellFormedAddress, false)
                .get(wellFormedAddress);
        final Set<String> accessIds = new HashSet<>(meta.getWriterIDs());
        accessIds.addAll(meta.getReaderIDs());
        locker.lockSubtree(wellFormedAddress, identity, new ArrayList<>(accessIds), lockHandler);
    }

    @Override
    public final void notify(final String address, final VslIdentity identity) throws VslException {
        // TODO: check access rights for identity?
        subscriptionManager.notifySubscribers(address);
    }

    /**
     * Registers the given address as List with the correct handlers as virtualNode (Rootnode, add,
     * del).
     *
     * @param address
     *            The address of the List
     * @param creatorID
     *            The creator of the ListRootNode.
     * @param restrictions
     *            The restrictions specified for the list.
     * @param accessIDs
     *            The IDs that can add new nodes.
     */
    private void registerList(final String address, final String creatorID,
            final String restrictions, final List<String> accessIDs) {
        // nothing to do if the list is already registered.
        final VslIdentity id = new VslIdentity() {

            @Override
            public boolean isKA() {
                return false;
            }

            @Override
            public String getClientId() {
                return creatorID;
            }

            @Override
            public Collection<String> getAccessIDs() {
                return accessIDs;
            }
        };
        try {
            registerVirtualNode(address, new ListRootHandler(address, this, nodeFactory), id);
        } catch (final NodeAlreadyVirtualException e) {
            LOGGER.error("Tried to register a Listnode as virtual that was already registered: {}",
                    address);
        } catch (final VslException e) {
            LOGGER.error("Exception on registring ListRootHandler:", e);
        }
        try {
            registerVirtualNode(address + "/add", new ListAddHandler(address, this, creatorID,
                    restrictions, accessIDs, nodeFactory), id);
        } catch (final NodeAlreadyVirtualException e) {
            LOGGER.error("Tried to register a Listnode as virtual that was already registered: {}",
                    address);
        } catch (final VslException e) {
            LOGGER.error("Exception on registring ListAddHandler:", e);
        }
        try {
            registerVirtualNode(address + "/del",
                    new ListDelHandler(address, this, creatorID, restrictions, nodeFactory), id);
        } catch (final NodeAlreadyVirtualException e) {
            LOGGER.error("Tried to register a Listnode as virtual that was already registered: {}",
                    address);
        } catch (final VslException e) {
            LOGGER.error("Exception on registring ListDelHandler:", e);
        }

    }

    /**
     * Initializes all lists already stored in the KOR on startup, re-registering them as
     * virtualNodes.
     *
     * @param rootAddress
     *            search for nodes which are lists below this address and re-register them.
     */
    private void registerLists(final String rootAddress) {
        final List<String> lists = myNodeDatabase.getAddressesOfType(rootAddress, "/basic/List");
        for (final String address : lists) {
            try {
                final MetaNode rootNode = myNodeTree.getMetaData(address, false).get(address);
                registerList(address, rootNode.getWriterIDs().get(0), rootNode.getRestriction(),
                        rootNode.getWriterIDs());
            } catch (final NodeNotExistingException e) {
                LOGGER.error("Tried to register non existent node as List: {}", address);
            }
        }
    }

    @Override
    public final String registerService(final VslServiceManifest manifest,
            final VslIdentity identity) throws VslException {
        final String modelId = manifest.getModelId();
        if (modelId == null || "".equals(modelId)) {
            // no model, no instantiation. Return service "home" being the whole agent.
            return "/" + localID;
        }
        try {
            addSubtreeFromModelID("/" + localID, modelId, identity.getClientId(),
                    identity.getClientId());
            LOGGER.debug("registered Service {} for id {}", modelId, identity.getClientId());
        } catch (final NodeAlreadyExistingException e) {
            LOGGER.debug("re-registered Service {} for id {}", modelId, identity.getClientId());
            // if the node already exists, the service re-registered itself.
            // on re-registration, make sure all lists work again.
            registerLists("/" + localID + "/" + identity.getClientId());
        }
        return "/" + localID + "/" + identity.getClientId();
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler, final VslIdentity identity)
            throws VslException {
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);
        // TODO check if node exists
        final String creator = myNodeTree.getNodeCreatorId(wellFormedAddress);
        if (identity.getClientId().contains(creator)) {
            virtualNodeManager.registerVirtualNode(wellFormedAddress, virtualNodeHandler);
        } else {
            throw new NoPermissionException(
                    "Couldn't register VirtualNode for " + identity.getClientId()
                            + ", only the creator of the node can do this: " + creator);
        }
    }

    @Override
    public final void removeNode(final String address) throws NodeNotExistingException {
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);

        myNodeTree.removeNode(wellFormedAddress);
        // update the structure log
        if ((wellFormedAddress.startsWith("/" + localID + "/")
                || wellFormedAddress.equals("/" + localID))
                && !wellFormedAddress.startsWith("/" + localID + "/system/")) {
            // LOGGER.debug("removed {}", address);
            structureLog.logChangedAddress(wellFormedAddress);
            structureLog.newLogpointHash(
                    myNodeDatabase.getHashOfSubtree("/" + localID, Arrays.asList("system")));
        }
        // update the VslTypeSearchProvider
        updateTypeSearchProvider(wellFormedAddress, true);
    }

    /**
     * Helper method for KORupdate handling. Removes the given node and its subtree if they exist.
     * Otherwise nothing happens.
     *
     * @param address
     *            The address of the node to remove.
     */
    private void removeNodeWithoutException(final String address) {
        try {
            // Remove anything that may be on this position already.
            removeNode(address);
        } catch (final NodeNotExistingException e) {
            LOGGER.debug("An KORupdate tried to remove an non existent address: {}", address);
        }
    }

    @Override
    public final void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        locker.rollbackSubtree(AddressParser.makeWellFormedAddress(address), identity);
    }

    @Override
    public final void set(final String address, final VslNode knowledge, final VslIdentity identity)
            throws VslException {

        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);
        Collection<String> changedNodes = null;
        try {
            changedNodes = myNodeTree.setValue(wellFormedAddress, identity, knowledge);

        } catch (final NodeNotExistingException e) {
            // LOGGER.debug(address + " not found");
            final String virtualParent = virtualNodeManager
                    .getFirstVirtualParent(wellFormedAddress);
            if (virtualParent == null) {
                throw new NodeNotExistingException(e.getMessage());
            }
            // test access rights on virtual rootNode
            myNodeTree.get(virtualParent, new AddressParameters(), identity);
            virtualNodeManager.getVirtualNodeHandler(virtualParent).set(wellFormedAddress,
                    knowledge, identity);
        }

        if (virtualNodeManager.isVirtualNode(wellFormedAddress)) {
            virtualNodeManager.getVirtualNodeHandler(wellFormedAddress).set(wellFormedAddress,
                    knowledge, identity);
        }

        if (changedNodes != null) {
            try {
                subscriptionManager.notifySubscribers(changedNodes);
            } catch (final Exception e) {
                LOGGER.debug("Error on notifiying subscribers for: {}, error: {}", address,
                        e.getMessage());
            }
        }
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        // If no access permission on initial get, the exception won't be stopped and simply
        // handed to the issuer of the get request.
        try {
            // Check, if the correct access permission are in place.
            myNodeTree.get(AddressParser.makeWellFormedAddress(address), new AddressParameters(), identity);
        } catch (final NodeNotExistingException e) {
            // Node doesn't exist, check for virtual parent and ask his handler.
            final String virtualParent = virtualNodeManager.getFirstVirtualParent(address);
            if (virtualParent == null) {
                throw new NodeNotExistingException(e.getMessage());
            }

            // test access rights on virtual rootNode
            myNodeTree.get(virtualParent, new AddressParameters(), identity);
            return virtualNodeManager.getVirtualNodeHandler(virtualParent).getStream(address, identity);
        }
        // Access was ok, check if the node is virtual, as stream methods are only allowed on virtual
        // nodes for now.
        if (!virtualNodeManager.isVirtualNode(address)) {
            throw new NoVirtualNodeException("streams are currently only allowed with virtual nodes");
        }

        return virtualNodeManager.getVirtualNodeHandler(address).getStream(address, identity);
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);

        try {
            myNodeTree.checkRootWriteAccess(wellFormedAddress, identity);
        } catch (final NodeNotExistingException e) {
            final String virtualParent = virtualNodeManager.getFirstVirtualParent(wellFormedAddress);
            if (virtualParent == null) {
                throw new NodeNotExistingException(e.getMessage());
            }
            myNodeTree.checkRootWriteAccess(virtualParent, identity);
            virtualNodeManager.getVirtualNodeHandler(virtualParent).setStream(address, stream, identity);
        }

        // Access was ok, check if the node is virtual, as stream methods are only allowed on virtual
        // nodes for now.
        if (!virtualNodeManager.isVirtualNode(address)) {
            throw new NoVirtualNodeException("streams are currently only allowed with virtual nodes");
        }

        virtualNodeManager.getVirtualNodeHandler(address).setStream(address, stream, identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslIdentity identity) throws VslException {
        subscribe(address, subscriber, new AddressParameters(), identity);
    }

    @Override
    public final void subscribe(final String address, final VslSubscriber subscriber,
            final VslAddressParameters params, final VslIdentity identity) throws VslException {
        // TODO: check access rights.
        final VslNode affectedNodes = get(address, params, identity);
        subscriptionManager.addSubscription(address, subscriber, params, identity, affectedNodes);
    }

    @Override
    public final void unregisterService(final VslIdentity identity) throws VslException {
        // TODO: what to do on unregistration? we want to keep the data.
        // TODO: re-register lists afterwards? So they still work for other services with sufficient
        // rights.
        virtualNodeManager.unregisterAllVirtualNodes("/" + localID + "/" + identity.getClientId());
    }

    @Override
    public final void unregisterVirtualNode(final String address, final VslIdentity identity)
            throws VslException {
        final String wellFormedAddress = AddressParser.makeWellFormedAddress(address);
        if (identity.getAccessIDs().contains(myNodeTree.getNodeCreatorId(wellFormedAddress))) {
            virtualNodeManager.unregisterVirtualNode(wellFormedAddress);
        } else {
            throw new NoPermissionException(
                    "Couldn't register VirtualNode, only the creator of the node can do this.");
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
        subscriptionManager.removeSubscription(address, params, identity);
    }

    @Override
    public final void setKORSyncHandler(final VslKORSyncHandler newKORSyncHandler) {
        this.korSyncHandler = newKORSyncHandler;
    }

    @Override
    public final void cacheVslNodes(final String address, final VslNode node) {
        myNodeDatabase.cacheVslNode(address, node);

    }

    @Override
    public final void removeCachedNode(final String address) {
        if (!address.startsWith("/" + localID + "/")) {
            myNodeDatabase.removeCachedNode(address);
            // update the VslTypeSearchProvider
            updateTypeSearchProvider(address, true);
        }
    }

    @Override
    public final VslStructureNode getStructure(final String address)
            throws NodeNotExistingException {
        return getStructureNodeFor(address, true);
    }
}
