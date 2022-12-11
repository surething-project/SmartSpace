package org.ds2os.vsl.core;

/**
 * Sender of {@link VslAlivePing} messages. These must be broadcastet to all potential KAs in a
 * network.
 *
 * @author felix
 */
public interface VslAlivePingSender {

    /**
     * Send an alive ping via broadcasting mechanism to as many potential KAs as possible.
     *
     * @param alivePing
     *            the alive ping message.
     */
    void sendAlivePing(VslAlivePing alivePing);
}
