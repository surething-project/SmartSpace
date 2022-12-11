package org.ds2os.benchmark.multicasttransport;

import static org.ds2os.vsl.netutils.TestHelper.randomData;

import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.netutils.KeyStringParser;
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
 * Class to benchmark how fast the {@link SymmetricKeyStore} can create authentication tags.
 *
 * @author Johannes Straßer
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5)
@Warmup(iterations = 3)
@Fork(warmups = 1, value = 2)
@State(value = Scope.Thread)
public class AuthenticationBenchmark {

    /**
     * TLS string describing the used MAC algorithm.
     */
    @Param({ ("TLS_PSK_WITH_NULL_SHA"), ("TLS_PSK_WITH_NULL_SHA256"),
            ("TLS_PSK_WITH_NULL_SHA384") })
    private String tlsString;

    /**
     * The number of packets to create MACs for.
     */
    @Param({ ("1"), ("10"), ("100") })
    private int numberOfPackets;

    /**
     * Length of the data to be authenticated. 1472 is the biggest value that is expected.
     */
    private static final int DATA_LENGTH = 1472;

    /**
     * Keystore that is benchmarked.
     */
    private VslSymmetricKeyStore keystore;

    /**
     * Pre-generated data.
     */
    private byte[][] data;

    /**
     * Pre-generated key hashes that are used to access pre-generated keys.
     */
    private byte[] keyHash;

    /**
     * Fills the keystore with keys and generates test data.
     */
    @Setup(value = Level.Iteration)
    public final void setupEnvironment() {
        keystore = new SymmetricKeyStore();
        final byte[] key = new KeyStringParser(tlsString).createKey();
        keystore.addKey(key, tlsString);
        keyHash = keystore.generateKeyHash(key);
    }

    /**
     * Prepare the data for the test method.
     */
    @Setup(value = Level.Invocation)
    public final void setupData() {
        data = new byte[numberOfPackets][];
        for (@SuppressWarnings("unused")
        byte[] rawData : data) {
            rawData = randomData(DATA_LENGTH);
        }
    }

    /**
     * Generates an HMAC. the used data, key and algorithm are determined by the number of a TLS
     * string.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    @Benchmark
    public final void authenticationBenchmark() throws KeyNotInKeystoreException {
        for (final byte[] rawData : data) {
            keystore.generateMAC(keyHash, rawData);
        }

    }

}
