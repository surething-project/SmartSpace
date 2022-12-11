package org.ds2os.vsl.kor.locking;

import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.core.VslLockHandler;

/**
 * Stores Information about a locked Node (who has the lock, when was it acquired, what changed
 * during the lock,...).
 *
 * @author liebald
 */
public class LockedNode {

    /**
     * The time the lock was accesses the last time in Unix time.
     */
    private long lastTimeAccessed;

    /**
     * The time the lock was acquired in Unix time.
     */
    private final long lockAquirationTime;

    /**
     * The ID of the entity that acquired the lock.
     */
    private final String lockHolder;

    /**
     * The time the lock is valid in seconds.
     */
    private final long lockValidityPeriod;

    /**
     * Stores all updates that happened on the locked node or its subtree for later committing.
     */
    private final Map<String, String> nodeUpdates;

    /**
     * Indicates whether the holder of this lock was already notified that it will expire soon.
     */
    private boolean lockHolderNotifiedExpiration = false;

    /**
     * The {@link VslLockHandler} for callbacks.
     */
    private final VslLockHandler lockHandler;

    /**
     * Constructor for a new LockedNode Object.
     *
     * @param lockHolder
     *            The ID of the entity that acquired the lock.
     * @param lockHandler
     *            The {@link VslLockHandler} for callbacks.
     * @param lockValidityPeriod
     *            The time in seconds the lock is guaranteed to be held, if it isn't commited before
     *            the time expires.
     */
    public LockedNode(final String lockHolder, final VslLockHandler lockHandler,
            final int lockValidityPeriod) {
        this.lockHolder = lockHolder;
        this.lockAquirationTime = System.currentTimeMillis();
        this.lastTimeAccessed = lockAquirationTime;
        this.nodeUpdates = new HashMap<String, String>();
        this.lockValidityPeriod = lockValidityPeriod;
        this.lockHandler = lockHandler;
    }

    /**
     * Adds the given node to the UpdateMap of node Updates. Used to store them until the lock is
     * committed or reverted.
     *
     * @param address
     *            The address of the node.
     * @param value
     *            The value of the node to set.
     */
    public final void addNodeToLockedSubtree(final String address, final String value) {
        nodeUpdates.put(address, value);
        lastTimeAccessed = System.currentTimeMillis();
    }

    /**
     * Returns the time the locked node/subtree was changed the last time in Unix time.
     *
     * @return time the locked node/subtree was changed the last time
     */
    public final long getLastTimeAccessed() {
        return lastTimeAccessed;
    }

    /**
     * Returns the time the lock was acquired in Unix time.
     *
     * @return time the lock was acquired
     */
    public final long getLockAquirationTime() {
        return lockAquirationTime;
    }

    /**
     * Returns the ID of the entity that acquired the lock.
     *
     * @return the lock Holder
     */
    public final String getLockHolder() {
        return lockHolder;
    }

    /**
     * Returns how many seconds the lock is valid since the time it was created.
     *
     * @return validity period in seconds.
     */
    public final long getLockValidityPeriod() {
        return lockValidityPeriod;
    }

    /**
     * Returns how many seconds the lock is still valid.
     *
     * @return amount of seconds the lock is still valid (negative if no longer valid).
     */
    public final long getTimeLockIsStillValid() {
        return lockValidityPeriod - (System.currentTimeMillis() - lockAquirationTime) / 1000;
    }

    /**
     * Returns the Map of nodes which were changed in the locked subtree since the lock exists.
     *
     * @return Map of node updates as address, value pairs.
     */
    public final Map<String, String> getUpdatedNodes() {
        return nodeUpdates;
    }

    /**
     * Returns the {@link VslLockHandler} of this lock.
     *
     * @return {@link VslLockHandler} registered for this lock.
     */
    public final VslLockHandler getLockHandler() {
        return lockHandler;
    }

    /**
     * Calling this method means that the lockholder was notified that his lock will expire.
     */
    public final void notifiedLockWillExpire() {
        lockHolderNotifiedExpiration = true;
    }

    /**
     * Returns whether or not the lockHolder was already notified that the lock will expire soon.
     *
     * @return Boolean indicating whether the holder was notified (true) or not (false)
     */
    public final boolean isLockHolderNotifiedAboutWillExpire() {
        return lockHolderNotifiedExpiration;
    }
}
