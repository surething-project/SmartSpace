package org.ds2os.vsl.core.config;

/**
 * Interface for the configuration of the VslAgentRegistry.
 *
 * @author liebald
 */
public interface VslAgentRegistryConfig extends VslAgentName {

    /**
     * Returns the time that lies between two calls of the
     * {@link org.ds2os.vsl.core.VslAgentRegistryService#cleanStaleAgents(long)} method.
     *
     * @return interval between calls of cleanStaleAgents in milliseconds
     */
    @ConfigDescription(description = "How often is the agentRegistry checked for stale agents "
            + "in milliseconds", id = "korSync.agentRegistryCleanerInterval"
                    + "", defaultValue = "30000", restrictions = ">0")
    long getAgentRegistryCleanerInterval();

    /**
     * Returns the time after which entries in the
     * {@link org.ds2os.vsl.core.VslAgentRegistryService} connected and unconnected lists are
     * considered stale. Not that cleaning these entries will occur after at least
     * getAgentRegistryStalenessTime but before maximum getAgentRegistryStalenessTime +
     * getAgentRegistryCleanerInterval.
     *
     * @return the time after which entries in the agentRegistry are considered stale in
     *         milliseconds
     */
    @ConfigDescription(description = "Time after which entries in the agentRegistry are considered "
            + "stale in milliseconds.", id = "korSync.agentRegistryStalenessTime"
                    + "", defaultValue = "60000", restrictions = ">=0")
    long getAgentRegistryStalenessTime();
}
