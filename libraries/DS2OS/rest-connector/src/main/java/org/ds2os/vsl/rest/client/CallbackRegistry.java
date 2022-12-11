package org.ds2os.vsl.rest.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.ds2os.vsl.core.VslCallback;

/**
 * Helper class which registers callbacks with UUIDs.
 *
 * @author felix
 */
public final class CallbackRegistry {

    /**
     * Map the {@link VslCallback}s by their {@link UUID}.
     */
    private final Map<UUID, VslCallback> callbackByUUID;

    /**
     * Map the {@link UUID} for each {@link VslCallback}.
     */
    private final Map<VslCallback, UUID> uuidByCallback;

    /**
     * Default constructor.
     */
    public CallbackRegistry() {
        callbackByUUID = new HashMap<UUID, VslCallback>();
        uuidByCallback = new HashMap<VslCallback, UUID>();
    }

    /**
     * Register a callback and obtain its UUID.
     *
     * @param callback
     *            the {@link VslCallback} to register.
     * @return the {@link UUID} assigned to the callback.
     */
    public UUID registerCallback(final VslCallback callback) {
        synchronized (callbackByUUID) {
            UUID uuid = uuidByCallback.get(callback);
            if (uuid == null) {
                do {
                    uuid = UUID.randomUUID();
                } while (callbackByUUID.containsKey(uuid));
                uuidByCallback.put(callback, uuid);
                callbackByUUID.put(uuid, callback);
            }
            return uuid;
        }
    }

    /**
     * Get the callback by its UUID.
     *
     * @param uuid
     *            the {@link UUID} to look up.
     * @return the {@link VslCallback} which was registered with this UUID or null, if there is no
     *         callback registered for this UUID.
     */
    public VslCallback getCallback(final UUID uuid) {
        synchronized (callbackByUUID) {
            return callbackByUUID.get(uuid);
        }
    }
}
