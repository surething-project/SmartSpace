package org.ds2os.vsl.kor.locking;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ds2os.vsl.core.config.VslKORLockingConfig;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for periodically checking if locks are still valid or if they exist for
 * too long and should be removed.
 *
 * @author liebald
 */
public class LockExpirationHandler implements Runnable {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LockExpirationHandler.class);

    /**
     * The list of active locks that should be observed.
     */
    final Map<String, LockedNode> activeLocks;

    /**
     * Config service.
     */
    private final VslKORLockingConfig configService;

    /**
     * Constructor for LockExpirationHandler.
     *
     * @param activeLocks
     *            The list of active locks that should be observed.
     * @param configService
     *            The config service for the Locker.
     */
    public LockExpirationHandler(final Map<String, LockedNode> activeLocks,
            final VslKORLockingConfig configService) {
        this.activeLocks = activeLocks;
        this.configService = configService;
    }

    @Override
    public final void run() {
        while (!Thread.interrupted()) {
            synchronized (activeLocks) {
                for (final Iterator<Entry<String, LockedNode>> iterator = activeLocks.entrySet()
                        .iterator(); iterator.hasNext();) {
                    final Entry<String, LockedNode> lock = iterator.next();

                    // if the lock has only a few time left, notify the holder if not done already.
                    if (!lock.getValue().isLockHolderNotifiedAboutWillExpire()
                            && lock.getValue().getTimeLockIsStillValid() <= configService
                                    .getLockExpirationWarningTime()) {
                        try {
                            lock.getValue().getLockHandler().lockWillExpire(lock.getKey());
                            lock.getValue().notifiedLockWillExpire();
                        } catch (final VslException e) {
                            LOGGER.error("Error on calling lockWillExpired on VslLockHandler: {}",
                                    e);
                        }
                    } else if (lock.getValue().getTimeLockIsStillValid() <= 0) {
                        // if the lock is no longer valid, remove it and notify the lockHolder.
                        LOGGER.info("removed {} from locks since it expired!", lock.getKey());
                        try {
                            lock.getValue().getLockHandler().lockExpired(lock.getKey());
                        } catch (final VslException e) {
                            LOGGER.error("Error on calling lockExpired on VslLockHandler: {}", e);
                        }
                        iterator.remove();
                    }
                }
            }
            try {
                Thread.yield();
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                break;
            }
        }
    }

}
