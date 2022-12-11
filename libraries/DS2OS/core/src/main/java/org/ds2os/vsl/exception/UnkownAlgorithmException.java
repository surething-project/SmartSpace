package org.ds2os.vsl.exception;

/**
 * This exception is thrown if the SymmetricCryptographyHandler cannot get a provider for an
 * algorithm specified in the given TLS string.
 *
 * @author Johannes Straßer
 *
 */
public class UnkownAlgorithmException extends VslException {
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
    public UnkownAlgorithmException(final String message) {
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
