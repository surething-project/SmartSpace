package org.ds2os.vsl.multicasttransport.fragmentation;

/**
 * This interface is used by the Assembler class to give back reassembled payloads.
 *
 * @author Johannes Straßer
 *
 */
public interface AssemblerCallback {
    /**
     * This method is called whenever an Assembler has completed the assembly of a higher layer
     * payload.
     *
     * @param messageData
     *            The reassembled payload
     * @param keyHash
     *            The keyHash of the key used to authenticate and maybe encrypt the packet. Null if
     *            an unknown key was used
     * @param isEncrypted
     *            If the messageData is encrypted
     */
    void messageComplete(final byte[] messageData, final byte[] keyHash, final boolean isEncrypted);
}
