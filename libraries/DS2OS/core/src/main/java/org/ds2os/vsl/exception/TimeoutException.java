package org.ds2os.vsl.exception;

import java.util.concurrent.TimeUnit;

/**
 * An exception for a timeout.
 */
public final class TimeoutException extends VslException {

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = -2776587723864844859L;

    /**
     * Default constructor with the timeout in nanoseconds and the message.
     *
     * @param timeout
     *      The timeout that expired in nanoseconds.
     * @param msg
     *      The message for the exception.
     */
    public TimeoutException(final long timeout, final String msg) {
        this(timeout, TimeUnit.NANOSECONDS, msg);
    }

    /**
     * Constructor with the timeout and its time unit, with the message.
     * @param timeout
     *      The timeout that expired.
     * @param unit
     *      The unit of the timeout.
     * @param msg
     *      The message for the exception.
     */
    public TimeoutException(final long timeout, final TimeUnit unit, final String msg) {
        super("operation took longer than " + timeout + unit + "; " + msg);
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
