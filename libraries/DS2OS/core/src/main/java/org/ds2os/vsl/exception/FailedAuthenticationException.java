package org.ds2os.vsl.exception;

/**
 * @author jay, felix
 */
public final class FailedAuthenticationException extends VslException {

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
    private static final byte ERROR_CODE_MINOR = 1;

    /**
     * Public constructor for the exception.
     *
     * @param message
     *            The message to be sent to the caller.
     */
    public FailedAuthenticationException(final String message) {
        super("Could not authenticate the provided certificate. " + message);
    }

    /**
     * Public constructor for the exception.
     *
     * @param message
     *            The message to be sent to the caller.
     * @param cause
     *            the throwable which caused this exception.
     */
    public FailedAuthenticationException(final String message, final Throwable cause) {
        super("Could not authenticate the provided certificate. " + message, cause);
    }

    @Override
    public byte getErrorCodeMajor() {
        return ERROR_CODE_MAJOR;
    }

    @Override
    public byte getErrorCodeMinor() {
        return ERROR_CODE_MINOR;
    }
}
