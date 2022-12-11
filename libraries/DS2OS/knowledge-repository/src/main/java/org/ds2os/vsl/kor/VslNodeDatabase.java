package org.ds2os.vsl.kor;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;

/**
 * Interface for different implementations of a NodeDatabase, e.g. using different kinds of
 * databases.
 *
 * @author liebald
 *
 */
public interface VslNodeDatabase {

    /**
     * Used in list serialization/deserialization as delimiter between elements.
     */
    String LIST_SEPARATOR = ";";

    /**
     * Adds a node to the kor.
     *
     * @param address
     *            The address of the new node
     * @param types
     *            The types of the new node
     * @param readerIds
     *            The allowed readers of the new node
     * @param writerIds
     *            The allowed writers of the new node
     * @param restriction
     *            A restriction to the nodes value
     * @param cacheParameters
     *            The cache parameters of this node.
     */
    void addNode(String address, List<String> types, List<String> readerIds, List<String> writerIds,
            String restriction, String cacheParameters);

    /**
     * Returns all addresses that contain the specified type. Search is performed in the subtree
     * specified by <code>rootAddress</code>.
     *
     * @param rootAddress
     *            The subtree where to search for nodes of the required type
     * @param type
     *            The type that should be matched by nodes
     * @return A list of addresses of nodes in the specified subtree that contain the required type
     */
    List<String> getAddressesOfType(String rootAddress, String type);

    // /**
    // * Recursively determines and returns all child addresses of the node as wellFormedAddress.
    // *
    // * @param address
    // * The node to start at.
    // * @return All child addresses as a list.
    // * @throws NodeNotExistingException
    // * When there is no node at address
    // */
    // List<String> getAllChildAddresses(String address) throws NodeNotExistingException;

    // /**
    // * Returns all direct (1st-level) children of <code>address</code>.
    // *
    // * @param address
    // * The address for which to return the children
    // * @return A List of all children or an empty list if there are none.
    // * @throws NodeNotExistingException
    // * if the given node doesn't exist.
    // */
    // List<String> getDirectChildrenAddresses(String address) throws NodeNotExistingException;

    /**
     * Returns the hash of the structural information of the tree at the given rootAddress
     * (including rootAddress, excluding given subtrees).
     *
     * @param rootAddress
     *            The root Address of the subtree for which the hash is wanted.
     * @param exludeSubtrees
     *            List of subtrees that should be excluded from the hash. Theses must be given
     *            relative to the rootAddress (e.g. for rootAddress "/KA" exclude the subtree
     *            "system" means that the hash will contain the structures of "/KA" without
     *            "/KA/system")
     * @return The hash of the subtree.
     */
    String getHashOfSubtree(String rootAddress, List<String> exludeSubtrees);

    /**
     * Returns a TreeMap (ordered by node addresses) containing the MetaData of the specified node
     * at the given address and his children.
     *
     * @param address
     *            The address of the node to fetch.
     * @param includeSubtree
     *            Defines if child nodes should also be returned (not for archived nodes).
     * @return A TreeMap containing MetaNodes for the queried node and his children
     * @throws NodeNotExistingException
     *             Thrown if there is no node at address.
     */
    TreeMap<String, MetaNode> getNodeMetaData(String address, boolean includeSubtree)
            throws NodeNotExistingException;

