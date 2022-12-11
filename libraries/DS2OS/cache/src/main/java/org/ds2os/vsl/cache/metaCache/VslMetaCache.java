package org.ds2os.vsl.cache.metaCache;

import java.util.Collection;
import java.util.Map;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslNode;

/**
 * Interface for the cache component that holds the meta-informations for cached Nodes (e.g. access
 * frequency, time added to cache, last access,...). There could be e.g. Implementations with
 * LRU/LFU/RR replacement strategies.
 *
 * @author liebald
 */
public interface VslMetaCache {

    /**
     * Adds the address to the currently cached nodes and stores necessary meta-information (last
     * access,..., depending on implementation). If the node is already cached for this address, the
     * information is updated.
     *
     * @param address
     *            The address to add to the cache.
     * @param node
     *            The node to be cached at this address, including children.
     * @return The part of the VslNode that can be cached. null if nothing can be cached.
     */
    VslNode cache(String address, VslNode node);

    /**
     * Returns all information about currently cached nodes.
     *
     * @return Map with all currently cached nodes.
     */
    Map<String, CachedNode> getAllCachedNodes();

    /**
     * Returns the size of currently cached nodes including cache metaData.
     *
     * @return Size of currently cached nodes in byte.
     */
    long getCurrentCacheSize();

    /**
     * Returns a collection of all nodes that were removed from the metaCache but still need to be
     * removed from the dataCache. The nodesForRemoval list of the metaCache is cleared after this
     * call.
     *
     * @return Collection of Nodes that are up for removal.
     */
    Collection<String> getNodesForRemoval();

    /**
     * Returns whether or not an address is cached. This includes all accessible children of this
     * node. If any of these are not cached, this returns false.
     *
     * @param address
     *            The address to check.
     * @param identity
     *            The identity for which should be checked if the node and the accessible childs are
     *            cached.
     * @return True if a node and all accessible children are cached for the given address, false
     *         otherwise.
     */
    boolean isCached(String address, VslIdentity identity);

    /**
     * Removes the given node from the cache.
     *
     * @param address
     *            The address of the node to remove from the cache.
     */
    void removeFromCache(String address);

    /**
     * Updates the stored metadata for all nodes returned from the cache.
     *
     * @param address
     *            Address of the node served from the cache.
     * @param node
     *            The node served from the cache.
     */
    void updatedCachedData(String address, VslNode node);

    /**
     * Removes all currently cached items from it.
     */
    void clearCache();

}
