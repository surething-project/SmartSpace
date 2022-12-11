package org.ds2os.vsl.core.utils;

import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for base64 encoding and decoding.
 *
 * @author felix
 */
public final class Base64 {

    /**
     * Utility class - no instantiation.
     */
    private Base64() {
        // utility class
    }

    /**
     * Encode the byte array as base64 string.
     *
     * @param data
     *            the byte array.
     * @return the base64 string.
     */
    public static String encode(final byte[] data) {
        return DatatypeConverter.printBase64Binary(data);
    }

    /**
     * Decode the base64 string to a byte array.
     *
     * @param data
     *            the base64 string.
     * @return the byte array.
     */
    public static byte[] decode(final String data) {
        return DatatypeConverter.parseBase64Binary(data);
    }
}
