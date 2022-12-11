package org.ds2os.vsl.cache.cacheCleaner;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.ds2os.vsl.cache.dataCache.VslDataCache;
import org.ds2os.vsl.cache.metaCache.CachedNode;
import org.ds2os.vsl.cache.metaCache.VslMetaCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cachecleaner that runs regulary and drops outdated Items from the Cache. Also removes nodes from
 * the dataCache that were already dropped from the metaCache.
 *
 * @author liebald
 */
public class CacheCleaner implements Runnable {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheCleaner.class);

    /**
     * Used to synchronize access to the cache.
     */
    private final Object threadLock;

    /**
     * Access to cached metaData (what is cached, TTL,...).
     */
    private final VslMetaCache metaCache;

    /**
     * Access to the actual cached items.
     */
    private final VslDataCache dataCache;

    /**
     * Constructor.
     *
     * @param threadLock
     *            Used to synchronize access to the cache.
     * @param metaCache
     *            The {@link VslMetaCache} with access to cached metaData (what is cached, TTL,...).
     * @param dataCache
     *            The {@link VslDataCache} with Access to the actual cached items.
     */
    public CacheCleaner(final Object threadLock, final VslMetaCache metaCache,
            final VslDataCache dataCache) {
        this.threadLock = threadLock;
        this.metaCache = metaCache;
        this.dataCache = dataCache;
    }

    @Override
    public final void run() {
        // TODO: Replacement policy could also take place here, checking regularly not only if items
        // are expired but also if the cache is full.

        while (!Thread.interrupted()) {
            final Collection<String> toRemove = new LinkedList<String>();
            synchronized (threadLock) {
                // check all expired nodes
                for (final Entry<String, CachedNode> cachedNode : metaCache.getAllCachedNodes()
                        .entrySet()) {
                    if (cachedNode.getValue().isExpired()) {
                        toRemove.add(cachedNode.getKey());
                    }
                }
                if (!toRemove.isEmpty()) {
                    LOGGER.info("Removing {} from cache due to being outdated", toRemove);
                }

                // LOGGER.debug("{}", toRemove.size());
                // remove all expired nodes from meta cache
                for (final String address : toRemove) {
                    metaCache.removeFromCache(address);
                }
                toRemove.addAll(metaCache.getNodesForRemoval());

            }
            // removal from the dataCache can be done outside the synchronized, since the removed
            // content won't be used if it is no longer in the metaCache. So there it can't be the
            // case that someone wants to access it while it gets deleted.

            for (final String address : toRemove) {
                dataCache.removeFromCache(address);
            }
            // sleep 1 second, then check again.
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
