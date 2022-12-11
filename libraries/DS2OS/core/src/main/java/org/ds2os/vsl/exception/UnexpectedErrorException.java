package org.ds2os.vsl.exception;

/**
 * An exception for an unexpected error.
 */
public class UnexpectedErrorException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = -3776522884912380859L;

    /**
     * Default constructor with a description of what went wrong.
     *
     * @param msg
     *            The message of what want wrong.
     */
    public UnexpectedErrorException(final String msg) {
        super("an unexpected error occurred: " + msg);
    }

    /**
     * Default constructor with a description of what went wrong and a cause.
     *
     * @param msg
     *      the message of what want wrong.
     * @param cause
     *      the cause of the exception.
     */
    public UnexpectedErrorException(final String msg, final Throwable cause) {
        super("an unexpected error occurred: " + msg, cause);
    }

    @Override
    public byte getErrorCodeMajor() {
        return 5;
    }

    @Override
    public byte getErrorCodeMinor() {
        return 0;
    }
}
