package org.ds2os.vsl.core.transport;

import java.util.UUID;

import org.ds2os.vsl.exception.VslException;

/**
 * Interface for sending callback invocations over a stateful transport channel.
 *
 * @author felix
 */
public interface CallbackSender {

    /**
     * Set the {@link CallbackResponseListener} which is responsible for the specified callback id.
     *
     * @param callbackId
     *            the callback id.
     * @param responseListener
     *            the response listener. Replaces a previously set response listener.
     */
    void setResponseListener(UUID callbackId, CallbackResponseListener responseListener);

    /**
     * Invoke a callback at the other side by sending the {@link CallbackInvocationMessage}.
     * Notifies the set {@link CallbackResponseListener} once the callback response is received.
     *
     * @param invocationMessage
     *            the invocation message to send.
     * @throws VslException
     *             If the invocation fails.
     */
    void invokeCallback(CallbackInvocationMessage invocationMessage) throws VslException;
}