    // /**
    // * Returns a TreeMap (ordered by node addresses) containing the specified node at the given
    // * address and his children.
    // *
    // * @param address
    // * The address of the node to fetch.
    // * @param includeSubtree
    // * Defines if child nodes should also be returned (not for archived nodes).
    // * @return A TreeMap containing InternalNodes for the queried node and his children
    // * @throws NodeNotExistingException
    // * Thrown if there is no node at address.
    // */
    // @Deprecated
    // TreeMap<String, InternalNode> getNodeRecord(String address, boolean includeSubtree)
    // throws NodeNotExistingException;
    //
    // /**
    // * Returns a TreeMap (ordered by node addresses) containing the specified node at the given
    // * address and his childs. Instead of the newest version, the specified version of that tree
    // is
    // * returned. This means every node has the version/value at the time the requested nodes
    // subtree
    // * was changed the last time, which is lower or equal to the specified treeversion.
    // *
    // * @param address
    // * The address of the node to fetch.
    // * @param includeSubtree
    // * Defines if child nodes should also be returned (not for archived nodes).
    // * @param treeVersion
    // * Defines the version that is queried.
    // * @return A TreeMap containing InternalNodes for the queried node and his children with the
    // * given version of the root node.
    // * @throws NodeNotExistingException
    // * Thrown if there is no node at address.
    // */
    // @Deprecated
    // TreeMap<String, InternalNode> getNodeRecord(String address, boolean includeSubtree,
    // int treeVersion) throws NodeNotExistingException;
    //
    // /**
    // * Returns a TreeMap (ordered by node addresses) containing the specified node at the given
    // * address and his children. Instead of the newest version of that tree, the newest version in
    // * the specified interval is returned. The root node must lie in between the given timespan,
    // his
    // * children are returned in the version/timestamp they had at the time the root node that is
    // * returned was created/changed.
    // *
    // * @param address
    // * The address of the node to fetch.
    // * @param includeSubtree
    // * Defines if child nodes should also be returned (not for archived nodes).
    // * @param from
    // * The minimum Timestamp of the node/tree that is wanted. If null January 1, 1970,
    // * 00:00:00 GMT is taken.
    // * @param to
    // * The maximum Timestamp of the node/tree that is wanted. If null the current time is
    // * taken.
    // * @return A TreeMap containing InternalNodes for the queried node and his children, within
    // the
    // * given timespan.
    // * @throws NodeNotExistingException
    // * Thrown if there is no node at address.
    // */
    // @Deprecated
    // TreeMap<String, InternalNode> getNodeRecord(String address, boolean includeSubtree, Date
    // from,
    // Date to) throws NodeNotExistingException;

    // /**
    // * Returns the total number of nodes that exist in the local agent's KOR.
    // *
    // * @return The number of nodes
    // */
    // int getNumberOfNodes();

    /**
     * Returns whether or not a node exists at the specified address.
     *
     * @param address
     *            The address to check
     * @return True if there is a node at address, else false.
     */
    boolean nodeExists(String address);

    /**
     * Removes a node from the given address. Also removes archived versions and children of the
     * node.
     *
     * @param address
     *            Address of the node that should be removed.
     */
    void removeNode(String address);

    // /**
    // * Sets a new value for the node at given address with the current timestamp. Also makes sure
    // * that the version number of the given node and his parents are incremented (up to service
    // * level).
    // *
    // * @param address
    // * The address where to set the new value.
    // * @param value
    // * The value to set.
    // * @throws NodeNotExistingException
    // * If there is no node at address.
    // */
    // void setValue(String address, String value) throws NodeNotExistingException;

    /**
     * Sets new values for all the nodes at given addresses with the current timestamp. Also makes
     * sure that the version number of the given nodes and their parents are incremented by 1 (up to
     * service level).
     *
     * @param values
     *            Map of address, value pairs to be set.
     * @throws NodeNotExistingException
     *             If there is no node at address.
     */
    void setValueTree(Map<String, String> values) throws NodeNotExistingException;

    /**
     * Shutdown the database cleanly, closing the connection .
     */
    void shutdown();

    /**
     * Activates the Database (open connection, ...). Must be called before the
     * {@link VslNodeDatabase} can be used. Constructor only does the wiring.
     */
    void activate();

    /**
     * Stores the given nodes in the KOR as cached. Version and timestamps are taken from the nodes
     * and not incremented for parents as with a normal set. Only stores nodes where timestamp,
     * version and value are valid values.
     *
     * @param address
     *            The address of the node that should be cached.
     * @param nodes
     *            The nodes to be cached. Each node is seen separately without children.
     */
    void cacheVslNode(String address, VslNode nodes);

    /**
     * Removes the cached Node at the given address. Structure, children and parents are kept, only
     * the node with the given address is deleted (all versions).
     *
     * @param address
     *            The address of the node to be deleted.
     */
    void removeCachedNode(String address);

    /**
     * Returns a TreeMap (ordered by node addresses) containing the specified node at the given
     * address and his children.
     *
     * @param address
     *            The address of the node to fetch.
     * @param params
     *            Defines what information about the queried node(-s) should be returned.
     * @return A TreeMap containing InternalNodes for the queried node and his children
     * @throws NodeNotExistingException
     *             Thrown if there is no node at address.
     */
    TreeMap<String, InternalNode> getNodeRecord(String address, VslAddressParameters params)
            throws NodeNotExistingException;

}
