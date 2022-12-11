package org.ds2os.vsl.kor;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.impl.KAIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidValueException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeLockedException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.ParentNotExistingException;
import org.ds2os.vsl.kor.dataStructures.MetaNode;

/**
 * Interface for the nodeTree (access control, tree structure,...).
 *
 * @author liebald
 *
 */
public interface VslNodeTree {

    /**
     * This ID is added as reader and writer to an node. Do not allow anyone besides the system to
     * get this ID!
     */
    String SYSTEM_USER_ID = "system";

    /**
     * A {@link VslIdentity} for the SYSTEM_USER_ID.
     */
    VslIdentity SYSTEM_USER_IDENTITY = new KAIdentity(SYSTEM_USER_ID);

    /**
     * This is the type of the node tree root.
     */
    String TYPE_TREE_ROOT = "/treeRoot";

    /**
     * Creates a new Node of given type and adds it at position address. The read and write
     * permissions are granted to creatorID. Checks for access rights on parent node, if parent node
     * exists and if node to add already exists.
     *
     * @param address
     *            address of the new node
     * @param types
     *            Types of the new node
     * @param readerIds
     *            The allowed readers of the new node
     * @param writerIds
     *            The allowed writers of the new node
     * @param restriction
     *            A restriction to the nodes value
     * @param cacheParameters
     *            The cache parameters of this node.
     * @param creatorId
     *            creator of the node
     * @throws NodeAlreadyExistingException
     *             If there is already a Node at the given address.
     * @throws ParentNotExistingException
     *             If the parent Node does not exist.
     */
    void addNode(String address, List<String> types, List<String> readerIds, List<String> writerIds,
            String restriction, String cacheParameters, String creatorId)
            throws NodeAlreadyExistingException, ParentNotExistingException;

    /**
     * Returns this node's data value. If the node has children, their values are included as children
     * of the VslNode that is returned.
     *
     *
     * @param address
     *            The address of the node the action should happen.
     * @param params
     *            Parameters of the get request.
     * @param identity
     *            The IDs of the reader.
     * @return A VslNode including children. Values that can't be accessed by identity are set to
     *         null.
     * @throws NoPermissionException
     *             if the node and none of his children is accessible by identity.
     * @throws NodeNotExistingException
     *             If there is no node at address.
     * @throws InvalidValueException
     *             If the given Parameter is invalid.
     */
    VslNode get(String address, VslAddressParameters params, VslIdentity identity)
            throws NoPermissionException, NodeNotExistingException, InvalidValueException;

    /**
     * Returns the Meta Data (Types, readers/writers, restriction) for the give address.
     *
     * @param address
     *            Address of the requested node.
     * @param includeSubtree
     *            Defines if children data should also be returned.
     * @return A Tree of MetaNode Objects containing the metaData of the queried node and his
     *         children, if specified.
     * @throws NodeNotExistingException
     *             Thrown if no node exists at the given address.
     */
    TreeMap<String, MetaNode> getMetaData(String address, boolean includeSubtree)
            throws NodeNotExistingException;

    /**
     * Returns the ID of the creator of the specified node (serviceID).
     *
     * @param address
     *            The address of the node.
     * @return The ID of the creator as String
     * @throws NodeNotExistingException
     *             Thrown if the node doesn't exist.
     */
    String getNodeCreatorId(String address) throws NodeNotExistingException;

    /**
     * Removes a node at the given address and all subnodes.
     *
     * @param address
     *            The address of the node that should be removed
     * @throws NodeNotExistingException
     *             Thrown if the node we want to remove doesn't exist
     */
    void removeNode(String address) throws NodeNotExistingException;

    /**
     * Sets this nodes data value. This automatically increases the version number of this node's
     * whole subtree (towards the root node).
     *
     * @param address
     *            The address of the node the action should happen.
     * @param node
     *            The new data value for this node stored as VslNode.
     * @param identity
     *            The ID of the writer.
     * @return Collection of all addresses that were actually changed.
     * @throws NoPermissionException
     *             If writersID has no write permission on the node.
     * @throws NodeNotExistingException
     *             If there is no node at address.
     * @throws InvalidValueException
     *             If the value to set is not valid (Restrictions not matched).
     * @throws NodeLockedException
     *             If a node is locked,.
     */
    Collection<String> setValue(String address, VslIdentity identity, VslNode node)
            throws NoPermissionException, NodeNotExistingException, InvalidValueException,
            NodeLockedException;

    /**
     * Checks, if the given identity has write permission on the node.
     *
     * @param address
     *      the addressof the node.
     * @param identity
     *      the identity of the writer.
     * @throws NodeNotExistingException
     *      if the node does not exist.
     * @throws NoPermissionException
     *      if the writer has no permission
     */
    void checkRootWriteAccess(String address, VslIdentity identity)
            throws NodeNotExistingException, NoPermissionException;

    /**
     * Activate the the {@link VslNodeTree} Object, so that it can start working.
     */
    void activate();

}
