package org.ds2os.vsl.agentRegistry.cryptography;

import static org.ds2os.vsl.netutils.TestHelper.randomData;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.security.InvalidKeyException;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.agentregistry.cryptography.SymmetricCryptographyHandler;
import org.ds2os.vsl.agentregistry.cryptography.SymmetricKeyStore;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.UnkownAlgorithmException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for SymmetricCryptographyHandler. Note that these tests do not test the cryptographic
 * strength of the encryption.
 *
 * @author Johannes Straßer
 *
 */
public class SymmetricCryptographyHandlerTest {

    /**
     * Hash of a key to be used by tests.
     */
    private byte[] keyHash;

    /**
     * The VslSymmetricKeyStore for tests to use.
     */
    private VslSymmetricKeyStore keyStore;

    /**
     * Add Bouncy Castle security provider in case JVM does not have all ciphers.
     */
    @BeforeClass
    public static final void loadBcprov() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Adds a key to the SymmetricKeyStore and saves the hash.
     */
    @Before
    public final void setUp() {
        this.keyStore = new SymmetricKeyStore();
        final String tlsString = "TLS_PSK_WITH_AES_256_GCM_SHA384";
        final byte[] key = new KeyStringParser(tlsString).createKey();
        this.keyStore.addKey(key, tlsString);
        keyHash = this.keyStore.generateKeyHash(key, 2);
    }

    /**
     * Test encryption and decryption with the use of the SymmetricKeyStore for key storage.
     *
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    @Test
    public final void testCipher0() throws KeyNotInKeystoreException {
        final byte[] data = randomData(3000);
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, keyHash, this.keyStore), keyHash,
                this.keyStore), data), is(true));
    }

    /**
     * Test encryption and decryption with 256 bit keys without the use of the SymmetricKeyStore for
     * key storage.
     *
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the given keyString can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    @Test
    public final void testCipher1() throws InvalidKeyException, UnkownAlgorithmException {
        final byte[] key = new KeyStringParser("TLS_PSK_WITH_AES_256_GCM_SHA384").createKey();
        final byte[] data = randomData(3000);
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, key, "TLS_PSK_WITH_AES_256_GCM_SHA384"),
                key, "TLS_PSK_WITH_AES_256_GCM_SHA384"), data), is(true));
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, key,
                        "TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384"),
                key, "TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384"), data), is(true));
    }

    /**
     * Test encryption and decryption with 128 bit keys without the use of the SymmetricKeyStore for
     * key storage.
     *
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the given keyString can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    @Test
    public final void testCipher2() throws InvalidKeyException, UnkownAlgorithmException {
        final byte[] key = new KeyStringParser("TLS_PSK_WITH_AES_128_GCM_SHA256").createKey();
        final byte[] data = randomData(3000);
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, key, "TLS_PSK_WITH_AES_128_GCM_SHA256"),
                key, "TLS_PSK_WITH_AES_128_GCM_SHA256"), data), is(true));
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, key,
                        "TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                key, "TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256"), data), is(true));
    }

    /**
     * Test encryption and decryption with null ciphers without the use of the SymmetricKeyStore for
     * key storage.
     *
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the given keyString can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    @Test
    public final void testCipher3() throws InvalidKeyException, UnkownAlgorithmException {
        final byte[] data = randomData(3000);
        // Use a NULL key
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, null, "TLS_PSK_WITH_NULL_SHA"), null,
                "TLS_PSK_WITH_NULL_SHA"), data), is(true));
        // Use a key that was created for MACing
        final byte[] key = new KeyStringParser("TLS_PSK_WITH_NULL_SHA384").createKey();
        assertThat(Arrays.equals(SymmetricCryptographyHandler.decrypt(
                SymmetricCryptographyHandler.encrypt(data, key, "TLS_PSK_WITH_NULL_SHA384"), key,
                "TLS_PSK_WITH_NULL_SHA384"), data), is(true));
    }

    /**
     * Test the createKeys method.
     */
    @Test
    public final void testCreateKey() {
        byte[] key;
        key = new KeyStringParser("TLS_PSK_WITH_NULL_SHA").createKey();
        assertThat(key.length, is(160 / 8));
        key = new KeyStringParser("TLS_PSK_WITH_NULL_SHA384").createKey();
        assertThat(key.length, is(384 / 8));
        key = new KeyStringParser("TLS_PSK_WITH_AES_128_GCM_SHA256").createKey();
        assertThat(key.length, is(256 / 8));
        key = new KeyStringParser("TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256").createKey();
        assertThat(key.length, is(256 / 8));
        key = new KeyStringParser("TLS_PSK_WITH_AES_256_GCM_SHA384").createKey();
        assertThat(key.length, is(384 / 8));
        key = new KeyStringParser("TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384").createKey();
        assertThat(key.length, is(384 / 8));
    }

}
