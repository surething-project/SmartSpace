package org.ds2os.vsl.core;

/**
 * Handler of received {@link VslAlivePing} messages.
 *
 * @author felix
 */
public interface VslAlivePingHandler {

    /**
     * Handle a received alive ping message.
     *
     * @param alivePing
     *            the alive ping message.
     * @param isAuthenticated
     *            If the alive ping was authenticated
     */
    void handleAlivePing(VslAlivePing alivePing, boolean isAuthenticated);
}
