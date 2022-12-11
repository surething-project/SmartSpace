package org.ds2os.vsl.multicasttransport.fragmentation;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for SubAssemblers that will assemble incoming packets of _one_ sender into higher layer
 * payloads.
 *
 * @author Johannes Straßer
 *
 */
class SubAssembler implements FragmentBufferCallback {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SubAssembler.class);

    /**
     * The current quota left for authorized packets in bytes.
     */
    private int authorizedBufferQuota;

    /**
     * The current quota left for unauthorized packets in bytes.
     */
    private int unauthorizedBufferQuota;

    /**
     * If this sender has ever successfully used a known key.
     */
    private boolean knownSender = false;

    /**
     * The highest packet number that was received to date. Starts as -1 as no packets were received
     * at the start.
     */
    private int packetNumber = -1;

    /**
     * Set containing the packet numbers of all expected packets.
     */
    private final HashSet<Integer> expectedPackets = new HashSet<Integer>();

    /**
     * A map of authorized buffers identified by their first packets.
     */
    private final HashMap<Integer, FragmentBuffer> authorizedBuffer = new HashMap<Integer, FragmentBuffer>();

    /**
     * A map of unauthorized buffers identified by their first packets.
     */
    private final HashMap<Integer, FragmentBuffer> unauthorizedBuffer = new HashMap<Integer, FragmentBuffer>();

    /**
     * The object that will be notified if a messages assembly is completed.
     */
    private final SubAssemblerCallback callback;

    /**
     * This time stamp shows when the last fragment was successfully inserted into an underlying
     * FragmentBuffer.
     */
    private Timestamp lastUsed;

    /**
     * Lock object to make sure packets are digested sequentially.
     */
    private final Object digestLock = new Object();

    /**
     * Creates a new subAssembler to assemble packets of exactly one sender. Giving it packets of
     * different senders will create errors. Employs space quotas for buffer allocation.
     *
     * @param maxAuthorizedBufferSize
     *            The maximum size of the buffer for authorized packets
     * @param maxUnauthorizedBufferSize
     *            The maximum size of the buffer for unauthorized packets
     * @param callback
     *            The object that will be called if a message's assembly is complete
     * @param cutoffInterval
     *            The cutoff interval in milliseconds that is used to clean up stale
     *            FragmentBuffers.
     */
    SubAssembler(final int maxAuthorizedBufferSize, final int maxUnauthorizedBufferSize,
            final SubAssemblerCallback callback, final long cutoffInterval) {
        this.authorizedBufferQuota = maxAuthorizedBufferSize;
        this.unauthorizedBufferQuota = maxUnauthorizedBufferSize;
        this.callback = callback;
        this.lastUsed = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Stores the given packet in a buffer (or creates a buffer if necessary). Will drop the packet
     * if it was already seen.
     *
     * @param packet
     *            The packet to be stored.
     */
    void digest(final Packet packet) {
        synchronized (digestLock) {
            // Check quota
            while (packet.getData().length > getBufferQuota(packet)) {
                // Remove old buffers until enough space is freed
                int firstPacket = Integer.MAX_VALUE;
                for (final Integer i : getBufferList(packet).keySet()) {
                    if (i < firstPacket) {
                        firstPacket = i;
                    }
                }
                // Break if not enough space can be freed
                if (firstPacket == Integer.MAX_VALUE) {
                    LOGGER.error("Packet ({} bytes) bigger than quota for subAssembler ({} bytes)",
                            packet.getData().length, getBufferQuota(packet));
                    return;
                }
                removeBuffer(firstPacket);
            }

            // Handle packets from expectedPackets list (only authorized packets are there)
            if (this.expectedPackets.contains(packet.getPacketNumber())) {
                if (packet.hasValidMac()) {
                    insertIntoBuffer(packet);
                    this.expectedPackets.remove(packet.getPacketNumber());
                }
                return;
            }

            // Handle new packets
            if (packet.getPacketNumber() > this.packetNumber) {
                // Handle non fragments
                if (packet.getLeadingFragments() == 0 && packet.getTrailingFragments() == 0) {
                    if (packet.hasValidMac()) {
                        LOGGER.debug("Unfragmented, authorized packet with packet number "
                                + packet.getPacketNumber());
                        this.knownSender = true;
                        this.packetNumber = packet.getPacketNumber();
                    } else {
                        LOGGER.debug("Unfragmented, unauthorized packet with packet number "
                                + packet.getPacketNumber());
                    }
                    this.callback.fragmentComplete(packet.getData(),
                            packet.hasValidMac() ? packet.getGroupKeyHash() : null,
                            packet.hasEncryptedPayload());
                    return;
                }

                /*
                 * Handle fragments
                 */
                // Create new buffer if necessary (Necessary for unauthenticated packets)
                if (!bufferExists(packet)) {
                    createBuffer(packet);
                }

                // Update expectedPackets and packetNumber (if authorized packet)
                if (packet.hasValidMac()) {
                    for (int i = packet.getPacketNumber()
                            - packet.getLeadingFragments(); i <= packet.getPacketNumber()
                                    + packet.getTrailingFragments(); i++) {
                        expectedPackets.add(i);
                        LOGGER.debug("Added Packet to expectedPackets:" + i);
                    }
                    expectedPackets.remove(packet.getPacketNumber());
                    this.packetNumber = packet.getPacketNumber() + packet.getTrailingFragments();
                }

                // Actually insert packet (this also updates buffer sizes)
                insertIntoBuffer(packet);
                return;
            }

            // Handle stale packets
            // LOGGER.debug("Received a packet with the stale packet number "
            // + packet.getPacketNumber() + ". Dropped packet.");
            return;
        }

    }

    /**
     * Tries to create a buffer that will accept the given packet and all other packets of the same
     * message.
     *
     * @param packet
     *            The packet defining the message to be contained in the buffer
     */
    private void createBuffer(final Packet packet) {
        if (packet.usesKnownKey()) {
            this.knownSender = true;
        }
        getBufferList(packet).put(packet.getPacketNumber() - packet.getLeadingFragments(),
                new FragmentBuffer(packet.getLeadingFragments() + 1 + packet.getTrailingFragments(),
                        packet.getPacketNumber() - packet.getLeadingFragments(),
                        packet.hasValidMac(), this));
    }

    /**
     * Checks if a buffer that could accept the given packet already exists.
     *
     * @param packet
     *            The packet that is to be checked for
     * @return True if a suiting buffer exists False if not
     */
    private boolean bufferExists(final Packet packet) {
        return getBufferList(packet)
                .containsKey(packet.getPacketNumber() - packet.getLeadingFragments());
    }

    /**
     * Tries to insert a packet into the suiting buffer. Method assumes that there is a buffer
     * suitable for this packet and that there is enough quota remaining.
     *
     * @param packet
     *            The packet to be inserted
     */
    private void insertIntoBuffer(final Packet packet) {
        final FragmentBuffer buffer = getBufferList(packet)
                .get(packet.getPacketNumber() - packet.getLeadingFragments());
        if (buffer == null) {
            throw new IllegalStateException(
                    "No suitable buffer found for packet " + packet.getPacketNumber());
        }
        if (!reserveBufferQuota(packet)) {
            throw new IllegalStateException("insertIntoBuffer was called with a Packet"
                    + " exceeding the reamining space quota.");
        }
        buffer.insertPacket(packet);
        this.lastUsed = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Checks if the sender associated with this subAssembler is using a known key.
     *
     * @return True if the senders key is known, False if not
     */
    final boolean knownSender() {
        return this.knownSender;
    }

    /**
     * Gets the remaining quota suitable for the given packet (based on usesKnownKey).
     *
     * @param packet
     *            The normative packet
     * @return The remaining quota of the buffer this packet belongs to
     */
    private int getBufferQuota(final Packet packet) {
        if (packet.usesKnownKey()) {
            return this.authorizedBufferQuota;
        } else {
            return this.unauthorizedBufferQuota;
        }
    }

    /**
     * Tries to reserve space quota for the given packet.
     *
     * @param packet
     *            Packet defining size and authorization of required quota
     * @return True if quota could be reserved, False if not
     */
    private boolean reserveBufferQuota(final Packet packet) {
        final int size = packet.getData().length;
        if (packet.usesKnownKey()) {
            if (this.authorizedBufferQuota >= size) {
                this.authorizedBufferQuota -= size;
                LOGGER.debug("Reserved some space. Authorized Quota is now {}",
                        this.authorizedBufferQuota);
                return true;
            } else {
                return false;
            }
        } else {
            if (this.unauthorizedBufferQuota >= size) {
                this.unauthorizedBufferQuota -= size;
                LOGGER.debug("Reserved some space. Unauthorized Quota is now {}",
                        this.unauthorizedBufferQuota);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Gets the buffer suitable for the given packet (based on usesKnownKey).
     *
     * @param packet
     *            The normative packet
     * @return The buffer this packet belongs to
     */
    private HashMap<Integer, FragmentBuffer> getBufferList(final Packet packet) {
        if (packet.usesKnownKey()) {
            return this.authorizedBuffer;
        } else {
            return this.unauthorizedBuffer;
        }
    }

    /**
     * Removes a FragmentBuffer from the local buffers. Also removes the contained packets from the
     * missedPackets list and updates the respective quota.
     *
     * @param firstPacket
     *            The packet number to identify the FragmentBuffer
     */
    private void removeBuffer(final int firstPacket) {
        FragmentBuffer buffer = this.unauthorizedBuffer.remove(firstPacket);
        if (buffer == null) {
            buffer = this.authorizedBuffer.remove(firstPacket);
            if (buffer == null) {
                LOGGER.debug("Tried to remove non existing FragmentBuffer " + firstPacket);
                return;
            }
            this.authorizedBufferQuota += buffer.getSize();
            LOGGER.debug("Removed a message from the authorized buffer. New quota: "
                    + this.authorizedBufferQuota);
        } else {
            this.unauthorizedBufferQuota += buffer.getSize();
            LOGGER.debug("Removed a message from the unauthorized buffer. New quota: "
                    + this.unauthorizedBufferQuota);
        }
        for (int i = firstPacket; i < buffer.getLength() + firstPacket; i++) {
            this.expectedPackets.remove(i);
        }
    }

    @Override
    public void fragmentComplete(final byte[] data, final int firstPacket, final byte[] keyHash,
            final boolean isEncrypted) {
        removeBuffer(firstPacket);
        this.callback.fragmentComplete(data, keyHash, isEncrypted);
    }

    /**
     * Returns a time stamp marking the time this SubAssembler last stored a new fragment into one
     * of its buffers.
     *
     * @return The time this SubAssembler was last (successfully) active
     */
    public final Timestamp getLastUsed() {
        return this.lastUsed;
    }

}
