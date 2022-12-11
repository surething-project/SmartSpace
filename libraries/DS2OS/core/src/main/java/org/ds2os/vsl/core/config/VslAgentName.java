package org.ds2os.vsl.core.config;

/**
 * @author jay
 *
 */
public interface VslAgentName {

    /**
     * Method to get the Agent Name of KA.
     *
     * @return String representation of the Agent name.
     */
    @ConfigDescription(description = "The (unique) name of the local agent."
            + "", id = "ka.agentName", defaultValue = "agent1", restrictions = "none")
    String getAgentName();
}
