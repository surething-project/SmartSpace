package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.KeyNotInKeystoreException;

/**
 * Interface for storing symmetric keys. Keys consist of key data and a TLS String describing their
 * use. Key data not matching its use cannot be added. The keys are accessible by hashes which can
 * be generated via this interface. This interface also generates MACs from the stored keys.
 *
 * @author Johannes Straßer
 */
public interface VslSymmetricKeyStore {

    /**
     * Hashes the key using SHA-256 and crops it to the given length. Adds new aliases for the whole
     * hash to the aliasMap.
     *
     * @param key
     *            The key to be hashed
     * @param length
     *            The desired length of the hash value
     * @return Returns a hash value in a byte array of the given length
     */
    byte[] generateKeyHash(byte[] key, int length);

    /**
     * Calls {@link VslSymmetricKeyStore#generateKeyHash(byte[], int)} with the given key and this
     * class' internal hash length.
     *
     * @param key
     *            The key to be hashed
     * @return A hash which fits this classes internal hash length
     */
    byte[] generateKeyHash(byte[] key);

    /**
     * Adds a key to the keyStore. This method also checks if the key can be used to create MACs
     * and, encrypt and decrypt data using the algorithms implied by the keyString. The key will
     * only be added if the checks were successful.
     *
     * @param key
     *            The key to be added
     * @param keyString
     *            The TLS string describing the used algorithms
     * @return True if the key was successfully added, false if not
     */
    boolean addKey(byte[] key, String keyString);

    /**
     * Tries to remove the given key from the key storage. Also removes all stored aliases for this
     * key's hash.
     *
     * @param key
     *            The key to be removed
     * @return True if the given key was removed, false if the given key was unknown
     */
    boolean removeKey(byte[] key);

    /**
     * Returns the Key associated with the given keyHash. This works with shortened aliases. The
     * aliases must have been created by the {@link VslSymmetricKeyStore#generateKeyHash(byte[])}
     * methods.
     *
     * @param keyHash
     *            The hash of the respective key
     * @return The key belonging to the given hash
     * @throws KeyNotInKeystoreException
     *             Thrown if the key to the give keyHash is not in the keyStore
     */
    byte[] getKey(byte[] keyHash) throws KeyNotInKeystoreException;

    /**
     * Returns the TLS KeyString associated with the given keyHash. This works with shortened
     * aliases. The aliases must have been created by the
     * {@link VslSymmetricKeyStore#generateKeyHash(byte[])} methods.
     *
     * @param keyHash
     *            The hash of the respective key
     * @return The key belonging to the given hash
     * @throws KeyNotInKeystoreException
     *             Thrown if the key to the give keyHash is not in the keyStore
     */
    String getKeyString(byte[] keyHash) throws KeyNotInKeystoreException;

    /**
     * Generates a MAC over data using the key associated with the given keyHash. Uses HMAC with the
     * hash algorithm implied by this key's keyString (as given by
     * {@link VslSymmetricKeyStore#getKeyString(byte[])}).
     *
     * @param keyHash
     *            A hash of the key to be used
     * @param data
     *            Data to be authenticated
     * @return Returns a MAC tag over the given data
     * @throws KeyNotInKeystoreException
     *             Thrown if the key to the give keyHash is not in the keyStore
     */
    byte[] generateMAC(byte[] keyHash, byte[] data) throws KeyNotInKeystoreException;

}
