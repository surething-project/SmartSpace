package org.ds2os.vsl.core;

import java.util.Collection;

import org.ds2os.vsl.core.impl.KAInfo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Information about a remote KA which can be exchanged between KAs.
 *
 * @author felix
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = KAInfo.class)
public interface VslKAInfo {

    /**
     * Get the agent identifier.
     *
     * @return agent identifier as string.
     */
    String getAgentId();

    /**
     * Get the hash of the KOR.
     *
     * @return KOR hash as a string
     */
    String getKorHash();

    /**
     * Get all supported {@link VslTransportConnector}s of this agent.
     *
     * @return Collection of {@link VslTransportConnector}.
     */
    Collection<VslTransportConnector> getTransports();
}
