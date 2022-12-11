package org.ds2os.vsl.core.config;

/**
 * This interface offers access to Aliveping related configuration options.
 *
 * @author liebald
 */
public interface VslAlivePingConfig extends VslAgentName {

    /**
     * Returns the intervall in which alivePings are sent in seconds.
     *
     * @return Intervall between two alivePings in seconds.
     */
    @ConfigDescription(description = "Time between two AlivePings sent by the Agent in seconds."
            + "", id = "alivePing.senderIntervall", defaultValue = "2", restrictions = ">0")
    int getAlivePingIntervall();
}
