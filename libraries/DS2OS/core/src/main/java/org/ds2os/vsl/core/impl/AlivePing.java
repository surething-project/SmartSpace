package org.ds2os.vsl.core.impl;

import java.util.Collection;

import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslTransportConnector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Alive ping data object.
 *
 * @author felix
 */
public class AlivePing extends KAInfo implements VslAlivePing {

    /**
     * Number of KAs in the established overlay.
     */
    private final int numKAs;

    /**
     * Public key of the CA certificate.
     */
    private final String caPub;

    /**
     * The groupID of the established overlay. At the moment this is a hash of the groupKey.
     */
    private final String groupID;

    /**
     * Default constructor.
     *
     * @param agentId
     *            The agent identifier.
     * @param numKAs
     *            Number of KAs in the established overlay.
     * @param caPub
     *            Public key of the CA certificate.
     * @param transports
     *            Set of all supported {@link VslTransportConnector}s of this agent.
     * @param groupID
     *            ID of the group of the established overlay
     * @param korHash
     *            The hash of this agents KOR
     */
    @JsonCreator
    public AlivePing(@JsonProperty("agentID") final String agentId,
            @JsonProperty("numKAs") final int numKAs, @JsonProperty("caPub") final String caPub,
            @JsonProperty("transports") final Collection<VslTransportConnector> transports,
            @JsonProperty("groupID") final String groupID,
            @JsonProperty("korHash") final String korHash) {
        super(agentId, transports, korHash);
        this.numKAs = numKAs;
        this.caPub = caPub;
        this.groupID = groupID;
    }

    @Override
    public final int getNumKAs() {
        return numKAs;
    }

    @Override
    public final String getCaPub() {
        return caPub;
    }

    @Override
    public final String getGroupID() {
        return groupID;
    }
}
