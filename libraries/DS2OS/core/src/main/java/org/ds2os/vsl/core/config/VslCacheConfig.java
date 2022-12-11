package org.ds2os.vsl.core.config;

/**
 * Interface for Caching related configurations.
 *
 * @author liebald
 */
public interface VslCacheConfig extends VslAgentName, VslCharset {

    /**
     * Returns whether the cache is enabled and should be used or not. Default is not.
     *
     * @return True if the cache is enabled, false otherwise.
     */
    @ConfigDescription(description = "Flag that specifies whether the cache is enabled (1) or not"
            + " (0).", id = "cache.enabled", defaultValue = "0", restrictions = "0 or 1 (boolean)")
    boolean isCacheEnabled();

    /**
     * Returns the TTL (Time to live) used if the model of a node doesn't specify one by itself. A
     * node is cached no longer than its TTL specifies. The default value is 60 seconds.
     *
     * @return The TTL of the cached node in seconds.
     */
    @ConfigDescription(description = "The default Time-To-Live for nodes cached by the cache in "
            + "seconds.", id = "cache.defaultTTL", defaultValue = "60", restrictions = ">=0")
    int getDefaultTTL();

    /**
     * Returns the maximum capacity of the cache in Bytes. Default is 1MB.
     *
     * @return Maximum capacity of the cache in Bytes.
     */
    @ConfigDescription(description = "Specifies how many nodes can be cached simultaneously "
            + "in bytes.", id = "cache.capacity", defaultValue = "1000000", restrictions = ">=0")
    int getCacheCapacity();

    /**
     * Returns the Replacement strategy that should be used when the cache reaches its capacity.
     * Default is Random Replacement (rr).
     *
     * @return Abbreviation of the replacement strategy. (lowercase)
     */
    @ConfigDescription(description = "The replacement Policy used by the cache.", id = "cache."
            + "replacementPolicy", defaultValue = "rr", restrictions = "rr, fifo, lru or lfu")
    String getReplacementPolicy();

}
