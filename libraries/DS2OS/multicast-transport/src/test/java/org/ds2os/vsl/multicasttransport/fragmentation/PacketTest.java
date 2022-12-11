package org.ds2os.vsl.multicasttransport.fragmentation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.ds2os.vsl.netutils.TestHelper;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for Packet.
 *
 * @author Johannes Straßer
 *
 */
public class PacketTest {

    /**
     * A randomly generated key that can be used by tests.
     */
    private byte[] keyHash;

    /**
     * The VslSymmetricKeyStore for tests to use.
     */
    private VslSymmetricKeyStore keyStore;

    /**
     * Source address String for tests.
     */
    private String sourceAddress;

    /**
     * Set up method to initialize global fields.
     */
    @Before
    public final void setUp() {
        final String tlsString = "TLS_PSK_WITH_NULL_SHA384";
        final byte[] key = new KeyStringParser(tlsString).createKey();
        this.keyStore = new SymmetricKeyStore();
        this.keyStore.addKey(key, tlsString);
        this.keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        this.sourceAddress = "udp://192.168.0.1:12345";
    }

    /**
     * Standard test for the constructors.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacket()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        byte[] data;
        Packet packet;
        Packet packet2;
        short leadingPackets;
        short trailingPackets;

        // Test a packet
        leadingPackets = 0;
        trailingPackets = 100;
        data = TestHelper.randomData(300);
        packet = new Packet(222222, 1234, leadingPackets, trailingPackets, true, this.keyHash, data,
                this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(222222));
        assertThat(packet.getPacketNumber(), is(1234));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is((short) 100));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet.getGroupKeyHash(), is(this.keyHash));
        assertThat(packet.getData(), is(data));

        packet2 = new Packet(packet.getRawPacket(), this.keyStore, this.sourceAddress);
        assertThat(packet2.getCertificateAuthorityID(), is(222222));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(1234));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(true));
        assertThat(packet2.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet2.getData(), is(data));
    }

    /**
     * Test for the constructors using SHA1 as MAC algorithm.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacketMAC0()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        String tlsString;
        byte[] data;
        Packet packet;
        Packet packet2;
        byte[] key;
        short leadingPackets;
        short trailingPackets;

        /*
         * Test a packet using SHA1
         */
        tlsString = "TLS_PSK_WITH_NULL_SHA";
        key = new KeyStringParser(tlsString).createKey();
        this.keyStore.addKey(key, tlsString);
        keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        leadingPackets = 0;
        trailingPackets = 100;

