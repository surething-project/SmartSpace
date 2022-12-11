package org.ds2os.vsl.core;

import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.exception.NodeNotExistingException;

/**
 * Abstract implementation of {@link VslRequestHandler} which routes requests to remote KAs or the
 * local KA depending on the address.
 *
 * @author liebald
 * @author felix
 */
public abstract class AbstractRequestRouter implements VslRequestHandler {

    /**
     * The local agent name configuration.
     */
    private final VslAgentName agentName;

    /**
     * Constructor for subclasses.
     *
     * @param agentName
     *            the local agent name configuration.
     */
    protected AbstractRequestRouter(final VslAgentName agentName) {
        this.agentName = agentName;
    }

    /**
     * Resolve the given address based on the identity to an absolute address.
     *
     * @param address
     *            the address the service provided.
     * @param identity
     *            the identity of the service.
     * @return the resolved = absolute address.
     * @throws NodeNotExistingException
     *             If the address is not resolvable or invalid.
     */
    protected final String resolveAddress(final String address, final VslIdentity identity)
            throws NodeNotExistingException {
        if (address.startsWith("~") || address.startsWith("/~") || address.startsWith("~/")
                || address.startsWith("/~/")) {
            return "/" + getAgentId() + "/" + identity.getClientId()
                    + address.substring(address.indexOf("~") + 1);
        } else if (address.startsWith("/search/")) {
            return "/" + getAgentId() + "/search" + address.substring(7);
        } else if (address.startsWith("/")) {
            return address;
        } else {
            throw new NodeNotExistingException("Node address" + address + " is not resolvable.");
        }
    }

    /**
     * Extract the agent id out of a fully resolved address.
     *
     * @param resolvedAddress
     *            the resolved address.
     * @return the agent id.
     */
    protected final String extractAgentId(final String resolvedAddress) {
        final int secondSlash = resolvedAddress.indexOf('/', 1);
        if (secondSlash < 0) {
            return resolvedAddress.substring(1);
        } else {
            return resolvedAddress.substring(1, secondSlash);
        }
    }

    /**
     * Get the {@link VslAgentName} configuration.
     *
     * @return the agent name configuration instance.
     */
    public final VslAgentName getAgentName() {
        return agentName;
    }

    /**
     * Get the local agent id.
     *
     * @return the local agent id.
     */
    public final String getAgentId() {
        return agentName.getAgentName();
    }
}
