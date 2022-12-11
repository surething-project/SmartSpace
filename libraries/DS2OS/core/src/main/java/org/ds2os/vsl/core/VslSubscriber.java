package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.VslException;

/**
 * Interface for the callback handling in {@link VslConnector#subscribe}.
 *
 * @author felix
 * @author ugurlu
 */
public interface VslSubscriber extends VslCallback {

    /**
     * Triggers the callback fired for a change in the specified address. Example: If you subscribe
     * to /agent1/foo and a change occurs in /agent1/foo/bar your callback will be called with
     * /agent1/foo/bar as parameter.
     *
     * @param address
     *            The address where a change took place.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void notificationCallback(String address) throws VslException;
}
