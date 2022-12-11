package org.ds2os.vsl.core.transport;

/**
 * Interface for listening to callback responses on a stateful transport channel.
 *
 * @author felix
 */
public interface CallbackResponseListener {

    /**
     * Triggered when a callback response is received.
     *
     * @param message
     *            the {@link CallbackResponseMessage}.
     */
    void receivedResponse(final CallbackResponseMessage message);
}
