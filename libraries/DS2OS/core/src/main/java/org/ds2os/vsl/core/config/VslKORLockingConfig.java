package org.ds2os.vsl.core.config;

/**
 * This interface contains all configuration methods for the Locking procedure.
 *
 * @author liebald
 */
public interface VslKORLockingConfig {

    /**
     * Returns the amount of seconds a Lock is valid until it is seen as expired and discarded if
     * not yet committed.
     *
     * @return Expiration time in seconds.
     */
    @ConfigDescription(description = "Amount of seconds a VSL lock is valid until it expires."
            + "", id = "kor.locking.expirationTime", defaultValue = "30", restrictions = ">0")
    int getLockExpirationTime();

    /**
     * Returns how many seconds will be left (at most) for a lock before its lockWillExpire on
     * {@link org.ds2os.vsl.core.VslLockHandler} will be called.
     *
     * @return Amount of seconds before lock expiration when lockWillExpire will be called.
     */
    @ConfigDescription(description = "Amount of seconds after aquiring a VSL lock "
            + "before a notification about soon expiration of the lock is sent to "
            + "the lock holder.", id = "kor.locking.expirationWarningTime"
                    + "", defaultValue = "5", restrictions = ">0, <kor.locking.expirationTime")
    int getLockExpirationWarningTime();
}
