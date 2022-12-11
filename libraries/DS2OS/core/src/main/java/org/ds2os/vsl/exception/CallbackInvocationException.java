package org.ds2os.vsl.exception;

/**
 * Exception of the transport if the invocation of the callback fails after receiving a callback
 * invocation message.
 *
 * @author felix
 */
public final class CallbackInvocationException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = -4494767665453587949L;

    /**
     * Create a callback invocation exception with a message.
     *
     * @param message
     *            the message.
     */
    public CallbackInvocationException(final String message) {
        super(message);
    }

    /**
     * Create a callback invocation exception with a message and a cause.
     *
     * @param message
     *            the message.
     * @param cause
     *            the cause.
     */
    public CallbackInvocationException(final String message, final Throwable cause) {
        super(message, cause);
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
