package org.ds2os.vsl.multicasttransport.fragmentation;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.NumberOfSendersOverflowException;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.ds2os.vsl.netutils.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Class for testing the Assembler.
 *
 * @author Johannes Straßer
 *
 */
public class AssemblerTest {

    /**
     * A key hash to be use in the tests.
     */
    private byte[] keyHash;

    /**
     * The VslSymmetricKeyStore for tests to use.
     */
    private VslSymmetricKeyStore keyStore;

    /**
     * Method to set up the environment for the tests.
     */
    @Before
    public final void setUp() {
        final byte[] key = new KeyStringParser("TLS_PSK_WITH_AES_128_CBC_SHA").createKey();
        this.keyStore = new SymmetricKeyStore();
        this.keyStore.addKey(key, "TLS_PSK_WITH_AES_128_CBC_SHA");
        this.keyHash = this.keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
    }

    /**
     * Tests the digest method with packets from different senders to see if the Assembler respects
     * the given number of senders quota correctly.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest0() throws UnkownProtocolVersionException {
        AssemblerCallback callback;
        final int maxNum = 10;
        final int maxPayloadsize = 1500;
        final int certificateAuthorityID = 22222;
        callback = Mockito.mock(AssemblerCallback.class);
        Assembler ass;
        ass = new Assembler(20000, 10000, maxNum, callback, this.keyStore, 10000L,
                certificateAuthorityID);
        Fragmenter frag;
        final byte[] data = TestHelper.randomData(maxPayloadsize * 4);
        List<byte[]> packets;

        for (int i = 0; i < maxNum + 3; i++) {
            final String sourceAddress = "udp://192.168.0." + i + ":12345";
            frag = new Fragmenter(maxPayloadsize, certificateAuthorityID, this.keyStore,
                    sourceAddress);
            packets = frag.fragmentData(data, false, this.keyHash);
            try {
                ass.digest(new Packet(packets.get(0), this.keyStore, sourceAddress), sourceAddress);
                if (i >= maxNum) {
                    fail("Packet should not have been accepted");
                }
            } catch (final NumberOfSendersOverflowException e) {
                if (i < maxNum) {
                    fail("Packet should have been accepted");
                }
            }
        }
    }

    /**
     * Tests the digest method with packets from a different CA. These packets should not be
     * accepted.
     *
     * @throws NumberOfSendersOverflowException
     *             Should not be thrown
     */
    @Test
    public final void testDigest1() throws NumberOfSendersOverflowException {
        AssemblerCallback callback;
        final int maxNum = 10;
        final int maxPayloadsize = 1500;
        final int certificateAuthorityIDsender = 11111;
        final int certificateAuthorityIDrecv = 22222;
        callback = Mockito.mock(AssemblerCallback.class);
        Assembler ass;
        ass = new Assembler(20000, 10000, maxNum, callback, this.keyStore, 10000L,
                certificateAuthorityIDrecv);
        Fragmenter frag;
        final byte[] data = TestHelper.randomData(maxPayloadsize * 4);
        List<byte[]> packets;
        frag = new Fragmenter(maxPayloadsize, certificateAuthorityIDsender, this.keyStore,
                "udp://192.168.0.1:12345");
        packets = frag.fragmentData(data, false, this.keyHash);
        for (final byte[] packet : packets) {
            ass.digest(packet, "address");
        }
        verify(callback, never()).messageComplete(any(byte[].class), any(byte[].class),
                any(boolean.class));

    }

}
