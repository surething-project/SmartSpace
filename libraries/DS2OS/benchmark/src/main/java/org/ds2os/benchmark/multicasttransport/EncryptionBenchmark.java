package org.ds2os.benchmark.multicasttransport;

import static org.ds2os.vsl.netutils.TestHelper.randomData;

import java.security.InvalidKeyException;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.agentregistry.cryptography.SymmetricCryptographyHandler;
import org.ds2os.vsl.exception.UnkownAlgorithmException;
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
 * Benchmark class for different encryptions used by the multicast-transport module.
 *
 * @author Johannes Straßer
 *
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5)
@Warmup(iterations = 3)
@Fork(warmups = 1, value = 2)
public class EncryptionBenchmark {
    /**
     * Length of one payload chunk. Changing this will linearly (approximately) influence test
     * results.
     */
    private static final int DATA_LENGTH = 1436;

    /**
     * Inner class for encapsulating stuff.
     *
     */
    @State(value = Scope.Thread)
    public static class MyState {

        /**
         * TLS strings describing the benchmarked algorithms.
         */
        @Param({ ("TLS_PSK_WITH_NULL_SHA"), ("TLS_PSK_WITH_AES_128_CBC_SHA256"),
                ("TLS_PSK_WITH_AES_128_GCM_SHA256"), ("TLS_PSK_WITH_AES_256_CBC_SHA384"),
                ("TLS_PSK_WITH_AES_256_GCM_SHA384"), ("TLS_PSK_WITH_ARIA_128_CBC_SHA256"),
                ("TLS_PSK_WITH_ARIA_128_GCM_SHA256"), ("TLS_PSK_WITH_ARIA_256_CBC_SHA384"),
                ("TLS_PSK_WITH_ARIA_256_GCM_SHA384"), ("TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                ("TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384") })
        private String tlsString;

        /**
         * The number of packets of DATA_LENGTH that will be encrypted.
         */
        @Param({ ("1"), ("10"), ("100") })
        private int numberOfPackets;

        /**
         * Pre-generated key.
         */
        private byte[] key;

        /**
         * Pre-generated data.
         */
        private byte[] data;

        /**
         * Register security provider.
         */
        @Setup(value = Level.Iteration)
        public final void setupProviders() {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }

        /**
         * Sets up key and data for the encryption tests.
         */
        @Setup(value = Level.Invocation)
        public final void setup() {
            key = new KeyStringParser(tlsString).createKey();
            data = randomData(DATA_LENGTH * numberOfPackets);
        }

        /**
         * Encrypts random data.
         *
         * @return Encrypted data
         *
         * @throws UnkownAlgorithmException
         *             Thrown if no provider for the given keyString can be loaded
         * @throws InvalidKeyException
         *             Thrown if the given key is invalid
         */
        public final byte[] encryptionBenchmark()
                throws InvalidKeyException, UnkownAlgorithmException {
            return SymmetricCryptographyHandler.encrypt(data, key, tlsString);
        }
    }

    /**
     * Method to test encryption.
     *
     * @param state
     *            inner class containing test data
     *
     * @return Encrypted data
     *
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the given keyString can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    @Benchmark
    public final byte[] encryptionBenchmark(final MyState state)
            throws InvalidKeyException, UnkownAlgorithmException {
        return state.encryptionBenchmark();
    }
}
