package org.ds2os.vsl.exception;

/**
 * An exception for when the execution of e.g. a thread has been interrupted.
 */
public class InterruptedOperationException extends VslException {

    /**
     * Serial version uid.
     */
    private static final long serialVersionUID = -3855831915889713911L;

    /**
     * @see Exception#Exception(String)
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public InterruptedOperationException(final String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     * @param cause
     *            the cause (which is saved for later retrieval by the Throwable.getCause() method).
     *            (A null value is permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public InterruptedOperationException(final Throwable cause) {
        super(cause);
    }

    @Override
    public final byte getErrorCodeMajor() {
        return 5;
    }

    @Override
    public final byte getErrorCodeMinor() {
        return 0;
    }
}
