package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.VslException;

/**
 * Interface for the callback handling of {@link VslConnector#lockSubtree}.
 *
 * @author felix
 */
public interface VslLockHandler extends VslCallback {

    /**
     * Callback that the lock is successfully acquired.
     *
     * @param address
     *            the address where the lock is acquired.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockAcquired(String address) throws VslException;

    /**
     * Callback that the lock will expire if it is not renewed.
     *
     * @param address
     *            the address where the lock will expire.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockWillExpire(String address) throws VslException;

    /**
     * Callback that the lock is now expired.
     *
     * @param address
     *            the address where the lock expired.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockExpired(String address) throws VslException;
}
