package org.ds2os.vsl.multicasttransport.fragmentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.ds2os.vsl.netutils.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Class for testing the SubAssemblers.
 *
 * @author Johannes Straßer
 *
 */
public class SubAssemblerTest {

    /**
     * The VslSymmetricKeyStore for tests to use.
     */
    private VslSymmetricKeyStore keyStore;

    /**
     * The MTU used in all tests.
     */
    private final int mtu = 1500;

    /**
     * A SecretKey to be use in the tests.
     */
    private byte[] key;

    /**
     * The hash of the provided key.
     */
    private byte[] keyHash;

    /**
     * The TLS keyString of the provided key.
     */
    private static final String TLS_STRING = "TLS_PSK_WITH_AES_128_CBC_SHA";

    /**
     * Source address String for tests.
     */
    private final String sourceAddress = "udp://192.168.0.1:12345";

    /**
     * Cutoff Interval for the subAssembler in this test.
     */
    private final long cutoffInterval = 1000;

    /**
     * Method to set up the environment for the tests.
     */
    @Before
    public final void setUp() {
        this.keyStore = new SymmetricKeyStore();
        this.key = new KeyStringParser(TLS_STRING).createKey();
        keyStore.addKey(this.key, TLS_STRING);
        this.keyHash = keyStore.generateKeyHash(this.key, Packet.GROUPKEYHASH_LENGTH);
    }

