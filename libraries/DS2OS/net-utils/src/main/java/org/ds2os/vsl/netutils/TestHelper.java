package org.ds2os.vsl.netutils;

import java.util.Random;

/**
 * Class containing various helper methods that are used by the unit tests.
 *
 * @author Johannes Straßer
 *
 */
public final class TestHelper {

    /**
     * Random generator used by {@link randomData}.
     */
    private static final Random RANDOM = new Random();

    /**
     * Stub constructor to hide default constructor.
     */
    private TestHelper() {
        // Nothing
    }

    /**
     * Creates a byte array of the specified length containing random data.
     *
     * @param length
     *            The length of the byte array to be created
     * @return A byte array of the given length containing random data
     */
    public static byte[] randomData(final int length) {
        final byte[] data = new byte[length];
        RANDOM.nextBytes(data);
        return data;
    }

    /**
     * Create a random string skipping the first 32 char values (control sequences).
     *
     * @param length
     *            the length of the resulting string in characters.
     * @return the random string.
     */
    public static String randomString(final int length) {
        final char[] data = new char[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (char) (32 + RANDOM.nextInt(Character.MAX_VALUE - 32));
        }
        return new String(data);
    }
}
