package org.ds2os.vsl.core.transport;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object of a POST body describing a VSL operation.
 *
 * @author felix
 */
public class PostOperation {

    /**
     * The operation performed by the POST request.
     */
    private final OperationType operation;

    /**
     * The callback identifier of the optional callback.
     */
    private final UUID callbackId;

    // TODO: callbackUrl

    /**
     * Constructor with operation type only.
     *
     * @param operation
     *            the operation performed by the POST request.
     */
    public PostOperation(final OperationType operation) {
        this.operation = operation;
        this.callbackId = null;
    }

    /**
     * Constructor with operation type and callback Id.
     *
     * @param operation
     *            the operation performed by the POST request.
     * @param callbackId
     *            the callback identifier.
     */
    @JsonCreator
    public PostOperation(@JsonProperty("operation") final OperationType operation,
            @JsonProperty("callbackId") final UUID callbackId) {
        this.operation = operation;
        this.callbackId = callbackId;
    }

    /**
     * Get the operation performed by the POST request.
     *
     * @return the {@link OperationType}.
     */
    public final OperationType getOperation() {
        return operation;
    }

    /**
     * Get the optional callback identifier of the operation.
     *
     * @return the callback identifier or null, if no callback is specified.
     */
    public final UUID getCallbackId() {
        return callbackId;
    }

    /**
     * Operation type to be performed.
     *
     * @author felix
     */
    public enum OperationType {

        /**
         * Notify the KA about changes on a virtual node.
         */
        NOTIFY,

        /**
         * Subscribe to notifications.
         */
        SUBSCRIBE,

        /**
         * Unsubscribe from notifications.
         */
        UNSUBSCRIBE,

        /**
         * Lock a subtree.
         */
        LOCK_SUBTREE,

        /**
         * Commit to a locked subtree.
         */
        COMMIT_SUBTREE,

        /**
         * Rollback changes of the subtree.
         */
        ROLLBACK_SUBTREE,

        /**
         * Register a virtual node.
         */
        REGISTER_VIRTUAL_NODE,

        /**
         * Unregister a virtual node.
         */
        UNREGISTER_VIRTUAL_NODE
    }
}
