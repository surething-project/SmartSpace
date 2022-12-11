package org.ds2os.vsl.korsync.updateCache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.config.VslKORSyncConfig;
import org.ds2os.vsl.korsync.KORSyncHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache class for VslKORUpdates which couldn't be applied immediately.
 *
 * @author liebald
 */
public class KorUpdateCache extends AbstractVslModule implements Runnable {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(KorUpdateCache.class);

    /**
     * Used to store VslUpdates that couldn't be applied instantly since the fromHash didn't match.
     * They can possibly be applied after one of the next updates for the same ka. The key is the
     * agentID the update is from concatenated with the hashFrom of the update. The value is a
     * {@link CachedUpdate}, which includes the {@link VslKORUpdate} and additional caching
     * information like an timestamp.
     */
    private final Map<String, CachedUpdate> updateCache;

    /**
     * String used between the concatenation of agentID and hashFrom as key for the updateCache Map.
     */
    private final String keyConcatenator = "?";

    /**
     * The configuration of the {@link KORSyncHandler}.
     */
    private final VslKORSyncConfig config;

    /**
     * Constructor for {@link KorUpdateCache}.
     *
     * @param config
     *            The configuration of the {@link org.ds2os.vsl.korsync.KORSyncHandler}.
     */
    public KorUpdateCache(final VslKORSyncConfig config) {
        updateCache = new HashMap<String, CachedUpdate>();
        this.config = config;

    }

    @Override
    public final void activate() throws Exception {
        final Thread t = new Thread(this);
        t.setName("KorUpdateCache");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public final void shutdown() {
    }

    /**
     * Adds an {@link VslKORUpdate} to the {@link KorUpdateCache}. Only stores incremental updates.
     *
     * @param update
     *            the {@link VslKORUpdate} to add.
     */
    public final void add(final VslKORUpdate update) {
        if (update.getHashFrom().equals("")) {
            return;
        }
        LOGGER.debug("Added KOR update from {} to KOR update cache", update.getAgentName());
        synchronized (updateCache) {
            updateCache.put(update.getAgentName() + keyConcatenator + update.getHashFrom(),
                    new CachedUpdate(update));
        }
    }

    /**
     * Checks if the {@link KorUpdateCache} contains an update for the given agentID which starts
     * from the given hash. If yes it is returned and removed from the cache. If not, null is
     * returned.
     *
     * @param agentID
     *            The agentID for which an update is wanted.
     * @param currentHash
     *            The current hash of the local structure of the KA with the given agentID.
     * @return A matching {@link VslKORUpdate} or null, if no matching {@link VslKORUpdate} is
     *         cached.
     */
    public final VslKORUpdate getUpdate(final String agentID, final String currentHash) {
        VslKORUpdate update = null;
        final String key = agentID + keyConcatenator + currentHash;
        synchronized (updateCache) {
            if (updateCache.containsKey(key)) {
                update = updateCache.get(key).getUpdate();
                updateCache.remove(key);
            }
        }
        return update;
    }

    @Override
    public final void run() {
        try {
            while (!Thread.interrupted()) {
                final long validityPeriod = config.getMaxKORUpdateCacheTime();
                // check all 10 seconds if items are outdated.
                synchronized (updateCache) {
                    final List<String> outdated = new LinkedList<String>();
                    for (final Entry<String, CachedUpdate> entry : updateCache.entrySet()) {
                        if (System.currentTimeMillis()
                                - entry.getValue().getTimestamp() > validityPeriod) {
                            outdated.add(entry.getKey());
                        }
                    }
                    for (final String update : outdated) {
                        updateCache.remove(update);
                    }
                }
                Thread.sleep(validityPeriod / 10);
            }
        } catch (final InterruptedException e) {
            LOGGER.error("VslUpdateCache Thread interrupted.");
        }
    }

}
