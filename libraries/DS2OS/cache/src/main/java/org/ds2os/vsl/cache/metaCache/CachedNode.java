package org.ds2os.vsl.cache.metaCache;

/**
 * Class used to store necessary meta information about cached nodes. The actual values are stored
 * separated.
 *
 * @author liebald
 */
public class CachedNode {

    /**
     * Size of metaData stored by this node.
     */
    public static final int SIZE = 4 * Long.SIZE / 8 + 2 * Integer.SIZE / 8;
    /**
     * Version of the node that is cached.
     */
    private final long nodeVersion;

    /**
     * The size of the cached node in bytes.
     */
    private final long size;

    /**
     * Timestamp when the node was cached the first time.
     */
    private final long initialCacheTimestamp;

    /**
     * Timestamp of the last access.
     */
    private long lastAccessed;

    /**
     * The amount of time the node was accessed.
     */
    private int amountAccessed;

    /**
     * The time to live of this node.
     */
    private final int ttl;

    /**
     * Constructor.
     *
     * @param version
     *            Version of the node that is cached.
     * @param ttl
     *            The time to live of this node.
     * @param valueSize
     *            The size of the cached value in bytes (including the version and timestamp that is
     *            storeded in the dataCache). Size of Data stored in the CachedNode is added
     *            automatically.
     */
    public CachedNode(final long version, final int ttl, final long valueSize) {
        nodeVersion = version;
        this.ttl = ttl;
        initialCacheTimestamp = System.currentTimeMillis();
        lastAccessed = initialCacheTimestamp;
        amountAccessed = 0;

        // add the size of the metaNode and the version and timestamp stored in the dataCache.
        size = valueSize + SIZE;
    }

    /**
     * Returns how often the node was accessed since he was cached.
     *
     * @return the amountAccessed
     */
    public final int getAmountAccessed() {
        return amountAccessed;
    }

    /**
     * Returns the timestamp when the node was added to the cache.
     *
     * @return the initialCacheTimestamp
     */
    public final long getInitialCacheTimestamp() {
        return initialCacheTimestamp;
    }

    /**
     * Returns when the node was accessed the last time.
     *
     * @return Unix timestamp of the last access.
     */
    public final long getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Returns the version of the cached node.
     *
     * @return the nodeVersion
     */
    public final long getNodeVersion() {
        return nodeVersion;
    }

    /**
     * Returns the size of the cached node including value and metadata. May differ from the actual
     * needed size, depending on the implementation of the
     * {@link org.ds2os.vsl.cache.dataCache.VslDataCache}.
     *
     * @return The size of the cached node (value, version, timestamp, cache related metaData) in
     *         byte.
     */
    public final long getSize() {
        return size;
    }

    /**
     * Returns whether or not the node is expired and should be removed.
     *
     * @return True if the node is expired, false otherwise.
     */
    public final boolean isExpired() {
        return (System.currentTimeMillis() - initialCacheTimestamp) / 1000.0 > ttl;
    }

    /**
     * Updates the metaInformation of the cached node when it was accessed.
     */
    public final void nodeAccessed() {
        amountAccessed++;
        lastAccessed = System.currentTimeMillis();
    }

}
