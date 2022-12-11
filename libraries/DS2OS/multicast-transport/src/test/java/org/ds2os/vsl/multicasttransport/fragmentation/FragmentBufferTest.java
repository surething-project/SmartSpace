package org.ds2os.vsl.multicasttransport.fragmentation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.ds2os.vsl.netutils.TestHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Class for testing the FragmentBuffer.
 *
 * @author Johannes Straßer
 *
 */
public class FragmentBufferTest {

    /**
     * Testing the constructor, but mostly the insertPacket method. Testing includes inserting out
     * of range packets as well as reordering packets.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testFragmentBuffer() throws UnkownProtocolVersionException {
        // Initialize variables
        final String tlsString = "TLS_PSK_WITH_AES_128_CBC_SHA";
        final String sourceAddress = "udp://192.168.0.1:12345";
        FragmentBufferCallback callback;
        final ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<byte[]> hashCaptor = ArgumentCaptor.forClass(byte[].class);
        final ArgumentCaptor<Boolean> isEncryptedCaptor = ArgumentCaptor.forClass(Boolean.class);
        final byte[] key = new KeyStringParser(tlsString).createKey();
        final VslSymmetricKeyStore keyStore = new SymmetricKeyStore();
        keyStore.addKey(key, tlsString);
        final byte[] keyHash = keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        final Fragmenter frag = new Fragmenter(1000 + Packet.getHeaderLength(tlsString), 22222,
                keyStore, sourceAddress);
        FragmentBuffer fb;
        byte[] data;
        List<byte[]> packets;
        List<byte[]> packets2;
        Iterator<byte[]> iterator;
        Iterator<byte[]> iterator2;

        // Test 9.5 fragments in order
        // Packets from 0 to 9
        callback = Mockito.mock(FragmentBufferCallback.class);
        fb = new FragmentBuffer(10, 0, true, callback);
        data = TestHelper.randomData(9 * 1000 + 500);
        packets = frag.fragmentData(data, false, keyHash);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            fb.insertPacket(new Packet(iterator.next(), keyStore, sourceAddress));
        }
        verify(callback).fragmentComplete(dataCaptor.capture(), intCaptor.capture(),
                hashCaptor.capture(), isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(intCaptor.getValue(), is(0));
        assertThat(hashCaptor.getValue(), is(keyHash));
        assertThat(isEncryptedCaptor.getValue(), is(false));

        // Test 20 fragments in order while resending old ones
        // Packets from 10 to 29
        callback = Mockito.mock(SubAssembler.class);
        fb = new FragmentBuffer(20, 10, true, callback);
        data = TestHelper.randomData(20 * 1000);
        packets2 = packets;
        Collections.shuffle(packets2);
        iterator2 = packets2.iterator();
        packets = frag.fragmentData(data, false, keyHash);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            fb.insertPacket(new Packet(iterator.next(), keyStore, sourceAddress));
            if (iterator2.hasNext()) {
                fb.insertPacket(new Packet(iterator2.next(), keyStore, sourceAddress));
            }
        }
        verify(callback).fragmentComplete(dataCaptor.capture(), intCaptor.capture(),
                hashCaptor.capture(), isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(intCaptor.getValue(), is(10));
        assertThat(hashCaptor.getValue(), is(keyHash));
        assertThat(isEncryptedCaptor.getValue(), is(false));

        // Test 20 fragments out of order
        // The payload is supposedly encrypted
        // Packets from 30 to 49
        callback = Mockito.mock(SubAssembler.class);
        fb = new FragmentBuffer(20, 30, true, callback);
        data = TestHelper.randomData(20 * 1000);
        packets = frag.fragmentData(data, true, keyHash);
        Collections.shuffle(packets);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            fb.insertPacket(new Packet(iterator.next(), keyStore, sourceAddress));
        }
        verify(callback).fragmentComplete(dataCaptor.capture(), intCaptor.capture(),
                hashCaptor.capture(), isEncryptedCaptor.capture());
        assertThat(dataCaptor.getValue(), is(data));
        assertThat(intCaptor.getValue(), is(30));
        assertThat(hashCaptor.getValue(),
                is(new Packet(packets.get(0), keyStore, sourceAddress).getGroupKeyHash()));
        assertThat(isEncryptedCaptor.getValue(), is(true));

    }

    /**
     * Test if authenticated buffers correctly ignore unauthenticated packets.
     *
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testFragmentBuffer1() throws UnkownProtocolVersionException {
        // Initialize variables
        final String tlsString = "TLS_PSK_WITH_AES_128_CBC_SHA";
        final String sourceAddress = "udp://192.168.0.1:12345";
        FragmentBufferCallback callback;
        final byte[] key = new KeyStringParser(tlsString).createKey();
        final VslSymmetricKeyStore keyStore = new SymmetricKeyStore();
        keyStore.addKey(key, tlsString);
        final byte[] keyHash = keyStore.generateKeyHash(key, Packet.GROUPKEYHASH_LENGTH);
        final Fragmenter frag = new Fragmenter(1000 + Packet.getHeaderLength(tlsString), 22222,
                keyStore, sourceAddress);
        FragmentBuffer fb;
        byte[] data;
        List<byte[]> packets;
        Iterator<byte[]> iterator;

        // Test 9.5 unauthenticated fragments fragments in authenticated buffer
        // Packets from 0 to 9
        callback = Mockito.mock(FragmentBufferCallback.class);
        fb = new FragmentBuffer(10, 0, true, callback);
        data = TestHelper.randomData(9 * 1000 + 500);
        packets = frag.fragmentData(data, false, keyHash);
        keyStore.removeKey(key);
        iterator = packets.iterator();
        while (iterator.hasNext()) {
            fb.insertPacket(new Packet(iterator.next(), keyStore, sourceAddress));
        }
        verify(callback, never()).fragmentComplete(any(byte[].class), any(Integer.class),
                any(byte[].class), any(Boolean.class));
    }

}
