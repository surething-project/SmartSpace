package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.KeyNotInKeystoreException;

/**
 * Sender of {@link VslKORUpdate} messages. These must be broadcastet to all potential KAs in a
 * network.
 *
 * @author felix
 */
public interface VslKORUpdateSender {

    /**
     * Send a KOR update via multicast to all connected KAs. The update is encrypted with the
     * current key.
     *
     * @param korUpdate
     *            the KOR update.
     */
    void sendKORUpdate(VslKORUpdate korUpdate);

    /**
     * Send a KOR update via multicast to all connected KAs. The update is encrypted with the key
     * belonging to the given keyHash.
     *
     * @param korUpdate
     *            the KOR update.
     * @param keyHash
     *            hash of the key that is used for encryption
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    void sendKORUpdate(VslKORUpdate korUpdate, byte[] keyHash) throws KeyNotInKeystoreException;
}
