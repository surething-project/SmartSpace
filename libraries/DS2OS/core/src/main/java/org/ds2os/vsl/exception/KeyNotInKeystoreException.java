package org.ds2os.vsl.exception;

/**
 * This exception is thrown if the given key is not found in the keystore.
 *
 * @author Johannes Straßer
 *
 */
public class KeyNotInKeystoreException extends VslException {
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MAJOR = 4;

    /**
     * The minor error code of this exception.
     */
    private static final byte ERROR_CODE_MINOR = 4;

    /**
     * @see Exception#Exception(String)
     *
     * @param message
     *            Message describing the error
     */
    public KeyNotInKeystoreException(final String message) {
        super(message);
    }

    @Override
    public final byte getErrorCodeMajor() {
        return ERROR_CODE_MAJOR;
    }

    @Override
    public final byte getErrorCodeMinor() {
        return ERROR_CODE_MINOR;
    }

}
