package org.ds2os.vsl.exception;

/**
 * This exception is thrown if there is a packet with an unknown protocol version or if the used
 * cipher suite cannot be translated into a known protocol version..
 *
 * @author Johannes Straßer
 *
 */
public class UnkownProtocolVersionException extends VslException {
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
    private static final byte ERROR_CODE_MINOR = 4;

    /**
     * @see Exception#Exception(String)
     *
     * @param message
     *            Message describing the error
     */
    public UnkownProtocolVersionException(final String message) {
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
