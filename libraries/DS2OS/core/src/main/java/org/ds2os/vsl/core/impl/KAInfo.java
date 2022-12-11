package org.ds2os.vsl.core.impl;

import java.util.Collection;
import java.util.Collections;

import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslTransportConnector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link VslKAInfo}.
 *
 * @author felix
 */
public class KAInfo implements VslKAInfo {

    /**
     * The agent identifier.
     */
    private final String agentId;

    /**
     * Set of all supported {@link VslTransportConnector}s of this agent.
     */
    private final Collection<VslTransportConnector> transports;

    /**
     * The hash of the KOR.
     */
    private final String korHash;

    /**
     * Constructor of the KA information data object.
     *
     * @param agentId
     *            The agent identifier.
     * @param transports
     *            Set of all supported {@link VslTransportConnector}s of this agent.
     * @param korHash
     *            The has of this agents KOR.
     */
    @JsonCreator
    public KAInfo(@JsonProperty("agentID") final String agentId,
            @JsonProperty("transports") final Collection<VslTransportConnector> transports,
            @JsonProperty("korHash") final String korHash) {
        this.agentId = agentId;
        this.transports = Collections.unmodifiableCollection(transports);
        this.korHash = korHash;
    }

    @Override
    public final String getAgentId() {
        return agentId;
    }

    @Override
    public final Collection<VslTransportConnector> getTransports() {
        return transports;
    }

    @Override
    public final String getKorHash() {
        return korHash;
    }
}