    /**
     * Test for the digest method using unauthorized, non interlaced, complete messages.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest0() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(20000, 11000, callback, cutoffInterval);
        ArgumentCaptor<byte[]> dataCaptor;
        ArgumentCaptor<byte[]> hashCaptor;
        final ArgumentCaptor<Boolean> isEncryptedCaptor = ArgumentCaptor.forClass(Boolean.class);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        byte[] data;
        List<byte[]> packets;
        Iterator<byte[]> iterator;

        // Test unauthorized buffer with one fitting in order message
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, false, this.keyHash);
        iterator = packets.iterator();
        this.keyStore.removeKey(key);
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        assertThat(subAss.knownSender(), is(false));
        verify(callback).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(nullValue()));
        assertThat(isEncryptedCaptor.getValue(), is(false));

        // Test unauthorized buffer with one fitting in order message again, to check if the space
        // is freed correctly
        this.keyStore.addKey(key, TLS_STRING);
        this.keyHash = keyStore.generateKeyHash(this.key, Packet.GROUPKEYHASH_LENGTH);
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, false, this.keyHash);
        iterator = packets.iterator();
        this.keyStore.removeKey(key);
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        assertThat(subAss.knownSender(), is(false));
        verify(callback, times(2)).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(nullValue()));
        assertThat(isEncryptedCaptor.getValue(), is(false));
    }

    /**
     * Test for the digest method using authorized, non interlaced, complete messages.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest1() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(20000, 10000, callback, cutoffInterval);
        ArgumentCaptor<byte[]> dataCaptor;
        ArgumentCaptor<byte[]> hashCaptor;
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        byte[] data;
        List<byte[]> packets;
        Iterator<byte[]> iterator;

        // Test authorized buffer with one fitting in order message
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Boolean> isEncryptedCaptor = ArgumentCaptor.forClass(Boolean.class);
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, true, this.keyHash);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        assertThat(subAss.knownSender(), is(true));
        verify(callback).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(
                new Packet(packets.get(0), this.keyStore, this.sourceAddress).getGroupKeyHash()));
        assertThat(isEncryptedCaptor.getValue(), is(true));

        // Test authorized buffer with one fitting in order message again, to check if the space
        // is freed correctly
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, true, this.keyHash);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        assertThat(subAss.knownSender(), is(true));
        verify(callback, times(2)).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(
                new Packet(packets.get(0), this.keyStore, this.sourceAddress).getGroupKeyHash()));
        assertThat(isEncryptedCaptor.getValue(), is(true));

    }

    /**
     * Test for the digest method using non fragmented messages.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest3() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(20000, 10000, callback, cutoffInterval);
        ArgumentCaptor<byte[]> dataCaptor;
        ArgumentCaptor<byte[]> hashCaptor;
        final ArgumentCaptor<Boolean> isEncryptedCaptor = ArgumentCaptor.forClass(Boolean.class);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        byte[] data;
        List<byte[]> packets;

        // Test unauthorized message
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        data = TestHelper.randomData(this.mtu - 100);
        packets = frag.fragmentData(data, false, this.keyHash);
        this.keyStore.removeKey(key);
        subAss.digest(new Packet(packets.get(0), this.keyStore, this.sourceAddress));
        assertThat(subAss.knownSender(), is(false));
        verify(callback).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(nullValue()));
        assertThat(isEncryptedCaptor.getValue(), is(false));

        // Test authorized message
        this.keyStore.addKey(key, TLS_STRING);
        this.keyHash = keyStore.generateKeyHash(this.key, Packet.GROUPKEYHASH_LENGTH);
        dataCaptor = ArgumentCaptor.forClass(byte[].class);
        hashCaptor = ArgumentCaptor.forClass(byte[].class);
        data = TestHelper.randomData(this.mtu - 100);
        packets = frag.fragmentData(data, true, this.keyHash);
        subAss.digest(new Packet(packets.get(0), this.keyStore, this.sourceAddress));
        verify(callback, times(2)).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());
        assertThat(subAss.knownSender(), is(true));
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(hashCaptor.getValue(), is(
                new Packet(packets.get(0), this.keyStore, this.sourceAddress).getGroupKeyHash()));
        assertThat(isEncryptedCaptor.getValue(), is(true));

    }

    /**
     * Test for the digest method using authorized, interlaced, complete messages that do not exceed
     * the buffer size.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest4() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(150000, 10000, callback, cutoffInterval);
        final ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<byte[]> hashCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Boolean> isEncryptedCaptor = ArgumentCaptor.forClass(Boolean.class);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        final List<byte[]> data = new ArrayList<byte[]>();
        List<byte[]> packets;
        final List<byte[]> allPackets = new ArrayList<byte[]>();
        for (int i = 0; i < 10; i++) {
            data.add(TestHelper.randomData(10000));
            packets = frag.fragmentData(data.get(i), false, this.keyHash);
            // Make sure all buffers are opened up
            subAss.digest(new Packet(packets.remove((int) (Math.random() * packets.size())),
                    this.keyStore, this.sourceAddress));
            allPackets.addAll(packets);
        }

        // Randomly send packets
        Collections.shuffle(allPackets);
        final Iterator<byte[]> iterator = allPackets.iterator();
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }

        verify(callback, times(10)).fragmentComplete(dataCaptor.capture(), hashCaptor.capture(),
                isEncryptedCaptor.capture());

    }

    /**
     * Test for the digest method using authorized, interlaced, complete messages that do exceed the
     * buffer size.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest5() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(20000, 10000, callback, cutoffInterval);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        final List<byte[]> data = new ArrayList<byte[]>();
        final int numMessages = 30;
        List<byte[]> packets;
        final List<byte[]> allPackets = new ArrayList<byte[]>();
        for (int i = 0; i < numMessages; i++) {
            data.add(TestHelper.randomData(25000));
            packets = frag.fragmentData(data.get(i), false, this.keyHash);
            allPackets.addAll(packets);
        }

        // Randomly send packets
        Collections.shuffle(allPackets);
        final Iterator<byte[]> iterator = allPackets.iterator();
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        verify(callback, never()).fragmentComplete(any(byte[].class), any(byte[].class),
                any(Boolean.class));

    }

    /**
     * Test for the digest method using unauthorized, interlaced, complete messages that do exceed
     * the buffer size.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testDigest6() throws UnkownProtocolVersionException {
        // Initialize variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(150000, 9000, callback, cutoffInterval);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        final List<byte[]> data = new ArrayList<byte[]>();
        List<byte[]> packets;
        final List<byte[]> allPackets = new ArrayList<byte[]>();
        for (int i = 0; i < 30; i++) {
            data.add(TestHelper.randomData(10000));
            packets = frag.fragmentData(data.get(i), false, this.keyHash);
            allPackets.addAll(packets);
        }

        // Remove key
        this.keyStore.removeKey(key);

        // Randomly send packets
        Collections.shuffle(allPackets);
        final Iterator<byte[]> iterator = allPackets.iterator();
        while (iterator.hasNext()) {
            subAss.digest(new Packet(iterator.next(), this.keyStore, this.sourceAddress));
        }
        verify(callback, never()).fragmentComplete(any(byte[].class), any(byte[].class),
                any(Boolean.class));

    }

    /**
     * Test for the knownSender method.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testKnownSender() throws UnkownProtocolVersionException {
        // Initialize Variables
        SubAssemblerCallback callback;
        callback = Mockito.mock(SubAssemblerCallback.class);
        final SubAssembler subAss = new SubAssembler(20000, 11000, callback, this.cutoffInterval);
        final Fragmenter frag = new Fragmenter(this.mtu, 22222, this.keyStore, this.sourceAddress);
        byte[] data;
        List<byte[]> packets;
        // Send an unauthorized packet first then an authorized one
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, false, this.keyHash);
        assertThat(subAss.knownSender(), is(false));
        this.keyStore.removeKey(key);
        subAss.digest(new Packet(packets.get(0), this.keyStore, this.sourceAddress));
        assertThat(subAss.knownSender(), is(false));
        this.keyStore.addKey(key, TLS_STRING);
        this.keyHash = keyStore.generateKeyHash(this.key, Packet.GROUPKEYHASH_LENGTH);
        final Packet packet = new Packet(packets.get(1), this.keyStore, this.sourceAddress);
        assertThat(packet.usesKnownKey(), is(true));
        subAss.digest(packet);
        assertThat(subAss.knownSender(), is(true));

        // Send an authorized packet first then an unauthorized one
        data = TestHelper.randomData(9500);
        packets = frag.fragmentData(data, false, this.keyHash);
        subAss.digest(new Packet(packets.get(0), this.keyStore, this.sourceAddress));
        assertThat(subAss.knownSender(), is(true));
        this.keyStore.removeKey(key);
        subAss.digest(new Packet(packets.get(1), this.keyStore, this.sourceAddress));
        assertThat(subAss.knownSender(), is(true));
    }

}
