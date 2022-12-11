package org.ds2os.vsl.multicasttransport.fragmentation;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that takes data and prepares it for sending. This includes fragmenting the data and
 * prepending layer 1 headers.
 *
 * @author Johannes Straßer
 *
 */
public class Fragmenter {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Fragmenter.class);

    /**
     * The VslSymmetricKeyStore used by this instance.
     */
    private final VslSymmetricKeyStore keyStore;

    /**
     * The ascending number of the current packet.
     */
    private int packetNumber;

    /**
     * The ID of the certificate authority of the sender.
     */
    private final int certfificateAuthorityID;

    /**
     * The MTU this Fragmenter fragments for.
     */
    private final int mtu;

    /**
     * The source address that is used to send the packets provided by this Fragmenter.
     */
    private final String sourceAddress;

    /**
     * Constructor for a Fragmenter that will fragment a payload to make it suitable for lower
     * layers. It will also prepend the necessary lower layer header.
     *
     * @param mtu
     *            The MTU of the link that is to be used for sending the fragmented packets
     * @param certfificateAuthorityID
     *            The ID of the certificate authority of the sender
     * @param keyStore
     *            The VslSymmetricKeyStore used during Packet creation
     * @param sourceAddress
     *            The source address that is used to send the packets provided by this Fragmenter
     */
    public Fragmenter(final int mtu, final int certfificateAuthorityID,
            final VslSymmetricKeyStore keyStore, final String sourceAddress) {
        this.packetNumber = 0;
        this.certfificateAuthorityID = certfificateAuthorityID;
        this.keyStore = keyStore;
        this.mtu = mtu;
        this.sourceAddress = sourceAddress;
    }

    /**
     * This fragments given data into fragmentation layer packets that can be sent over a link with
     * the given MTU. The key to the given keyHash must be the same the data was encrypted with.
     *
     * @param data
     *            The data to be fragmented
     * @param isEncrypted
     *            If the data is encrypted
     * @param keyHash
     *            The hash of the key used for signing
     * @return A list of fragmentation layer packets containing fragments of the given data.
     * @throws InvalidParameterException
     *             Thrown if the chosen MTU is too small to create valid packets or the given
     *             keyHash is not found in the given keyStore
     */
    public final List<byte[]> fragmentData(final byte[] data, final boolean isEncrypted,
            final byte[] keyHash) throws InvalidParameterException {
        String keyString;
        // Get keyString from keyStore
        try {
            keyString = this.keyStore.getKeyString(keyHash);
        } catch (final KeyNotInKeystoreException e) {
            LOGGER.warn("KeyHash was not found in the given keyStore.");
            throw new InvalidParameterException("KeyHash was not found in the given keyStore.");
        }
        // Check if MTU is big enough
        final int payloadSize = this.mtu - Packet.getHeaderLength(keyString);
        if (payloadSize <= 0) {
            LOGGER.warn("MTU too small to create valid packets.");
            throw new InvalidParameterException("MTU too small to create valid packets.");
        }

        // Initiate parameters
        int relPacketnumber = 0;
        short leadingPackets = 0;
        short trailingPackets = (short) (data.length / payloadSize);
        final List<byte[]> packets = new ArrayList<byte[]>();
        if ((data.length % payloadSize) == 0) {
            trailingPackets--;
        }
        // Create fragments
        try {
            while (trailingPackets >= 0) {
                byte[] packetData;
                if (trailingPackets > 0) {
                    packetData = Arrays.copyOfRange(data, relPacketnumber * payloadSize,
                            (relPacketnumber + 1) * payloadSize);
                } else {
                    packetData = Arrays.copyOfRange(data, relPacketnumber * payloadSize,
                            data.length);
                }
                Packet packet;
                packet = new Packet(this.certfificateAuthorityID, this.packetNumber, leadingPackets,
                        trailingPackets, isEncrypted, keyHash, packetData, this.keyStore,
                        this.sourceAddress);
                packets.add(packet.getRawPacket());
                this.packetNumber++;
                relPacketnumber++;
                leadingPackets++;
                trailingPackets--;
            }
            LOGGER.debug("Fragmented data into " + (leadingPackets) + " layer 1 fragments.");
            return packets;
        } catch (final KeyNotInKeystoreException e) {
            // Should not happen as this is checked at the method's start.
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    /**
     * Gets the source address this Fragmenter uses.
     *
     * @return This Fragmenter's used source address
     */
    public final String getSourceAddress() {
        return sourceAddress;

    }

}
