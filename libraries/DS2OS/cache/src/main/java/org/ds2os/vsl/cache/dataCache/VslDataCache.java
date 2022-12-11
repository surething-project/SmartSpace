package org.ds2os.vsl.cache.dataCache;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.VslException;

/**
 * Interface for the cache component that actually stores the cached data. Implementations could use
 * e.g. the KOR or a different storage.
 *
 * @author liebald
 */
public interface VslDataCache {

    /**
     * Adds the given VslNode to the cache.
     *
     * @param address
     *            The address the VslNode belongs to in the Vsl.
     * @param node
     *            The VslNode to cache.
     */
    void cache(String address, VslNode node);

    /**
     * Returns the VslNode at the given address. Throws an Exception if there is no value cached.
     *
     * @param address
     *            Address in the Vsl to check.
     * @param identity
     *            The Identity of the issuer of the operation, to check if he has access to the
     *            node.
     * @return The VslNode cache at the given address.
     * @throws VslException
     *             thrown if there is no VslNode cached for the given address.
     */
    VslNode get(String address, VslIdentity identity) throws VslException;

    /**
     * Removes the given node from the cache.
     *
     * @param address
     *            The address of the node to remove from the cache.
     */
    void removeFromCache(String address);

}
