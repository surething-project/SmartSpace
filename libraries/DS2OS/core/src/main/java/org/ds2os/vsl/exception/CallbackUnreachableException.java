package org.ds2os.vsl.exception;

/**
 * An exception for when a virtual node callback is unreachable.
 */
public class CallbackUnreachableException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = -3776522884912380859L;

    /**
     * Default constructor with the client id of the unreachable callback.
     *
     * @param clientId
     *            The id of the client who was unreachable.
     */
    public CallbackUnreachableException(final String clientId) {
        super("callback is not reachable for client with id: " + clientId);
    }

    @Override
    public byte getErrorCodeMajor() {
        return 4;
    }

    @Override
    public byte getErrorCodeMinor() {
        return 4;
    }
}
