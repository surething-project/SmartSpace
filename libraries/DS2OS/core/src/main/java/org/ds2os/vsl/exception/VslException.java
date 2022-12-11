package org.ds2os.vsl.exception;

/**
 * Base class for VSL exceptions.
 *
 * @author felix
 */
public abstract class VslException extends Exception {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 851774397503017928L;

    /**
     * Constructor with message string and cause.
     *
     * @param message
     *            the message to pass to users.
     * @param cause
     *            the Throwable which caused this exception.
     */
    protected VslException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with cause only (additional message string is usually more helpful!).
     *
     * @param cause
     *            the Throwable which caused this exception.
     */
    protected VslException(final Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /**
     * Constructor with message string only.
     *
     * @param message
     *            the message to pass to users.
     */
    protected VslException(final String message) {
        super(message);
    }

    /**
     * Get the major error code (HTTP error codes: XYY: X is major).
     *
     * @return Major error code (values 1-9 are valid).
     */
    public abstract byte getErrorCodeMajor();

    /**
     * Get the minor error code (HTTP error codes: XYY: YY is minor).
     *
     * @return Minor error code (values 0-99 are valid).
     */
    public abstract byte getErrorCodeMinor();

    /**
     * Get the error code (compatible to HTTP error codes).
     *
     * @return Error code.
     */
    public final int getErrorCode() {
        return getErrorCodeMajor() * 100 + getErrorCodeMinor();
    }

    /**
     * Get a string representation of the error code and the message.
     *
     * @return String of the error code and message.
     */
    @Override
    public final String toString() {
        return getErrorCode() + ": " + getMessage();
    }
}
