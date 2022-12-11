package org.ds2os.vsl.kor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidValueException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeLockedException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.NodeNotLockedException;
import org.ds2os.vsl.exception.ParentNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.ds2os.vsl.kor.locking.VslLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liebald, pahl, ugurlu
 */
public class NodeTree implements VslNodeTree {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeTree.class);

    /**
     * ID of the local KA.
     */
    private final String localKA;

    /**
     * The LockControl object of the Nodetree.
     */
    private final VslLocker lockControl;

    /**
     * Database that stores all node-related information.
     */
    private final VslNodeDatabase myNodeDatabase;

    /**
     * The {@link VslNodeFactory} used to create different types of VslNodes.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * Constructor. Creates a new NodeTree Objects that treats localID as the id of the local KA.
     * This constructor can be used to specify a specific implementation of NodeDatabase that should
     * be used.
     *
     * @param localID
     *            ID of the local KA.
     * @param database
     *            the nodeDatabase Implementation that should be used for accessing the database.
     * @param lockControl
     *            the lockControl Implementation that should be used for accessing the lock
     *            controller.
     * @param nodeFactory
     *            The {@link VslNodeFactory} used to create different types of VslNodes.
     */
    public NodeTree(final String localID, final VslNodeDatabase database,
            final VslLocker lockControl, final VslNodeFactory nodeFactory) {
        this.myNodeDatabase = database;
        this.localKA = localID;
        this.lockControl = lockControl;
        this.nodeFactory = nodeFactory;
    }

    @Override
    public final void activate() {
        final List<String> types = new ArrayList<String>();
        types.add(TYPE_TREE_ROOT);
        final List<String> access = new ArrayList<String>();
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
        try {
            // add the rootnode for the local ka (/)
            addNode("/", types, access, access, "", "", SYSTEM_USER_ID);
            nodesToSet.put("/", "");
            myNodeDatabase.setValueTree(nodesToSet);
        } catch (final Exception e1) {
            LOGGER.error("Problems adding root node for the VSL: ", e1);
        }

        try {
            // add the node for the local ka (e.g. /ka1)
            addNode("/" + localKA, types, access, access, "", "", SYSTEM_USER_ID);
            nodesToSet.clear();
            nodesToSet.put("/" + localKA, "");
            myNodeDatabase.setValueTree(nodesToSet);

        } catch (final Exception e1) {
            LOGGER.error("Problems adding root node for local KOR: ", e1);
        }
        try {
            // add the node for the local system subtree (e.g. /ka1/system)
            addNode("/" + localKA + "/system", Arrays.asList("/basic/composed"), access, access, "",
                    "", SYSTEM_USER_ID);
            nodesToSet.clear();
            nodesToSet.put("/" + localKA + "/system", "");
            myNodeDatabase.setValueTree(nodesToSet);
        } catch (final Exception e1) {
            LOGGER.error("Problems adding system node for local KOR: ", e1);
        }
    }

    @Override
    public final synchronized void addNode(final String address, final List<String> types,
            final List<String> readerIds, final List<String> writerIds, final String restriction,
            final String cacheParameters, final String creatorId)
            throws NodeAlreadyExistingException, ParentNotExistingException {

        final String parentAddress = AddressParser.getParentAddress(address);
        if (!address.equals("/") && !myNodeDatabase.nodeExists(parentAddress)) {
            throw new ParentNotExistingException("Parent node doesn't exist for " + address);
        }

        if (myNodeDatabase.nodeExists(address)) {
            throw new NodeAlreadyExistingException("Node already exists: " + address);
        }

        final ArrayList<String> extendedReaders = new ArrayList<String>();
        final ArrayList<String> extendedWriters = new ArrayList<String>();

        // add creatorID and systemID to readers.
        // The creatorID MUST always be first, even if * is given as access.
        extendedReaders.add(creatorId);
        if (!extendedReaders.contains(SYSTEM_USER_ID) && !readerIds.contains("*")) {
            extendedReaders.add(SYSTEM_USER_ID);
        }

        // add creatorID and systemID to writers
        // The creatorID MUST always be first, even if * is given as access.
        extendedWriters.add(creatorId);
        if (!extendedWriters.contains(SYSTEM_USER_ID) && !writerIds.contains("*")) {
            extendedWriters.add(SYSTEM_USER_ID);
        }

        for (final String reader : readerIds) {
            if (!reader.isEmpty() && !extendedReaders.contains(reader)) {
                extendedReaders.add(reader);
            }
        }

        for (final String writer : writerIds) {
            if (!writer.isEmpty() && !extendedWriters.contains(writer)) {
                extendedWriters.add(writer);
            }
        }
        myNodeDatabase.addNode(address, types, extendedReaders, extendedWriters, restriction,
                cacheParameters);

        // LOGGER.debug("added new node at address {} ",address);
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters parameters,
            final VslIdentity identity)
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        TreeMap<String, InternalNode> nodes = null;
        // TODO: re-enable version/timestamp based requests
        // if (parameters != null && !parameters.isEmpty()) {
        // switch (getParameterType(parameters)) {
        // case 1:
        // // version
        // nodes = myNodeDatabase.getNodeRecord(address, true, Integer.parseInt(parameters));
        // break;
        // case 2:
        // nodes = myNodeDatabase.getNodeRecord(address, true,
        // getTimestampFromParameter(parameters, 1),
        // getTimestampFromParameter(parameters, 2));
        // break;
        // default:
        // throw new InvalidValueException(
        // "InvalidParameter, must be an integer (version) or timestamp range: "
        // + parameters);
        // }
        // } else {
        // nodes = myNodeDatabase.getNodeRecord(address, true);
        // for (Entry<String, InternalNode> node : nodes.entrySet()) {
        // LOGGER.debug("address: {}, value: {}, type: {}, reader: {}", node.getKey(),
        // node.getValue().getValue(), node.getValue().getType(),
        // node.getValue().getReaderIDs());
        // }
        // }
        nodes = myNodeDatabase.getNodeRecord(address, parameters);

        final TreeMap<String, InternalNode> validNodes = new TreeMap<String, InternalNode>();
        // if a subtree is retrieved, check access rights for all child nodes, and return
        // only additional data for those nodes, the requester has access rights to. Inner nodes
        // that are required for maintaining the node structure in the response should be
        // returned but all data should be nulled.
        for (final Entry<String, InternalNode> node : nodes.descendingMap().entrySet()) {
            if (!node.getValue().isReadableBy(identity)) {
                // if not readable use dummyvalues
                node.getValue().setValue(null);
                node.getValue().setType(new LinkedList<String>());
                node.getValue().setTimestamp(null);
                node.getValue().setVersion(-1);
                node.getValue().setRestriction("");
                // if writable, store with dummyvalues.
                if (node.getValue().isWritableBy(identity)) {
                    validNodes.put(node.getKey(), node.getValue());
                } else {
                    // if there is any child already in the validList nodes, add the with dummy
                    // values, even though not read and writable, since we want to keep the
                    // structure intact.
                    for (final String addr : validNodes.keySet()) {
                        if (addr.startsWith(node.getKey())) {
                            validNodes.put(node.getKey(), node.getValue());
                            break;
                        }
                    }
                }
            } else {
                validNodes.put(node.getKey(), node.getValue());
            }

        }
        if (validNodes.size() == 0) {
            throw new NoPermissionException(Arrays.deepToString(identity.getAccessIDs().toArray())
                    + " cannot read node at " + address + " or any child");
        }

        final Map<String, VslNode> resultNodes = new HashMap<String, VslNode>();
        resultNodes.put("",
                nodeFactory.createImmutableLeaf(validNodes.get(address).getType(),
                        validNodes.get(address).getValue(), validNodes.get(address).getTimestamp(),
                        validNodes.get(address).getVersion(),
                        getAccessFlag(validNodes.get(address), identity),
                        Restrictions.splitRestrictions(validNodes.get(address).getRestriction())));
        validNodes.remove(address);

        // iterate through all possible children and add them to the VslNode
        String parent;
        if (address.equals("/")) {
            parent = address;
        } else {
            parent = address + "/";
        }
        for (final Entry<String, InternalNode> node : validNodes.entrySet()) {

            resultNodes.put(node.getKey().replaceFirst(parent, ""),
                    nodeFactory.createImmutableLeaf(node.getValue().getType(),
                            node.getValue().getValue(), node.getValue().getTimestamp(),
                            node.getValue().getVersion(), getAccessFlag(node.getValue(), identity),
                            Restrictions.splitRestrictions(node.getValue().getRestriction())));
            // LOGGER.debug(node.getKey() + " " +node.getValue().getReaderIDs().toString() +" " +
            // node.getValue().getValue());
        }

        final VslMutableNode resultNode = nodeFactory.createMutableNode(resultNodes.entrySet());
        lockControl.updateGetResultWithLockedData(address, resultNode, identity.getClientId());
        return resultNode;
    }

    /**
     * Returns the access flag for the given internal node.
     *
     * @param node
     *            The {@link InternalNode} to check.
     * @param id
     *            The {@link VslIdentity} to check.
     * @return the access flags for the given node.
     */
    private String getAccessFlag(final InternalNode node, final VslIdentity id) {
        if (node.isReadableBy(id)) {
            if (!node.isWritableBy(id)) {
                return "r"; // read only
            } else {
                return ""; // read+write
            }
        } else {
            if (node.isWritableBy(id)) {
                return "w"; // write only
            } else {
                return "-"; // no access
            }
        }
    }

    @Override
    public final TreeMap<String, MetaNode> getMetaData(final String address,
            final boolean includeSubtree) throws NodeNotExistingException {
        return myNodeDatabase.getNodeMetaData(address, includeSubtree);
    }

    @Override
    public final String getNodeCreatorId(final String address) throws NodeNotExistingException {
        try {
            return myNodeDatabase.getNodeMetaData(address, false).get(address).getWriterIDs()
                    .get(0);
        } catch (final Exception e) {
            throw new NodeNotExistingException(
                    "Couldn't retrieve creatorID of the node: " + address + ": " + e.getMessage());
        }

    }

    @Override
    public final void removeNode(final String address) throws NodeNotExistingException {
        if (myNodeDatabase.nodeExists(address)) {
            myNodeDatabase.removeNode(address);
        } else {
            throw new NodeNotExistingException(
                    "Can't remove Node " + address + " since it doesn't exist");
        }
    }

    @Override
    public final Collection<String> setValue(final String address, final VslIdentity identity,
            final VslNode node) throws NoPermissionException, NodeNotExistingException,
            InvalidValueException, NodeLockedException {
        final TreeMap<String, MetaNode> metaData = myNodeDatabase.getNodeMetaData(address, true);
        // Collect all nodes that actually should be written (can't write directly since we need to
        // check access first for all nodes)
        // linkedHashMap secures the order of nodes when iterating over them.
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();

        // check initial node first, only check nodes which have values set
        if (node.getValue() != null) {
            try {
                if (!metaData.get(address).isWritableBy(identity)) {
                    throw new NoPermissionException(identity.getAccessIDs().toString()
                            + " cannot write node at " + address + ", nothing changed.");
                }
                if (!Restrictions.evaluateNumberText(node.getValue(),
                        metaData.get(address).getRestriction())) {
                    throw new InvalidValueException("Invalid value: " + node.getValue()
                            + " for restriction: " + metaData.get(address).getRestriction()
                            + " at node: " + address);
                }

            } catch (final NullPointerException e) {
                throw new NodeNotExistingException("Node " + address + " not existing");
            }
            nodesToSet.put(address, node.getValue());
        }

        // iterate over all childs and check their access rights/restrictions.
        for (final Entry<String, VslNode> child : node.getAllChildren()) {
            if (child.getValue().getValue() != null) {
                try {
                    if (!metaData.get(address + "/" + child.getKey()).isWritableBy(identity)) {
                        throw new NoPermissionException(
                                identity.getAccessIDs().toString() + " cannot write node at "
                                        + address + "/" + child.getKey() + ", nothing changed.");
                    }
                    if (!Restrictions.evaluateNumberText(child.getValue().getValue(),
                            metaData.get(address + "/" + child.getKey()).getRestriction())) {
                        throw new InvalidValueException("Invalid value: "
                                + child.getValue().getValue() + " for restriction: "
                                + metaData.get(address + "/" + child.getKey()).getRestriction()
                                + " at node: " + address + "/" + child.getKey());
                    }

                } catch (final NullPointerException e) {
                    throw new NodeNotExistingException(
                            "Node " + address + "/" + child.getKey() + " not existing");
                }
                nodesToSet.put(address + "/" + child.getKey(), child.getValue().getValue());
            }
        }

        // for now we do a all or nothing locked strategy.
        // either none of the nodes we want to set is locked, or all of them are.

        // check for all nodes if they are locked.
        // If the root node of the set operation was locked, directly add the nodes to the database.
        if (!lockControl.isLocked(address)) {
            for (final Entry<String, String> n : nodesToSet.entrySet()) {
                if (lockControl.isLocked(n.getKey())) {
                    // if the rootNode of the set node is not locked but any child is locked, abort.
                    throw new NodeLockedException("Error, the Root Node of the Set operation ("
                            + address + ") was not locked, but a child was: " + n.getKey());
                }
            }
            // set new values
            myNodeDatabase.setValueTree(nodesToSet);
        } else {
            if (!lockControl.isLockedBy(address, identity.getClientId())) {
                throw new NodeLockedException("Error, the Root Node of the Set operation ("
                        + address + ") is already locked by someone else!");
            }
            for (final Entry<String, String> n : nodesToSet.entrySet()) {
                try {
                    lockControl.addNodeValueForCommit(n.getKey(), n.getValue());
                } catch (final NodeNotLockedException e) {
                    // shouldn't happen
                    LOGGER.error("Got a NodeNotLockedException where he should have been locked",
                            e);
                }
            }
        }
        return nodesToSet.keySet();

    }

    @Override
    public void checkRootWriteAccess(final String address, final VslIdentity identity)
            throws NodeNotExistingException, NoPermissionException {
        final TreeMap<String, MetaNode> metaData = myNodeDatabase.getNodeMetaData(address, true);

        final MetaNode rootNode = metaData.get(address);
        if (rootNode == null) {
            throw new NodeNotExistingException("Node " + address + " not existing");
        }
        if (!rootNode.isWritableBy(identity)) {
            throw new NoPermissionException(identity.getAccessIDs().toString()
                    + " cannot write node at " + address + ", nothing changed.");
        }
    }
}
