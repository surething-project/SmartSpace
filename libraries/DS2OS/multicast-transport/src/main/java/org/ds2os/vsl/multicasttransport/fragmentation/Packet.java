package org.ds2os.vsl.multicasttransport.fragmentation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that creates and validates fragmentation layer packets.
 *
 * Header:
 *
 * <pre>
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                    certificate authority ID                   |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         packet number                         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |       leading Fragments       |       trailing Fragments      |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      groupKey ID              |E|   Version   |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    .                                                               .
 *    .                              MAC                              .
 *    .                                                               .
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    .                                                               .
 *    .                             Data                              .
 *    .                                                               .
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * TODO As the MAC is created over the whole packet as well as its source address, MACs of packets
 * going through a NAT will always show up as incorrect.
 *
 * @author Johannes Straßer
 *
 */

class Packet {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Packet.class);

    /**
     * Charset to encode the sourcesAddress String.
     */
    private static final String CHARSET = "UTF-8";

    /**
     * VslSymmetricKeyStore used by this instance.
     */
    private final VslSymmetricKeyStore keyStore;

    /**
     * The MAC of the header.
     */
    private final byte[] mac;

    /**
     * The ID of the sender's certificate authority.
     */
    private final int certificateAuthorityID;

    /**
     * The hash of the used symmetric key (by generateGroupkeyHash()). A 256bit SHA-256 hash value
     * which gets shortened to GROUPKEYHASH_LENGTH bytes.
     */
    private final byte[] groupKeyHash;

    /**
     * A field containing a flag and the version number.
     *
     * Bit 1: encrypted payload flag. Bits 2 - 8: protocol version number.
     *
     * Bits are ordered in big endian.
     */
    private final byte flags;

    /**
     * The number of this packet.
     */
    private final int packetNumber;

    /**
     * The number of packets that came before this packet and belong to the same message.
     */
    private final short leadingFragments;

    /**
     * The number of packets that will come after this packet and belong to the same message.
     */
    private final short trailingFragments;

    /**
     * If the symmetric encryption key is known by this class.
     */
    private final boolean usesKnownKey;

    /**
     * If the MAC tag is valid. Always false if knownKey is false.
     */
    private final boolean hasValidMac;

    /**
     * This packet in raw bytes.
     */
    private final byte[] rawPacket;

    /**
     * The length of the groupKeyHash in bytes.
     */
    static final int GROUPKEYHASH_LENGTH = 3;

    /**
     * The length of the MAC in bytes.
     */
    private final short macLength;

    /**
     * The length of this header in bytes.
     */
    private final int headerLength;

    /**
     * Constructor to build a new packet header (and packet header object) from the given
     * parameters.
     *
     * @param certificateAuthorityID
     *            The ID of the certificate authority of the sender
     * @param packetNumber
     *            The number of this packet
     * @param leadingFragments
     *            The number of packets that came before this packet and belong to the same message
     * @param trailingFragments
     *            The number of packets that will come after this packet and belong to the same
     *            message
     * @param encrypted
     *            If the payload of this packet is encrypted
     * @param keyHash
     *            The keyHash to create this packets MAC tag
     * @param data
     *            The data for this packet
     * @param keyStore
     *            The key store used to create this packets MAC
     * @param sourceAddress
     *            An address string identifying the source address of this packet
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    Packet(final int certificateAuthorityID, final int packetNumber, final short leadingFragments,
            final short trailingFragments, final boolean encrypted, final byte[] keyHash,
            final byte[] data, final VslSymmetricKeyStore keyStore, final String sourceAddress)
            throws KeyNotInKeystoreException {
        // Write class variables
        this.certificateAuthorityID = certificateAuthorityID;
        this.groupKeyHash = Arrays.copyOf(keyHash, GROUPKEYHASH_LENGTH);
        this.packetNumber = packetNumber;
        this.leadingFragments = leadingFragments;
        this.trailingFragments = trailingFragments;
        this.usesKnownKey = true;
        this.keyStore = keyStore;
        this.macLength = getMACLength(this.keyStore.getKeyString(keyHash));

        // Calculate flags field
        byte version;
        try {
            version = KeyStringParser.macLengthToVersion(this.macLength);
        } catch (final UnkownProtocolVersionException e) {
            // Should not happen as this should be thrown when adding the key to the keystore.
            LOGGER.error(e.getMessage());
            version = 0;
        }
        this.flags = (byte) (version * (encrypted ? -1 : 1));
        this.headerLength = getHeaderLength(this.macLength);

        /*
         * Write packet header and data.
         *
         * The MAC is computed over the packet's header, data, and a string based on the packet's
         * source address. The address is not included in the packet later on.
         */
        // Initialize variables
        final byte[] sourceAddressBytes = sourceAddress.getBytes(Charset.forName(CHARSET));
        final ByteBuffer bb = ByteBuffer
                .allocate(headerLength + data.length + sourceAddressBytes.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        // Write header fields into buffer
        bb.putInt(certificateAuthorityID);
        bb.putInt(packetNumber);
        bb.putShort(leadingFragments);
        bb.putShort(trailingFragments);
        bb.put(this.groupKeyHash);
        bb.put(flags);
        // Fill place for MAC with 0s
        bb.mark();
        bb.put(new byte[this.macLength]);
        // Insert data
        bb.put(data);
        // Append source address
        bb.put(sourceAddressBytes);
        // Compute and add MAC tag
        bb.reset();
        this.mac = Arrays.copyOf(this.keyStore.generateMAC(this.groupKeyHash, bb.array()),
                this.macLength);
        bb.put(this.mac);
        this.hasValidMac = true;
        // Write packet (without the source address)
        bb.position(0);
        this.rawPacket = new byte[headerLength + data.length];
        bb.get(this.rawPacket);
        // LOGGER.debug("Successfully built a Layer 1 packet");
    }

    /**
     * Constructor to parse a byte array and build a new packet object from it.
     *
     * @param rawPacket
     *            The byte array that is to be parsed
     * @param keyStore
     *            The key store used to create this packets MAC
     * @param sourceAddress
     *            An address string identifying the source address of this packet
     * @throws UnkownProtocolVersionException
     *             Thrown if the given packet uses an unknown protocol number
     */
    Packet(final byte[] rawPacket, final VslSymmetricKeyStore keyStore, final String sourceAddress)
            throws UnkownProtocolVersionException {
        // Set keyStore
        this.keyStore = keyStore;

        // Extract information
        this.rawPacket = rawPacket;
        final ByteBuffer bb = ByteBuffer.wrap(rawPacket);
        bb.order(ByteOrder.BIG_ENDIAN);
        this.certificateAuthorityID = bb.getInt();
        this.packetNumber = bb.getInt();
        this.leadingFragments = bb.getShort();
        this.trailingFragments = bb.getShort();
        this.groupKeyHash = new byte[GROUPKEYHASH_LENGTH];
        bb.get(this.groupKeyHash);
        this.flags = bb.get();
        this.macLength = KeyStringParser
                .versionToMacLength(this.flags < 0 ? (byte) -this.flags : this.flags);

        // Set header length
        this.headerLength = getHeaderLength(this.macLength);

        /*
         * Check authentication
         */
        // Get MAC tag
        bb.mark();
        this.mac = new byte[this.macLength];
        bb.get(this.mac);
        // MAC must be calculated with zeroed MAC field
        bb.reset();
        bb.put(new byte[this.macLength]);
        // MAC must be calculated with appended source address
        final byte[] sourceAddressBytes = sourceAddress.getBytes(Charset.forName(CHARSET));
        final ByteBuffer macbb = ByteBuffer.allocate(bb.capacity() + sourceAddressBytes.length);
        bb.position(0);
        macbb.put(bb);
        macbb.put(sourceAddressBytes);
        // Check MAC
        boolean localUsesKnownKey;
        boolean localHasValidMac;
        try {
            this.keyStore.getKey(this.groupKeyHash);
            localUsesKnownKey = true;
            localHasValidMac = Arrays.equals(this.mac, Arrays.copyOf(
                    this.keyStore.generateMAC(this.groupKeyHash, macbb.array()), macLength));
        } catch (final KeyNotInKeystoreException e) {
            localUsesKnownKey = false;
            localHasValidMac = false;
        }
        this.usesKnownKey = localUsesKnownKey;
        this.hasValidMac = localHasValidMac;
        // LOGGER.debug("Successfully parsed a Layer 1 packet");
    }

    /**
     * Returns the length of the MAC that is specified in the given keyString.
     *
     * @param keyString
     *            The keyString
     * @return The length of the MAC in bytes
     */
    private static short getMACLength(final String keyString) {
        return (short) (new KeyStringParser(keyString).getMACKeyLength() / 8);
    }

    /**
     * Returns the length of the fragmentation layer header in bytes.
     *
     * @param keyString
     *            The keyString used for creating this packet's MAC
     * @return Length of fragmentation layer header in bytes
     */
    static int getHeaderLength(final String keyString) {
        return getHeaderLength(getMACLength(keyString));
    }

    /**
     * Returns the length of the fragmentation layer header in bytes.
     *
     * @param macLength
     *            The length of the used MAC in bytes
     * @return Length of the fragmentation layer header in bytes
     */
    private static int getHeaderLength(final short macLength) {
        final int result = Integer.SIZE / 8 // Integer certificateAuthorityID
                + Integer.SIZE / 8 // Integer packetNumber
                + Short.SIZE / 8 // Short leadingFragments
                + Short.SIZE / 8 // Short trailingFragments
                + GROUPKEYHASH_LENGTH // byte[] groupKeyHash
                + Byte.SIZE / 8 // byte flags
                + macLength; // byte[] MAC tag
        return result;
    }

    /**
     * Returns the length of this packet's header.
     *
     * @return The length of this packet's header in bytes
     */
    int getHeaderLength() {
        return this.headerLength;
    }

    /**
     * Returns the ID of the certificate authority of the current header object.
     *
     * @return The ID of the certificate authority
     */
    int getCertificateAuthorityID() {
        return certificateAuthorityID;
    }

    /**
     * Returns the packet number of the current header object.
     *
     * @return The packet number
     */
    int getPacketNumber() {
        return packetNumber;
    }

    /**
     * Returns the number of packets that came before this packet and belong to the same message.
     *
     * @return The number of leading packets
     */
    short getLeadingFragments() {
        return leadingFragments;
    }

    /**
     * Returns the number of packets that will come after this packet and belong to the same
     * message.
     *
     * @return The number of trailing fragments
     */
    short getTrailingFragments() {
        return trailingFragments;
    }

    /**
     * Returns the hash of the symmetric key used to generate this headers MAC tag.
     *
     * @return The hash of the GroupKey
     */
    byte[] getGroupKeyHash() {
        return groupKeyHash;
    }

    /**
     * If the symmetric key used for this header's MAC is contained in the given
     * VslSymmetricKeyStore.
     *
     * @return True if the key is known, False if not
     */
    boolean usesKnownKey() {
        return usesKnownKey;
    }

    /**
     * If the header has a valid MAC tag. Invalid tags can stem from wrong MACs as well as unknown
     * keys.
     *
     * @return True if the MAC tag could be validated, False if not
     */
    boolean hasValidMac() {
        return hasValidMac;
    }

    /**
     * If the payload of the packet is marked as encrypted.
     *
     * @return True is the payload is marked as encrypted, false if not
     */
    boolean hasEncryptedPayload() {
        // Java treats byte as signed and the checked bit is the highest
        return this.flags < 0;
    }

    /**
     * Returns the data payload of this packet.
     *
     * @return This packets data
     */
    byte[] getData() {
        return Arrays.copyOfRange(rawPacket, headerLength, rawPacket.length);
    }

    /**
     * Returns the header of this packet.
     *
     * @return This packets header
     */
    byte[] getHeader() {
        return Arrays.copyOfRange(rawPacket, 0, headerLength);
    }

    /**
     * Returns this packet as bytes.
     *
     * @return This packet
     */
    byte[] getRawPacket() {
        return rawPacket;
    }

}
