package org.ds2os.vsl.korsync.updateCache;

import org.ds2os.vsl.core.VslKORUpdate;

/**
 * Helperclass to store additional information with an cached {@link VslKORUpdate}.
 *
 * @author liebald
 */
public class CachedUpdate {

    /**
     * The update to cache using this {@link CachedUpdate}.
     */
    private final VslKORUpdate update;

    /**
     * The time this {@link CachedUpdate} was created.
     */
    private final long timestamp;

    /**
     * Creates an cachable {@link CachedUpdate}, containing the given {@link VslKORUpdate} and an
     * timestamp indicating when it was cached.
     *
     * @param update
     *            The update to cache
     */
    public CachedUpdate(final VslKORUpdate update) {
        this.update = update;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * @return the update that is stored in this {@link CachedUpdate}.
     */
    public final VslKORUpdate getUpdate() {
        return update;
    }

    /**
     * @return the timestamp when this update was cached in unix time (milliseconds since epoch).
     */
    public final long getTimestamp() {
        return timestamp;
    }

}