        // Create Packet
        data = TestHelper.randomData(300);
        packet = new Packet(222222, 1234, leadingPackets, trailingPackets, true, this.keyHash, data,
                this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(222222));
        assertThat(packet.getPacketNumber(), is(1234));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is(trailingPackets));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet.getGroupKeyHash(), is(this.keyHash));
        assertThat(packet.getData(), is(data));

        // Parse Packet
        packet2 = new Packet(packet.getRawPacket(), this.keyStore, this.sourceAddress);
        assertThat(packet2.getCertificateAuthorityID(), is(222222));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(1234));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(true));
        assertThat(packet2.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet2.getData(), is(data));
    }

    /**
     * Test for the constructors using SHA256 as MAC algorithm.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacketMAC1()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        String tlsString;
        byte[] data;
        Packet packet;
        Packet packet2;
        byte[] key;
        short leadingPackets;
        short trailingPackets;
        /*
         * Test a packet using SHA256
         */
        tlsString = "TLS_PSK_WITH_NULL_SHA256";
        key = new KeyStringParser(tlsString).createKey();
        this.keyStore.addKey(key, tlsString);
        keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        leadingPackets = 0;
        trailingPackets = 100;

        // Create Packet
        data = TestHelper.randomData(300);
        packet = new Packet(222222, 1234, leadingPackets, trailingPackets, true, this.keyHash, data,
                this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(222222));
        assertThat(packet.getPacketNumber(), is(1234));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is(trailingPackets));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet.getGroupKeyHash(), is(this.keyHash));
        assertThat(packet.getData(), is(data));

        // Parse Packet
        packet2 = new Packet(packet.getRawPacket(), this.keyStore, this.sourceAddress);
        assertThat(packet2.getCertificateAuthorityID(), is(222222));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(1234));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(true));
        assertThat(packet2.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet2.getData(), is(data));
    }

    /**
     * Test for the constructors using different SCHA384 as MAC algorithm.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacketMAC2()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        String tlsString;
        byte[] data;
        Packet packet;
        Packet packet2;
        byte[] key;
        short leadingPackets;
        short trailingPackets;
        /*
         * Test a packet using SHA 384
         */
        tlsString = "TLS_PSK_WITH_NULL_SHA384";
        key = new KeyStringParser(tlsString).createKey();
        this.keyStore.addKey(key, tlsString);
        keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        leadingPackets = 0;
        trailingPackets = 100;

        // Create Packet
        data = TestHelper.randomData(300);
        packet = new Packet(222222, 1234, leadingPackets, trailingPackets, true, this.keyHash, data,
                this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(222222));
        assertThat(packet.getPacketNumber(), is(1234));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is(trailingPackets));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet.getGroupKeyHash(), is(this.keyHash));
        assertThat(packet.getData(), is(data));

        // Parse Packet
        packet2 = new Packet(packet.getRawPacket(), this.keyStore, this.sourceAddress);
        assertThat(packet2.getCertificateAuthorityID(), is(222222));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(1234));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(true));
        assertThat(packet2.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(true));
        assertThat(packet2.getData(), is(data));

    }

    /**
     * Test if MAC fails when the source address changes.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacketMACFail()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        byte[] data;
        Packet packet;
        Packet packet2;
        short leadingPackets;
        short trailingPackets;

        // Create Packet
        leadingPackets = 50;
        trailingPackets = 20;
        data = TestHelper.randomData(6000);
        packet = new Packet(444444, 34567, leadingPackets, trailingPackets, false, this.keyHash,
                data, this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(444444));
        assertThat(packet.getPacketNumber(), is(34567));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is(trailingPackets));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(false));
        assertThat(packet.getData(), is(data));

        // Parse Packet
        packet2 = new Packet(packet.getRawPacket(), this.keyStore,
                "Not the same address than before");
        assertThat(packet2.getCertificateAuthorityID(), is(444444));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(34567));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(true));
        assertThat(packet2.hasValidMac(), is(false));
        assertThat(packet.hasEncryptedPayload(), is(false));
        assertThat(packet2.getData(), is(data));

    }

    /**
     * Test if unknown keys are detected.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testPacketKeyFail()
            throws KeyNotInKeystoreException, UnkownProtocolVersionException {
        // Initialize variables
        byte[] data;
        Packet packet;
        Packet packet2;
        short leadingPackets;
        short trailingPackets;

        // Create Packet
        leadingPackets = 50;
        trailingPackets = 20;
        data = TestHelper.randomData(6000);
        packet = new Packet(444444, 34567, leadingPackets, trailingPackets, false, this.keyHash,
                data, this.keyStore, this.sourceAddress);
        assertThat(packet.getCertificateAuthorityID(), is(444444));
        assertThat(packet.getPacketNumber(), is(34567));
        assertThat(packet.getLeadingFragments(), is(leadingPackets));
        assertThat(packet.getTrailingFragments(), is(trailingPackets));
        assertThat(packet.usesKnownKey(), is(true));
        assertThat(packet.hasValidMac(), is(true));
        assertThat(packet.hasEncryptedPayload(), is(false));
        assertThat(packet.getData(), is(data));

        // Remove key to provoke false knownKey and a failing MAC
        this.keyStore.removeKey(this.keyStore.getKey(keyHash));

        // Parse Packet
        packet2 = new Packet(packet.getRawPacket(), this.keyStore, this.sourceAddress);
        assertThat(packet2.getCertificateAuthorityID(), is(444444));
        assertThat(packet2.getGroupKeyHash(), is(packet.getGroupKeyHash()));
        assertThat(packet2.getPacketNumber(), is(34567));
        assertThat(packet2.getLeadingFragments(), is(leadingPackets));
        assertThat(packet2.getTrailingFragments(), is(trailingPackets));
        assertThat(packet2.usesKnownKey(), is(false));
        assertThat(packet2.hasValidMac(), is(false));
        assertThat(packet.hasEncryptedPayload(), is(false));
        assertThat(packet2.getData(), is(data));
    }
}
