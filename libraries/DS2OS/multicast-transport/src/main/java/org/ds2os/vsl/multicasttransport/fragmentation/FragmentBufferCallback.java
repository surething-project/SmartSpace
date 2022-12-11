package org.ds2os.vsl.multicasttransport.fragmentation;

/**
 * This interface is used by the FragmentBuffers to give back reassembled messages.
 *
 * @author Johannes Straßer
 *
 */
interface FragmentBufferCallback {

    /**
     * This method is called whenever a FragentBuffer has completed the assembly of a message.
     *
     * @param data
     *            The data of the reassembled message
     * @param firstPacket
     *            The packet number of the first packet of this message
     * @param keyHash
     *            The keyHash of the key used to authenticate and maybe encrypt the packet. Null if
     *            an unknown key was used
     * @param isEncrypted
     *            If the data is encrypted
     */
    void fragmentComplete(final byte[] data, final int firstPacket, final byte[] keyHash,
            final boolean isEncrypted);

}
