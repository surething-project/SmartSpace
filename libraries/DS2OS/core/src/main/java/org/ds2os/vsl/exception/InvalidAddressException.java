package org.ds2os.vsl.exception;

/**
 * This exception is thrown when a given address isn't valid (e.g. not supported parameters on a set
 * operation).
 *
 * @author liebald
 */
public class InvalidAddressException extends VslException {
    /**
     * Serial version uid.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MAJOR = 4;

    /**
     * The minor error code of this exception.
     */
    private static final byte ERROR_CODE_MINOR = 22;

    /**
     * @see Exception#Exception(String)
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public InvalidAddressException(final String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     * @param cause
     *            the cause (which is saved for later retrieval by the Throwable.getCause() method).
     *            (A null value is permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public InvalidAddressException(final Throwable cause) {
        super(cause);
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
