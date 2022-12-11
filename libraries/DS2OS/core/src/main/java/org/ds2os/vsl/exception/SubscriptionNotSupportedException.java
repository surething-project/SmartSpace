package org.ds2os.vsl.exception;

/**
 * This exception is thrown if a subsription is not supported on the specified node. This is usually
 * thrown by virtual nodes which don't support notifications.
 *
 * @author felix
 */
public final class SubscriptionNotSupportedException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = -3776587723812380859L;

    /**
     * Default constructor with the address of the node.
     *
     * @param address
     *            The VSL address which does not support subscriptions.
     */
    public SubscriptionNotSupportedException(final String address) {
        super("subscription is not supported at node " + address);
    }

    @Override
    public byte getErrorCodeMajor() {
        return 5;
    }

    @Override
    public byte getErrorCodeMinor() {
        return 5;
    }
}
