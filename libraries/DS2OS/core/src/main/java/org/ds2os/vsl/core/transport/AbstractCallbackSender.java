package org.ds2os.vsl.core.transport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Abstract base functionality for {@link CallbackSender}, mainly dealing with request/response
 * mapping.
 *
 * @author felix
 */
public abstract class AbstractCallbackSender implements CallbackSender {

    /**
     * Map to track {@link CallbackResponseListener} per callback id.
     */
    private final ConcurrentMap<UUID, CallbackResponseListener> callbackResponseListeners;

    /**
     * Default constructor.
     */
    protected AbstractCallbackSender() {
        callbackResponseListeners = new ConcurrentHashMap<UUID, CallbackResponseListener>();
    }

    /**
     * Get the {@link CallbackResponseListener} for the specified callback id.
     *
     * @param callbackId
     *            the callback id.
     * @return the {@link CallbackResponseListener} or null, if there is none.
     */
    protected final CallbackResponseListener getResponseListener(final UUID callbackId) {
        return callbackResponseListeners.get(callbackId);
    }

    @Override
    public final void setResponseListener(final UUID callbackId,
            final CallbackResponseListener responseListener) {
        callbackResponseListeners.put(callbackId, responseListener);
    }
}
