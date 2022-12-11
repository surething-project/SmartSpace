package org.ds2os.vsl.core.transport;

import java.util.UUID;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslNode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent for invocation of a callback.
 *
 * @author felix
 */
public class CallbackInvocationMessage {

    /**
     * The callback identifier of the callback.
     */
    private final UUID callbackId;

    /**
     * The serial number of the invocation for reply matching.
     */
    private final long serial;

    /**
     * The VSL address on which the callback is invoked.
     */
    private final String address;

    /**
     * The {@link CallbackMethod} to invoke.
     */
    private final CallbackMethod invoke;

    /**
     * The {@link VslIdentity} of the invoker (only really relevant for get/set on virtual nodes).
     */
    private final VslIdentity identity;

    /**
     * The {@link VslNode} data send with the VSET request (null otherwise).
     */
    private final VslNode data;

    /**
     * Create a callback invocation message.
     *
     * @param callbackId
     *            the callback identifier of the callback which is invoked.
     * @param serial
     *            the serial number of the invocation for reply matching.
     * @param address
     *            the VSL address on which the callback is invoked.
     * @param invoke
     *            the {@link CallbackMethod} to invoke.
     * @param identity
     *            the {@link VslIdentity} of the invoker.
     * @param data
     *            the {@link VslNode} data send with the VSET request (null otherwise).
     */
    @JsonCreator
    public CallbackInvocationMessage(@JsonProperty("callbackId") final UUID callbackId,
            @JsonProperty("serial") final long serial,
            @JsonProperty("address") final String address,
            @JsonProperty("invoke") final CallbackMethod invoke,
            @JsonProperty("identity") final VslIdentity identity,
            @JsonProperty("data") final VslNode data) {
        this.callbackId = callbackId;
        this.serial = serial;
        this.address = address;
        this.invoke = invoke;
        this.identity = identity;
        this.data = data;
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
     * Get the serial number of this invocation.
     *
     * @return the serial number.
     */
    public final long getSerial() {
        return serial;
    }

    /**
     * Get the affected VSL address.
     *
     * @return the address.
     */
    public final String getAddress() {
        return address;
    }

    /**
     * Get the invoked {@link CallbackMethod}.
     *
     * @return the callback method.
     */
    @JsonProperty("invoke")
    public final CallbackMethod getInvokedMethod() {
        return invoke;
    }

    /**
     * Get the identity of the invoker (only really relevant for get/set on virtual nodes).
     *
     * @return the {@link VslIdentity}.
     */
    public final VslIdentity getIdentity() {
        return identity;
    }

    /**
     * Get the data send with the VSET request (null otherwise).
     *
     * @return the {@link VslNode}.
     */
    public final VslNode getData() {
        return data;
    }
}
