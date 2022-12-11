package org.ds2os.vsl.multicasttransport.fragmentation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.ds2os.vsl.netutils.TestHelper;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for Fragmenter.
 *
 * @author Johannes Straßer
 *
 */
public class FragmenterTest {

    /**
     * The Fragmenter that is created in setUp and used by various methods.
     */
    private Fragmenter fragmenter;

    /**
     * The VslSymmetricKeyStore for tests to use.
     */
    private VslSymmetricKeyStore keyStore;

    /**
     * Hash of a key stored in the keyStore.
     */
    private byte[] keyHash;

    /**
     * SourceAddress for tests to use.
     */
    private String sourceAddress;

    /**
     * Sets up a Fragmenter for other methods to use.
     */
    @Before
    public final void setUp() {
        this.keyStore = new SymmetricKeyStore();
        final byte[] key = new KeyStringParser("TLS_PSK_WITH_AES_128_CBC_SHA").createKey();
        this.keyStore.addKey(key, "TLS_PSK_WITH_AES_128_CBC_SHA");
        this.keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        this.sourceAddress = "udp://192.168.0.1:12345";
        this.fragmenter = new Fragmenter(1500, 222222, this.keyStore, this.sourceAddress);

    }

    /**
     * Tests the fragmentData method.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testFragmentData() throws UnkownProtocolVersionException {
        // Initialize variables
        byte[] data;
        List<byte[]> packets;
        Iterator<byte[]> iterator;
        ByteBuffer bb;
        short relNum;
        short num;
        byte[] data2;

        // Test fragmentation of 5000 bytes
        data = TestHelper.randomData(5000);
        packets = this.fragmenter.fragmentData(data, false, this.keyHash);
        iterator = packets.iterator();
        bb = ByteBuffer.allocate(5000);
        relNum = 0;
        num = (short) packets.size();
        while (iterator.hasNext()) {
            final Packet packet = new Packet(iterator.next(), this.keyStore, sourceAddress);
            bb.put(packet.getData());
            assertThat(packet.getLeadingFragments(), is(relNum));
            assertThat(packet.getTrailingFragments(), is((short) (num - relNum - 1)));
            relNum++;
            assertThat(packet.getCertificateAuthorityID(), is(222222));
            assertThat(packet.usesKnownKey(), is(true));
            assertThat(packet.hasValidMac(), is(true));
        }
        data2 = bb.array();
        assertThat(data, is(data2));

        // Test fragmentation of 5000 bytes again
        data = TestHelper.randomData(5000);
        packets = this.fragmenter.fragmentData(data, false, this.keyHash);
        iterator = packets.iterator();
        bb = ByteBuffer.allocate(5000);
        relNum = 0;
        num = (short) packets.size();
        while (iterator.hasNext()) {
            final Packet packet = new Packet(iterator.next(), this.keyStore, sourceAddress);
            bb.put(packet.getData());
            assertThat(packet.getLeadingFragments(), is(relNum));
            assertThat(packet.getTrailingFragments(), is((short) (num - relNum - 1)));
            relNum++;
            assertThat(packet.getCertificateAuthorityID(), is(222222));
            assertThat(packet.usesKnownKey(), is(true));
            assertThat(packet.hasValidMac(), is(true));
        }
        data2 = bb.array();
        assertThat(data, is(data2));
    }

}
