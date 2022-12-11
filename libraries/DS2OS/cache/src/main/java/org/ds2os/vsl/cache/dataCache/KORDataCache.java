package org.ds2os.vsl.cache.dataCache;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.VslException;

/**
 * Implementation of {@link VslDataCache}. Used to cache data, necessary metadata is handled by a
 * {@link org.ds2os.vsl.cache.metaCache.VslMetaCache}.
 *
 * @author liebald
 */
public class KORDataCache implements VslDataCache {

    /**
     * Access to the caching capabilities of the KOR.
     */
    private final VslKORCacheHandler cacheHandler;

    /**
     * Constructor.
     *
     * @param cacheHandler
     *            Access to the caching capabilities of the KOR.
     */
    public KORDataCache(final VslKORCacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
    }

    @Override
    public final void cache(final String address, final VslNode node) {
        cacheHandler.cacheVslNodes(address, node);
    }

    @Override
    public final VslNode get(final String address, final VslIdentity identity) throws VslException {
        return cacheHandler.get(address, identity);
    }

    @Override
    public final void removeFromCache(final String address) {
        cacheHandler.removeCachedNode(address);
    }

}
