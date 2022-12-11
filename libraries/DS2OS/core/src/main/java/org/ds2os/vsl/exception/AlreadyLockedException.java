package org.ds2os.vsl.exception;

/**
 * This exception is thrown if a lockSubtree operation is issued twice on the same connector to the
 * same address.
 *
 * @author felix
 */
public final class AlreadyLockedException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 4808148296794347025L;

    /**
     * Default constructor with the address which was locked again by the same service.
     *
     * @param address
     *            The VSL address which is already locked.
     */
    public AlreadyLockedException(final String address) {
        super("lockSubtree was issued twice by the same service at node " + address);
    }

    @Override
    public byte getErrorCodeMajor() {
        return 4;
    }

    @Override
    public byte getErrorCodeMinor() {
        return 22;
    }
}
