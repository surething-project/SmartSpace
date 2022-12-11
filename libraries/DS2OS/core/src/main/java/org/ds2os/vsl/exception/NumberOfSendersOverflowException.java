package org.ds2os.vsl.exception;

/**
 * This exception is thrown if an Assembler receives a packet from a new receiver which it cannot
 * process without violating the given sender quota.
 *
 * @author Johannes Straßer
 *
 */
public class NumberOfSendersOverflowException extends VslException {
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The major error code of this exception.
     */
    private static final byte ERROR_CODE_MAJOR = 4;

    /**
     * The minor error code of this exception.
     */
    private static final byte ERROR_CODE_MINOR = 13;

    /**
     * @see Exception#Exception(String)
     * @param sourceAddress
     *            The ID of the sender.
     * @param quota
     *            The quota.
     */
    public NumberOfSendersOverflowException(final String sourceAddress, final int quota) {
        super("Packets from the sender with the address" + sourceAddress
                + " cannot be processed as there is already the maximum number of senders (" + quota
                + ") present.");
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
