package org.ds2os.vsl.netutils;

import java.security.InvalidParameterException;
import java.security.SecureRandom;

import org.ds2os.vsl.exception.UnkownProtocolVersionException;

/**
 * Parses TLS strings beginning with 'TLS_PSK_WITH_'. Excludes ciphers beginning with 'RC' (mainly
 * because of RC4), the cipher '3DES', and all strings using 'CCM' mode.
 *
 * @author Johannes Straßer
 *
 */
public class KeyStringParser {

    /**
     * Version number for packets using 160 bit MAC values.
     */
    private static final byte VERSION_160 = 1;

    /**
     * Version number for packets using 256 bit MAC values.
     */
    private static final byte VERSION_256 = 2;

    /**
     * Version number for packets using 384 bit MAC values.
     */
    private static final byte VERSION_384 = 3;

    /**
     * The full TLS string.
     */
    private final String keyString;

    /**
     * The used cipher.
     */
    private final String cipherString;

    /**
     * The mode the cipher is used in.
     */
    private final String modeString;

    /**
     * The used padding for the cipher.
     */
    private final String paddingString;

    /**
     * The key length used by the cipher in bits.
     */
    private final int cipherKeyLength;

    /**
     * The key length used by the MAC in bits.
     */
    private final int macKeyLength;

    /**
     * The used MAC scheme.
     */
    private final String macString;

    /**
     * Simple constructor.
     *
     * @param keyString
     *            The TLS string to be parsed
     */
    public KeyStringParser(final String keyString) {
        this.keyString = keyString;
        int pos = 0;
        final String[] elements = keyString.split("_");
        // Crop introduction string
        if (!elements[pos++].equals("TLS") || !elements[pos++].equals("PSK")
                || !elements[pos++].equals("WITH")) {
            throw new InvalidParameterException("Strings must start with 'TLS_PSK_WITH_'");
        }

        // Get cipher
        cipherString = elements[pos++];

        // Get cipher attributes
        if (cipherString.equals("NULL")) {
            cipherKeyLength = 0;
            modeString = null;
            paddingString = null;
        } else if (cipherString.startsWith("RC")) {
            throw new InvalidParameterException("Stream ciphers are not supported.");
        } else if (cipherString.equals("3DES")) {
            throw new InvalidParameterException("3DES is not supported.");
        } else {
            cipherKeyLength = Integer.parseInt(elements[pos++]);
            modeString = elements[pos++];
            paddingString = getPaddingforMode(modeString);
            if (modeString.equals("CCM")) {
                throw new InvalidParameterException("CCM is not supported.");
            }
        }

        // Get MAC
        if (elements[pos].equals(("SHA"))) {
            macString = "SHA1";
            macKeyLength = 160;
        } else {
            macString = elements[pos];
            if (macString.startsWith("SHA")) {
                macKeyLength = Integer.parseInt(macString.substring(3, macString.length()));
            } else {
                // fallback length
                macKeyLength = 512;
            }
        }

    }

    /**
     * Decides a suiting padding for a given mode.
     *
     * @param mode
     *            The mode in question
     * @return the padding schemes name as a String
     */
    private String getPaddingforMode(final String mode) {
        String result = null;
        if (mode.equals("CBC") || mode.equals("ECB")) {
            result = "PKCS5Padding";
        } else {
            result = "NOPADDING";
        }
        return result;
    }

    /**
     * @return the keyString
     */
    public final String getKeyString() {
        return keyString;
    }

    /**
     * @return the cipherString
     */
    public final String getCipherString() {
        return cipherString;
    }

    /**
     * @return the modeString
     */
    public final String getModeString() {
        return modeString;
    }

    /**
     * @return the paddingString
     */
    public final String getPaddingString() {
        return paddingString;
    }

    /**
     * @return the keyLength of the used cipher in bits
     */
    public final int getCipherKeyLength() {
        return cipherKeyLength;
    }

    /**
     * @return the keyLength of the used MAC in bits
     */
    public final int getMACKeyLength() {
        return macKeyLength;
    }

    /**
     * @return the macString
     */
    public final String getMacString() {
        return macString;
    }

    /**
     * Create a new key for the algorithm this object was initialized with. Uses SecureRandom.
     *
     * @return A new random key
     */
    public final byte[] createKey() {
        final SecureRandom rand = new SecureRandom();
        final byte[] key = new byte[(this.cipherKeyLength > this.macKeyLength ? this.cipherKeyLength
                : this.macKeyLength) / 8];
        rand.nextBytes(key);
        return key;
    }

    /**
     * Get a version number for a MAC of length length.
     *
     * @param length
     *            The Length of the MAC in bytes
     * @return The version number
     * @throws UnkownProtocolVersionException
     *             Thrown if the given MAC length does not match to a known protocol number
     */
    public static byte macLengthToVersion(final int length) throws UnkownProtocolVersionException {
        byte version;
        switch (length * 8) {
        case 160:
            version = VERSION_160;
            break;
        case 256:
            version = VERSION_256;
            break;
        case 384:
            version = VERSION_384;
            break;
        default:
            throw new UnkownProtocolVersionException(
                    "There is no protocol version allowing a MAC length of " + length + " bytes.");
        }
        return version;
    }

    /**
     * Get the MAC length for a given protocol version.
     *
     * @param version
     *            The protocol version
     * @return The MAC length of the given protocol version in bytes
     * @throws UnkownProtocolVersionException
     *             Thrown if the given protocol version is unknown.
     */
    public static short versionToMacLength(final byte version)
            throws UnkownProtocolVersionException {
        short length;
        switch (version) {
        case VERSION_160:
            length = 160 / 8;
            break;
        case VERSION_256:
            length = 256 / 8;
            break;
        case VERSION_384:
            length = 384 / 8;
            break;
        default:
            throw new UnkownProtocolVersionException(
                    "The protocol version " + version + " is unknown.");
        }
        return length;
    }

}
