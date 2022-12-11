package org.ds2os.vsl.core.transport;

import java.util.UUID;

import org.ds2os.vsl.core.node.VslNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent as a response to the invocation of a callback. It can optionally transport an error.
 *
 * @author felix
 */
public class CallbackResponseMessage {

    /**
     * The callback identifier of the callback.
     */
    private final UUID callbackId;

    /**
     * The serial number of the invocation for reply matching.
     */
    private final long serial;

    /**
     * The transported exception containing an error, if there was an error.
     */
    private final ExceptionMessage error;

    /**
     * The VSL data for VSET response.
     */
    private final VslNode data;

    /**
     * Create a callback invocation message.
     *
     * @param callbackId
     *            the callback identifier of the callback which is invoked.
     * @param serial
     *            the serial number of the invocation for reply matching.
     * @param error
     *            the transported exception containing an error, if there was an error.
     * @param data
     *            the VSL data which is responded if this was a VSET operation.
     */
    @JsonCreator
    public CallbackResponseMessage(@JsonProperty("callbackId") final UUID callbackId,
            @JsonProperty("serial") final long serial,
            @JsonProperty("error") final ExceptionMessage error,
            @JsonProperty("data") final VslNode data) {
        this.callbackId = callbackId;
        this.serial = serial;
        this.error = error;
        this.data = data;
    }

    /**
     * Get the serial number of this invocation.
     *
     * @return the serial number.
     */
    public final long getSerial() {
        return serial;
    }

    /**
     * Get the callback identifier.
     *
     * @return the UUID.
     */
    public final UUID getCallbackId() {
        return callbackId;
    }

    /**
     * Get the transported exception containing an error, if there was an error.
     *
     * @return the transported exception or null for successful callbacks.
     */
    public final ExceptionMessage getError() {
        return error;
    }

    /**
     * Get the VSL data which is responded if this was a VSET operation.
     *
     * @return the {@link VslNode}.
     */
    public final VslNode getData() {
        return data;
    }
}
