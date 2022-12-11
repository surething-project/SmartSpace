package org.ds2os.vsl.agentregistry.cryptography;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.UnkownAlgorithmException;
import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for storing symmetric keys. Also provides functions for generating hashes of keys and
 * calculating MACs.
 *
 * @author Johannes Straßer
 *
 */
public final class SymmetricKeyStore implements VslSymmetricKeyStore {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SymmetricKeyStore.class);

    /**
     * The SHA-256 hasher used by several functions.
     */
    private MessageDigest hasher = null;

    /**
     * The length of the hashes that are used internally to find keys.
     */
    private static final int HASH_LENGTH = 256 / 8;

    /**
     * Map that saves all used MAC functions. They are accessible by the String representation of
     * their key's hash. (use generateGroupkeyHash())
     */
    private final Map<String, Mac> macMap = new HashMap<String, Mac>();

    /**
     * Map that saves all used symmetric encryption keys. They are accessible by the String
     * representation of their hash. (use generateGroupkeyHash())
     */
    private final Map<String, byte[]> keyMap = new HashMap<String, byte[]>();

    /**
     * Map that saves all used TLS key Strings. They are accessible by the String representation of
     * their keys hash. (use generateGroupkeyHash())
     */
    private final Map<String, String> keyStringMap = new HashMap<String, String>();

    /**
     * HashMap that saves all shorter aliases of all key hashes. It allows for accessing the full
     * hash by the shortened ones.
     */
    private final HashMap<String, String> aliasMap = new HashMap<String, String>();

    /**
     * Simple constructor.
     */
    public SymmetricKeyStore() {
        initHasher();
    }

    /**
     * Initializes a new SHA-256 hasher.
     */
    private void initHasher() {
        try {
            this.hasher = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("This is not a standard compliant JVM.", e);
        }
    }

    @Override
    public byte[] generateKeyHash(final byte[] key, final int length) {
        final byte[] hash = hasher.digest(key);
        if (length > hash.length) {
            throw new InvalidParameterException(
                    "Cannot crop a " + hash.length + " byte key to " + length + " bytes.");
        }
        final byte[] croppedHash = Arrays.copyOf(hash, length);
        aliasMap.put(DatatypeConverter.printHexBinary(croppedHash),
                DatatypeConverter.printHexBinary(hash));
        hasher.reset();
        LOGGER.debug("Added hash alias: " + DatatypeConverter.printHexBinary(croppedHash));
        return croppedHash;
    }

    @Override
    public byte[] generateKeyHash(final byte[] key) {
        return generateKeyHash(key, HASH_LENGTH);
    }

    /**
     * Tries to add all keys found in the given agentRegistry to this class' storage.
     *
     * @param agentRegistry
     *            The agentRegistry to get keys from
     * @return A list of the hashes of all successfully retrieved and stored keys
     */
    public List<String> loadKeys(final VslAgentRegistryService agentRegistry) {
        final String[] keyHashes = agentRegistry.getAllMulticastGroupKeyHashes();
        final List<String> addedHashes = new ArrayList<String>();
        for (final String keyHash : keyHashes) {
            if (addKey(
                    DatatypeConverter.parseHexBinary(agentRegistry.getMulticastGroupKey(keyHash)),
                    agentRegistry.getMulticastGroupKeyString(keyHash))) {
                addedHashes.add(keyHash);
            }
        }
        return addedHashes;
    }

    @Override
    public boolean addKey(final byte[] key, final String keyString) {
        if (key == null) {
            return false;
        }

        // Parse keyString
        KeyStringParser parser = null;
        try {
            parser = new KeyStringParser(keyString);
        } catch (final InvalidParameterException e) {
            LOGGER.error("Could not parse keyString: {}", e.getMessage());
            return false;
        }

        // Check key length
        if (key.length * 8 < parser.getCipherKeyLength()
                || key.length * 8 < parser.getMACKeyLength()) {
            LOGGER.error("The given key ({} bits) was too short for the specified algorithm ",
                    key.length * 8);
            return false;
        }

        // Test if the MAC's length is compatible with the available protocol versions.
        try {
            KeyStringParser.macLengthToVersion(parser.getMACKeyLength() / 8);
        } catch (final UnkownProtocolVersionException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        // Create keyHash
        final String keyHash = DatatypeConverter.printHexBinary(generateKeyHash(key, HASH_LENGTH));
        if (keyMap.get(keyHash) == null) {
            // Create MAC object
            Mac mac;
            try {
                mac = Mac.getInstance("Hmac" + parser.getMacString());
            } catch (final NoSuchAlgorithmException e) {
                mac = null;
                LOGGER.error("Could not load MAC algorithm.", e);
                return false;
            }

            // Initialize MAC with the given key
            try {
                mac.init(new SecretKeySpec(Arrays.copyOfRange(key, 0, parser.getMACKeyLength()),
                        "Hmac" + parser.getMacString()));
            } catch (final InvalidKeyException e) {
                LOGGER.error(e.getMessage() + " Invalid Key.");
                return false;
            }
            // Test if a MAC can be computed
            final byte[] data = new byte[100];
            if (mac.doFinal(data) == null) {
                LOGGER.error("Invalid Key: Cannot be used by HMAC " + parser.getMacString());
                return false;
            }

            // Test encryption
            try {
                SymmetricCryptographyHandler.encrypt(data, key, keyString);
            } catch (final UnkownAlgorithmException e) {
                LOGGER.error(e.getMessage());
                return false;
            } catch (final InvalidKeyException e) {
                LOGGER.error(e.getMessage());
                return false;
            }

            // Add key and MAC to storage
            keyMap.put(keyHash, key);
            keyStringMap.put(keyHash, keyString);
            macMap.put(keyHash, mac);
            aliasMap.put(keyHash, keyHash);
            LOGGER.info("Added key with hash " + keyHash + " and TLS string " + keyString
                    + " to static SymmetricKeyStore.");

            /*
             * Add internally used hash to alias list. The hash is used by the class
             * org.ds2os.vsl.multicasttransport.fragmenation.packet. Its length is
             * Packet.GROUPKEYHASH_LENGTH.
             *
             * TODO rework aliases so that aliases of arbitrary length work.
             */
            generateKeyHash(key, 3);
            return true;
        } else {
            LOGGER.warn("KeyHash " + keyHash + "already exists.");
            return false;
        }

    }

    @Override
    public boolean removeKey(final byte[] key) {
        final String keyHash = DatatypeConverter.printHexBinary(generateKeyHash(key, HASH_LENGTH));
        if (keyMap.remove(keyHash) != null && macMap.remove(keyHash) != null) {
            for (String i = keyHash; i.length() > 1; i = i.substring(0, i.length() - 1)) {
                aliasMap.remove(i);
            }
            LOGGER.info("Removed key with hash " + keyHash + " from static SymmetricKeyStore.");
            return true;
        } else {
            return false;
        }

    }

    @Override
    public byte[] generateMAC(final byte[] keyHash, final byte[] data)
            throws KeyNotInKeystoreException {
        final Mac mac = macMap.get(aliasMap.get(DatatypeConverter.printHexBinary(keyHash)));

        if (mac != null) {
            synchronized (mac) {
                return mac.doFinal(data);
            }
        } else {
            throw new KeyNotInKeystoreException("Key with hash "
                    + DatatypeConverter.printHexBinary(keyHash) + " not found in keystore.");
        }

    }

    @Override
    public byte[] getKey(final byte[] keyHash) throws KeyNotInKeystoreException {
        final byte[] result = keyMap.get(aliasMap.get(DatatypeConverter.printHexBinary(keyHash)));
        if (result == null) {
            throw new KeyNotInKeystoreException("Key with hash "
                    + DatatypeConverter.printHexBinary(keyHash) + " not found in keystore.");
        } else {
            return result;
        }
    }

    @Override
    public String getKeyString(final byte[] keyHash) throws KeyNotInKeystoreException {
        final String result = keyStringMap
                .get(aliasMap.get(DatatypeConverter.printHexBinary(keyHash)));
        if (result == null) {
            throw new KeyNotInKeystoreException("Key with hash "
                    + DatatypeConverter.printHexBinary(keyHash) + " not found in keystore.");
        } else {
            return result;
        }
    }

}
