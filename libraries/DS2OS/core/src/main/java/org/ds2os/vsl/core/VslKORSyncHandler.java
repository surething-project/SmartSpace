package org.ds2os.vsl.core;

/**
 * Handler of received KOR synchronization updates and requests.
 *
 * @author felix
 * @author liebald
 */
public interface VslKORSyncHandler {

    /**
     * Handle a handshake request from another KA.
     *
     * @param handshakeRequest
     *            The handshake data of the request that was received from a KA.
     * @return the handshake data of the reply.
     *
     */
    VslHandshakeData handleHandshakeRequest(VslHandshakeData handshakeRequest);

    /**
     * Handle a KOR update from a KA which is part of the network. This update can be incremental or
     * a full update.
     *
     * @param korUpdate
     *            the update data which was received.
     */
    void handleKORUpdate(VslKORUpdate korUpdate);

    /**
     * Multi-/Broadcasts an incremental update to all connected KAs.
     *
     * @param update
     *            The update to send.
     */
    void sendIncrementalUpdate(VslKORUpdate update);

    /**
     * Checks if an KOR update of the given agent is required. If this is the case
     * (currentHash!=currently locally stored hash of that agent), request an update.
     *
     * @param agentID
     *            The ID of the agent that should be checked.
     * @param currentHash
     *            The current Hash of the agent (from an aliveping).
     */
    void checkKORUpdate(String agentID, String currentHash);

}
