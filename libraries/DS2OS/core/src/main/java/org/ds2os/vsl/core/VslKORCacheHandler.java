package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.VslException;

/**
 * Interface of the KOR for cache related functions.
 *
 * @author liebald
 */
public interface VslKORCacheHandler extends VslKORStructureHandler {
    /*
     * Necessary: cache Nodes, remove Nodes from cache (only data, not structure), get nodes from
     * cache, get node structure information from cache (cache meta info + access rights + existing
     * children)
     *
     * caching logic is in the cache module (replacement, decision what to cache, decision when to
     * remove, etc.) The KOR only is used as storage for the cache and returning allowed nodes.
     */

    /**
     * Stores the given node/children in the KOR as cached. Version and timestamps are taken from
     * the nodes and not incremented for parents as with a normal set. Only stores nodes where
     * timestamp, version and value are valid values. If there already is a node stored at the KOR
     * for the given address, it is replaced with the new one (no new version created).
     *
     * @param address
     *            The address of the node that should be cached.
     * @param node
     *            The nodes to be cached.
     */
    void cacheVslNodes(String address, VslNode node);

    /**
     * Removes the cached Node at the given address. Structure, children and parents are kept, only
     * the node with the given address is deleted (all versions of it).
     *
     * @param address
     *            The address of the node to be deleted
     */
    void removeCachedNode(String address);

    /**
     * Poll the knowledge at a given address.
     *
     * @param address
     *            the node address in the VSL.
     * @param identity
     *            the identity of the issuer of this operation.
     * @return VslNode object with all data (including children) of the node at this address.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    VslNode get(String address, VslIdentity identity) throws VslException;

}
