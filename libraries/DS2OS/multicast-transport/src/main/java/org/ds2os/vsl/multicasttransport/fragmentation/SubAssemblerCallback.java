package org.ds2os.vsl.multicasttransport.fragmentation;

/**
 * This interface is used by the SubAssemblers to give back reassembled messages.
 *
 * @author Johannes Straßer
 *
 */
interface SubAssemblerCallback {

    /**
     * This method is called whenever the SubAssembler has completed the assembly of a message.
     *
     * @param data
     *            The payload of the reassembled message
     * @param keyHash
     *            The keyHash of the key used to authenticate and maybe encrypt the packet. Null if
     *            an unknown key was used
     * @param isEncrypted
     *            If the payload is encrypted
     */
    void fragmentComplete(final byte[] data, final byte[] keyHash, final boolean isEncrypted);
}
