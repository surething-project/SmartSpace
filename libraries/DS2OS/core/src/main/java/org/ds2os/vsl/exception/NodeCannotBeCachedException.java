package org.ds2os.vsl.exception;

/**
 * Exception that is thrown when a node that should be cached can't be cached. Used internally by
 * the cache.
 *
 * @author liebald
 */
public class NodeCannotBeCachedException extends VslException {

    /**
     * Serial version uid.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MAJOR = 4;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MINOR = 4;

    /**
     * @see Exception#Exception(String)
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the
     *            Throwable.getMessage() method.
     */
    public NodeCannotBeCachedException(final String message) {
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
