package org.ds2os.vsl.multicasttransport.fragmentation;

import java.nio.ByteBuffer;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that is used by the SubAssemblers to store incoming packets.
 *
 * @author Johannes Straßer
 *
 */
class FragmentBuffer {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FragmentBuffer.class);

    /**
     * The packet number of the first packet that will be accepted.
     */
    private final int firstPacket;

    /**
     * If the message in this buffer consists of authenticated packets or not.
     */
    private final boolean isAuthenticated;

    /**
     * The buffer incoming packets are saved in.
     */
    private final byte[][] data;

    /**
     * A set containing the numbers of all needed packets that were not received yet.
     */
    private final HashSet<Integer> missingPackets;

    /**
     * The size of the message in bytes. Starts at 0 and grows as packets are inserted.
     */
    private int realSize;

    /**
     * The object that will be called if the buffered message is complete.
     */
    private final FragmentBufferCallback callback;

    /**
     * Constructs a new FragmentBuffer.
     *
     * @param length
     *            The length of the buffer in packets
     * @param firstPacket
     *            The packet number of the first packet that will be accepted
     * @param isAuthenticated
     *            If the packets in this buffer are required to be authenticated
     * @param callback
     *            The object that will be called if the buffered message is complete
     */
    FragmentBuffer(final int length, final int firstPacket, final boolean isAuthenticated,
            final FragmentBufferCallback callback) {
        this.firstPacket = firstPacket;
        this.isAuthenticated = isAuthenticated;
        this.data = new byte[length][];
        missingPackets = new HashSet<Integer>();
        for (int i = firstPacket; i < firstPacket + this.data.length; i++) {
            // LOGGER.debug("Missing packet:" + i);
            this.missingPackets.add(i);
        }
        this.realSize = 0;
        this.callback = callback;
    }

    /**
     * Tries to insert the given packet into the FragmentBuffer.
     *
     * @param packet
     *            The packet to be inserted
     */
    final void insertPacket(final Packet packet) {
        // Checks if the packet is suitable for this buffer
        if (!authCheck(packet)) {
            return;
        }

        // Insert data
        this.data[packet.getPacketNumber() - this.firstPacket] = packet.getData();
        LOGGER.debug("Wrote packet " + packet.getPacketNumber() + " into buffer.");

        // Update control structures
        this.realSize += packet.getData().length;
        this.missingPackets.remove(packet.getPacketNumber());

        // Check if message is completed
        if (this.missingPackets.isEmpty()) {
            final ByteBuffer message = ByteBuffer.allocate(this.realSize);
            for (final byte[] fragment : this.data) {
                message.put(fragment);
            }
            LOGGER.debug("Sucessfully reassemled a message");
            callback.fragmentComplete(message.array(), this.firstPacket,
                    packet.hasValidMac() ? packet.getGroupKeyHash() : null,
                    packet.hasEncryptedPayload());
        }
    }

    /**
     * Makes sure that only authenticated packets are added to an authenticated buffer. Throws a
     * runtime exception if the packet does not fit.
     *
     * @param packet
     *            The packet that is to be tested
     * @return True if the packet is valid, false if not
     */
    private boolean authCheck(final Packet packet) {
        if (this.isAuthenticated && !packet.hasValidMac()) {
            LOGGER.debug("Packet {} has invalid MAC", packet.getPacketNumber());
            return false;
        }
        if (this.missingPackets.contains(packet.getPacketNumber())) {
            return true;
        } else {
            throw new IllegalStateException("InsertPacket was called with an unexpected Packet.");
        }
    }

    /**
     * Returns the length of this FragmentBuffer.
     *
     * @return The length of this FragmentBuffer in packets
     */
    final int getLength() {
        return this.data.length;
    }

    /**
     * Returns the size of the data this FragmentBuffer currently holds.
     *
     * @return The size of the data this FragmentBuffer currently holds in bytes.
     */
    final int getSize() {
        return this.realSize;
    }

}
