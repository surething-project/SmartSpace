package org.ds2os.vsl.core.transport;

import org.ds2os.vsl.exception.VslException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The parts of an exception which get transported via VSL transports.
 *
 * @author felix
 */
public final class ExceptionMessage {

    /**
     * The status code.
     */
    private final int code;

    /**
     * The error message.
     */
    private final String message;

    /**
     * The stored (or generated) VSL exception.
     */
    @JsonIgnore
    private final VslException exception;

    /**
     * Create exception message from VSL exception.
     *
     * @param exception
     *            the original VSL exception.
     */
    public ExceptionMessage(final VslException exception) {
        this.exception = exception;
        this.code = exception.getErrorCode();
        this.message = exception.getMessage();
    }

    /**
     * Json creator for deserialization of a received exception.
     *
     * @param code
     *            the status code.
     * @param message
     *            the error message.
     * @param exceptionType
     *            the type of the exception.
     */
    @JsonCreator
    public ExceptionMessage(@JsonProperty("code") final int code,
            @JsonProperty("message") final String message,
            @JsonProperty("type") final String exceptionType) {
        this.code = code;
        this.message = message;

        // TODO: typed deserialization.
        this.exception = new VslException(message) {

            /**
             * Exceptions must be serializable.
             */
            private static final long serialVersionUID = -6265828576226617162L;

            @Override
            public byte getErrorCodeMajor() {
                return (byte) (code / 100);
            }

            @Override
            public byte getErrorCodeMinor() {
                return (byte) (code % 100);
            }
        };
    }

    /**
     * Get the status code of the exception.
     *
     * @return the status code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the error message of the exception.
     *
     * @return the error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the exception type which is the class name (including packet).
     *
     * @return the type string.
     */
    public String getType() {
        return exception.getClass().getName();
    }

    /**
     * Convert to a real {@link VslException}.
     *
     * @return the VSL exception object.
     */
    @JsonIgnore
    public VslException toException() {
        return exception;
    }
}
