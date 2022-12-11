package org.ds2os.vsl.exception;

public class StreamClosedException extends VslException {
    /**
     * Serial version uid.
     */
    private static final long serialVersionUID = 16484846845L;

    /**
     * @see Exception#Exception(String)
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public StreamClosedException(final String message) {
        super(message);
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
