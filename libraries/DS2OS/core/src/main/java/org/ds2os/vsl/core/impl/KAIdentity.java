package org.ds2os.vsl.core.impl;

import java.util.Arrays;
import java.util.Collection;

import org.ds2os.vsl.core.VslIdentity;

/**
 * {@link VslIdentity} specifically for KAs in KA to KA communication. Currently not sent by
 * transports and therefore not serializable.
 *
 * @author felix
 */
public final class KAIdentity implements VslIdentity {

    /**
     * The agent id of the KA.
     */
    private final String agentId;

    /**
     * Create a new KA identity.
     *
     * @param agentId
     *            the agent id of the KA.
     */
    public KAIdentity(final String agentId) {
        this.agentId = agentId;
    }

    @Override
    public String getClientId() {
        return agentId;
    }

    @Override
    public Collection<String> getAccessIDs() {
        return Arrays.asList("system");
    }

    @Override
    public boolean isKA() {
        return true;
    }
}
