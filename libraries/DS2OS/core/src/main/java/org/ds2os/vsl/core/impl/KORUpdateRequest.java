package org.ds2os.vsl.core.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KOR update request parameters in one Json object for transports.
 *
 * @author felix
 */
public class KORUpdateRequest {

    /**
     * The id of the agent to request data from.
     */
    private final String agentId;

    /**
     * The KOR hash to request incremental update from.
     */
    private final String hashFrom;

    /**
     * Creates a new KORUpdateRequest object.
     *
     * @param agentId
     *            the id of the agent to request data from.
     * @param hashFrom
     *            the KOR hash to request incremental update from or empty string for full update.
     */
    @JsonCreator
    public KORUpdateRequest(@JsonProperty("agentId") final String agentId,
            @JsonProperty("hashFrom") final String hashFrom) {
        this.agentId = agentId;
        this.hashFrom = hashFrom;
    }

    /**
     * Get the id of the agent to request data from.
     *
     * @return the agent id.
     */
    public final String getAgentId() {
        return agentId;
    }

    /**
     * Get the KOR hash to request incremental update from.
     *
     * @return the KOR hash or empty string for full update.
     */
    public final String getHashFrom() {
        return hashFrom;
    }
}
