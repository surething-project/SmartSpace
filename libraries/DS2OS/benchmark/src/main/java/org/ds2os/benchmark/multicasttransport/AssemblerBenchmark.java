package org.ds2os.benchmark.multicasttransport;

import static org.ds2os.vsl.netutils.TestHelper.randomData;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.NumberOfSendersOverflowException;
import org.ds2os.vsl.multicasttransport.fragmentation.Assembler;
import org.ds2os.vsl.multicasttransport.fragmentation.AssemblerCallback;
import org.ds2os.vsl.multicasttransport.fragmentation.Fragmenter;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Class for benchmarking in order digestion of messages with multiple sizes from different senders.
 *
 * @author Johannes Straßer
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5)
@Warmup(iterations = 3)
@Fork(warmups = 1, value = 2)
@State(value = Scope.Thread)
public class AssemblerBenchmark {
    /**
     * MTU of the assumed underlying link (IPv4/UDP).
     */
    private static final int MTU = 1476;

    /**
     * TLS string specifying the used MAC algorithm.
     */
    private static final String TLSSTRING = "TLS_PSK_WITH_AES_128_CBC_SHA256";

    /**
     * Size of the internal header (header + MAC).
     */
    private static final int HEADER_SIZE = 16
            + new KeyStringParser(TLSSTRING).getMACKeyLength() / 8;

    /**
     * ID of the certificate authority.
     */
    private static final int CA_ID = 123456;

    /**
     * The number of senders.
     */
    @Param({ ("1"), ("10"), ("100") })
    private int numberOfSenders;

    /**
     * The number of packets.
     */
    @Param({ ("1"), ("10"), ("100") })
    private int numberOfPackets;

    /**
     * The benchmarked Assembler.
     */
    private Assembler assembler;

    /**
     * Array of Fragmenters for the tests.
     */
    private Fragmenter[] fragmenters;

    /**
     * Hash of the key that is used for authentication.
     */
    private byte[] keyHash;

    /**
     * Array of lists of packets prepared for the tests.
     */
    private List<byte[]>[] packets;

    /**
     * Sets up the KeyStore as well as the Assembler and the Fragmenters.
     */
    @SuppressWarnings("unchecked") // caused by the list 'packets'
    @Setup(value = Level.Iteration)
    public final void setupEnvironment() {
        final AssemblerCallback callback = Mockito.mock(AssemblerCallback.class);
        final VslSymmetricKeyStore keystore = new SymmetricKeyStore();
        final byte[] key = new KeyStringParser(TLSSTRING).createKey();
        keyHash = keystore.generateKeyHash(key);
        keystore.addKey(key, TLSSTRING);
        assembler = new Assembler(MTU * numberOfPackets, 5000, numberOfSenders, callback, keystore,
                100, CA_ID);
        fragmenters = new Fragmenter[numberOfSenders];
        for (int i = 0; i < numberOfSenders; i++) {
            fragmenters[i] = new Fragmenter(MTU, CA_ID, keystore, "udp://131.23.2." + i + ":3456");
        }
        packets = new List[numberOfSenders];

    }

    /**
     * Prepares the packets that will be digested.
     */
    @Setup(value = Level.Invocation)
    public final void setupPackets() {
        for (int i = 0; i < numberOfSenders; i++) {
            packets[i] = fragmenters[i].fragmentData(
                    randomData(numberOfPackets * (MTU - HEADER_SIZE)), false, keyHash);
        }
    }

    /**
     * Tries to digest all packets using one assembler.
     */
    @Benchmark
    public final void assemblerBenchmark() {
        for (int i = 0; i < numberOfSenders; i++) {
            for (final byte[] packet : packets[i]) {
                try {
                    assembler.digest(packet, "udp://131.23.2." + i + ":3456");
                } catch (final NumberOfSendersOverflowException e) {
                    // Cannot happen here
                    continue;
                }
            }
        }
    }

}
