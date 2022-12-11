package org.ds2os.vsl.multicasttransport.fragmentation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.NumberOfSendersOverflowException;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for reassembling fragmentation layer packets.
 *
 * @author Johannes Straßer
 *
 */
public class Assembler implements SubAssemblerCallback {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Assembler.class);

    /**
     * The VslSymmetricKeyStore used by this instance.
     */
    private final VslSymmetricKeyStore keyStore;

    /**
     * The ID of the certificate authority this KA belongs to.
     */
    private final int certificateAuthorityID;

    /**
     * The maximum size of the buffer for authorized messages per sender.
     */
    private final int maxAuthorizedBufferSize;

    /**
     * The maximum size of the buffer for unauthorized messages per sender.
     */
    private final int maxUnauthorizedBufferSize;

    /**
     * The maximum number of different senders that will be allocated a buffer for.
     */
    private final int maxBufferNum;

    /**
     * A map that stores subAssemblers identified by the ID of the public of key the their senders.
     */
    private final HashMap<String, SubAssembler> subAssemblers;

    /**
     * The object that will be notified if a messages reassembly was finished.
     */
    private final AssemblerCallback callback;

    /**
     * The cutoff interval in milliseconds that is used to clean up stale buffers.
     */
    private final long cutoffInterval;

    /**
     * Lock object to secure the subAssemblers Map.
     */
    private final Object subAssemblersLock = new Object();

    /**
     * Constructs an assembler to store incoming fragmentation layer packets until they can be
     * reassembled.
     *
     * @param maxAuthorizedBufferSize
     *            The maximum size of the buffer for authorized messages per source address
     * @param maxUnauthorizedBufferSize
     *            The maximum size of the buffer for unauthorized messages per source address
     * @param maxBufferNum
     *            The maximum number of distinct source addresses that buffers will be allocated for
     * @param callback
     *            The object that will be called if a message's reassembly was finished
     * @param keyStore
     *            The VslSymmetricKeyStore used to check authentication
     * @param cutoffInterval
     *            The interval in milliseconds after which buffers are considered stale
     * @param certificateAuthorityID
     *            The ID of the certificate authority this KA belongs to
     */
    public Assembler(final int maxAuthorizedBufferSize, final int maxUnauthorizedBufferSize,
            final int maxBufferNum, final AssemblerCallback callback,
            final VslSymmetricKeyStore keyStore, final long cutoffInterval,
            final int certificateAuthorityID) {
        this.maxAuthorizedBufferSize = maxAuthorizedBufferSize;
        this.maxUnauthorizedBufferSize = maxUnauthorizedBufferSize;
        this.maxBufferNum = maxBufferNum;
        this.subAssemblers = new HashMap<String, SubAssembler>();
        this.callback = callback;
        this.keyStore = keyStore;
        this.cutoffInterval = cutoffInterval;
        this.certificateAuthorityID = certificateAuthorityID;
    }

    /**
     * Method to store an incoming packet until it can be reassembled into a higher layer payload.
     *
     * @param packet
     *            The packet to be stored
     * @param sourceAddress
     *            Canonical String describing the source address this packet originated from
     * @throws NumberOfSendersOverflowException
     *             Thrown if there is a packet that would increase the number of sender buffers over
     *             the given limit
     */
    public final void digest(final Packet packet, final String sourceAddress)
            throws NumberOfSendersOverflowException {
        // Check if packet uses the right CA
        if (this.certificateAuthorityID != packet.getCertificateAuthorityID()) {
            return;
        }

        // Filter cryptographically invalid packets
        if (packet.usesKnownKey()) {
            if (!packet.hasValidMac()) {
                LOGGER.info("Packet uses a known key, but MAC is invalid; Packet dropped.");
                return;
            }
        } else {
            if (packet.hasEncryptedPayload()) {
                LOGGER.info("Packet encrypted with unknown key; Packet dropped.");
                return;
            }
        }

        // Assign packet to SubAssemblers
        if (!subAssemblers.containsKey(sourceAddress)) {
            // Clean up stale SubAssemblers
            if (cleanBuffers(this.cutoffInterval)) {
                LOGGER.debug("Sucessfully cleaned stale buffers");
            }
            // Try to create a new SubAssembler
            if (subAssemblers.size() < this.maxBufferNum) {
                synchronized (subAssemblersLock) {
                    subAssemblers.put(sourceAddress, new SubAssembler(this.maxAuthorizedBufferSize,
                            this.maxUnauthorizedBufferSize, this, this.cutoffInterval / 2));
                }
            } else {
                LOGGER.warn("Maximum number of senders reached.");
                throw new NumberOfSendersOverflowException(sourceAddress, this.maxBufferNum);
            }
        }
        subAssemblers.get(sourceAddress).digest(packet);
    }

    /**
     * Method to store an incoming packet until it can be reassembled into a higher layer payload.
     *
     * @param packetData
     *            The packet to be stored in byte form
     * @param sourceAddress
     *            Canonical String describing the source address this packet originated from
     * @throws NumberOfSendersOverflowException
     *             Thrown if there is a packet that would increase the number of sender buffers over
     *             the given limit
     */
    public final void digest(final byte[] packetData, final String sourceAddress)
            throws NumberOfSendersOverflowException {
        try {
            digest(new Packet(packetData, this.keyStore, sourceAddress), sourceAddress);
        } catch (final UnkownProtocolVersionException e) {
            LOGGER.warn("Reveiced packet with unknown version number: {}", e.getMessage());
        }
    }

    @Override
    public final void fragmentComplete(final byte[] data, final byte[] keyHash,
            final boolean isEncrypted) {
        this.callback.messageComplete(data, keyHash, isEncrypted);
    }

    /**
     * Remove all buffers that were not used in the last interval milliseconds.
     *
     * FIXME This method creates a replay vulnerability. Buffers may only be cleaned if the keys
     * that were used to create the messages they received are expired. Currently such keys do not
     * expire.
     *
     * @param interval
     *            The cutoff interval in milliseconds
     * @return True if the number of buffers is smaller than their maximum number
     */
    public final boolean cleanBuffers(final long interval) {
        synchronized (subAssemblersLock) {
            final List<String> staleBuffers = new ArrayList<String>();
            final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - interval);
            // Find stale buffers (do not combine with removing)
            for (final Entry<String, SubAssembler> subAss : subAssemblers.entrySet()) {
                if (cutoff.after(subAss.getValue().getLastUsed())) {
                    staleBuffers.add(subAss.getKey());
                }
            }
            // Remove stale buffers (do not combine with finding)
            for (final String staleBuffer : staleBuffers) {
                subAssemblers.remove(staleBuffer);
            }
            return subAssemblers.size() < maxBufferNum;
        }
    }

}
