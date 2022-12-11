package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.VslException;

/**
 * Special connector for KA to KA synchronization, i.e. handshake and KOR sync information.
 *
 * @author felix
 */
public interface VslKASyncConnector {

    /**
     * Initiates a Handshake with another KA.
     *
     * @param localData
     *            The {@link VslHandshakeData} of the local KA.
     * @return The {@link VslHandshakeData} of the remote KA.
     * @throws VslException
     *             Thrown if something goes wrong during the handshake.
     */
    VslHandshakeData doHandshake(VslHandshakeData localData) throws VslException;

    /**
     * Requests an {@link VslKORUpdate} from an remote KA. Update can be full or incremental. It is
     * incremental if hashFrom is set.
     *
     * @param agentId
     *            The KA to request the Update from.
     * @param hashFrom
     *            Hash of the currently known version of the remote KAs structure. Empty String if a
     *            full update is requested.
     * @return {@link VslKORUpdate} from the remote KOR.
     * @throws VslException
     *             Thrown if something goes wrong during the handshake.
     */
    VslKORUpdate requestUpdate(String agentId, String hashFrom) throws VslException;
}
