package org.ds2os.vsl.agentregistry.cryptography;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.UnkownAlgorithmException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static class for symmetric encryption and decyrption. Uses SymmetricKeyStore as key storage, but
 * can be used without.
 *
 * @author Johannes Straßer
 *
 */
public final class SymmetricCryptographyHandler {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SymmetricCryptographyHandler.class);

    /**
     * Secure random generator used to create unique IVs.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Dummy constructor to hide default one.
     */
    private SymmetricCryptographyHandler() {
        // Dummy
    }

    /**
     * Encrypts data using the algorithm specified in the TLS string. It uses the given hash to get
     * a key from the SymmetricKeyStore.
     *
     * @param data
     *            The data to encrypt
     * @param keyHash
     *            The hash of the encryption key
     * @param keyStore
     *            The key store to be used
     * @return The encrypted data, preceded by the IV
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    public static byte[] encrypt(final byte[] data, final byte[] keyHash,
            final VslSymmetricKeyStore keyStore) throws KeyNotInKeystoreException {
        final byte[] key = keyStore.getKey(keyHash);
        final String keyString = keyStore.getKeyString(keyHash);
        if (key != null) {
            try {
                return encrypt(data, key, keyString);
            } catch (final UnkownAlgorithmException e) {
                LOGGER.error("Key with unsupported TLS string in keystore.");
                throw new KeyNotInKeystoreException(e.getMessage());
            } catch (final InvalidKeyException e) {
                LOGGER.error("Key from keystore is invalid");
                throw new KeyNotInKeystoreException(e.getMessage());
            }
        } else {
            return null;
        }
    }

    /**
     * Encrypts data using the algorithm specified in the keyString. Keys longer than required will
     * be cropped.
     *
     * @param data
     *            The data to encrypt
     * @param key
     *            The encryption key
     * @param keyString
     *            The TLS key string describing the used algorithms
     * @return The encrypted data, preceded by the used IV
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the implied algorithm can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    public static byte[] encrypt(final byte[] data, final byte[] key, final String keyString)
            throws UnkownAlgorithmException, InvalidKeyException {
        // Parse keyString
        KeyStringParser parser = null;
        try {
            parser = new KeyStringParser(keyString);
        } catch (final InvalidParameterException e) {
            throw new InvalidParameterException("Could not parse keyString: " + e.getMessage());
        }

        // Handle null ciphers
        if (parser.getCipherString().equals("NULL")) {
            LOGGER.warn("Null cipher is being used!");
            return data;
        }

        // Create cipher object and crop key if necessary
        Cipher encrypt = null;
        Key keyObject = null;
        try {
            encrypt = Cipher.getInstance(parser.getCipherString() + "/" + parser.getModeString()
                    + "/" + parser.getPaddingString());
            keyObject = new SecretKeySpec(
                    Arrays.copyOfRange(key, 0, parser.getCipherKeyLength() / 8),
                    parser.getCipherString());
        } catch (final NoSuchAlgorithmException e) {
            throw new UnkownAlgorithmException("Could not get needed cipher: " + e.getMessage());
        } catch (final NoSuchPaddingException e) {
            throw new UnkownAlgorithmException(
                    "Could not get needed cipher with padding:" + e.getMessage());
        }

        // Initialize cipher with cropped key and a fresh IV
        IvParameterSpec parameters = null;
        try {
            final byte[] iv = new byte[encrypt.getBlockSize()];
            RANDOM.nextBytes(iv);
            parameters = new IvParameterSpec(iv);
            encrypt.init(Cipher.ENCRYPT_MODE, keyObject, parameters);
        } catch (final InvalidAlgorithmParameterException e) {
            throw new UnkownAlgorithmException(
                    "Could not init the cipher with a generated IV:" + e.getMessage());
        }

        // Write IV to result
        final ByteBuffer result = ByteBuffer
                .allocate(encrypt.getBlockSize() + encrypt.getOutputSize(data.length));
        result.put(encrypt.getIV());

        // Encrypt data and write to result
        try {
            LOGGER.debug("Encrypting... IV: " + DatatypeConverter.printHexBinary(encrypt.getIV()));
            result.put(encrypt.doFinal(data));
            return result.array();
        } catch (final IllegalBlockSizeException e) {
            throw new UnkownAlgorithmException("Problems regarding block size: " + e.getMessage());
        } catch (final BadPaddingException e) {
            throw new UnkownAlgorithmException("Found bad padding: " + e.getMessage());
        }
    }

    /**
     * Decrypts data using the algorithm specified in the TLS string. The data has to be preceded by
     * a suitable IV. It uses the given hash to get a key from the SymmetricKeyStore.
     *
     * @param data
     *            The data to decrypt preceded by the IV
     * @param keyHash
     *            The hash of the decryption key
     * @param keyStore
     *            The key store to be used
     * @return The decrypted data
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keyStore
     */
    public static byte[] decrypt(final byte[] data, final byte[] keyHash,
            final VslSymmetricKeyStore keyStore) throws KeyNotInKeystoreException {
        final byte[] key = keyStore.getKey(keyHash);
        final String keyString = keyStore.getKeyString(keyHash);
        if (key != null) {
            try {
                return decrypt(data, key, keyString);
            } catch (final UnkownAlgorithmException e) {
                LOGGER.error("Key with unsupported TLS string in keystore.");
                throw new KeyNotInKeystoreException(e.getMessage());
            } catch (final InvalidKeyException e) {
                LOGGER.error("Key from keystore is invalid");
                throw new KeyNotInKeystoreException(e.getMessage());
            }
        } else {
            return null;
        }
    }

    /**
     * Decrypts data using the algorithm specified in the TLS string. The data has to be preceded by
     * a suitable IV.
     *
     * @param data
     *            The data to decrypt preceded by the IV
     * @param key
     *            The decryption key
     * @param keyString
     *            The TLS key string describing the used algorithms
     * @return The decrypted data
     * @throws UnkownAlgorithmException
     *             Thrown if no provider for the given keyString can be loaded
     * @throws InvalidKeyException
     *             Thrown if the given key is invalid
     */
    public static byte[] decrypt(final byte[] data, final byte[] key, final String keyString)
            throws UnkownAlgorithmException, InvalidKeyException {
        // Parse keyString
        KeyStringParser parser = null;
        try {
            parser = new KeyStringParser(keyString);
        } catch (final InvalidParameterException e) {
            LOGGER.error("Could not parse keyString: {}", e.getMessage());
            return null;
        }

        // Handle null ciphers
        if (parser.getCipherString().equals("NULL")) {
            LOGGER.warn("Null cipher is being used!");
            return data;
        }

        // Create cipher object and crop key if necessary
        Cipher decrypt = null;
        Key keyObject = null;
        try {
            decrypt = Cipher.getInstance(parser.getCipherString() + "/" + parser.getModeString()
                    + "/" + parser.getPaddingString());
            keyObject = new SecretKeySpec(
                    Arrays.copyOfRange(key, 0, parser.getCipherKeyLength() / 8),
                    parser.getCipherString());
        } catch (final NoSuchAlgorithmException e) {
            throw new UnkownAlgorithmException("Could not get needed cipher: " + e.getMessage());
        } catch (final NoSuchPaddingException e) {
            throw new UnkownAlgorithmException(
                    "Could not get needed cipher with padding:" + e.getMessage());
        }

        // Initialize cipher with the cropped key and the IV from the data
        byte[] parameters = null;
        try {
            parameters = Arrays.copyOfRange(data, 0, decrypt.getBlockSize());
            decrypt.init(Cipher.DECRYPT_MODE, keyObject, new IvParameterSpec(parameters));
        } catch (final InvalidAlgorithmParameterException e) {
            throw new UnkownAlgorithmException(
                    "Could not init the cipher with the implicitly given IV:" + e.getMessage());
        }

        // Decrypt data
        try {
            LOGGER.debug("Decrypting... IV: " + DatatypeConverter.printHexBinary(decrypt.getIV()));
            return decrypt.doFinal(Arrays.copyOfRange(data, decrypt.getBlockSize(), data.length));
        } catch (final IllegalBlockSizeException e) {
            throw new UnkownAlgorithmException("Problems regarding block size: " + e.getMessage());
        } catch (final BadPaddingException e) {
            throw new UnkownAlgorithmException("Found bad padding: " + e.getMessage());
        }
    }
}
