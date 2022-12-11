package org.ds2os.vsl.agentregistry;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.config.VslAgentRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for regularly cleaning stale agents.
 *
 * @author liebald
 *
 */
public class AgentRegistryCleaner extends Thread {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentRegistryCleaner.class);
    /**
     * The configuration of the {@link AgentRegistryService}.
     */
    private final VslAgentRegistryConfig config;

    /**
     * The {@link AgentRegistryService} to control.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * Constructor.
     *
     * @param config
     *            The configuration of the {@link AgentRegistryService}.
     * @param agentRegistryService
     *            The {@link AgentRegistryService} to control.
     */
    public AgentRegistryCleaner(final VslAgentRegistryConfig config,
            final VslAgentRegistryService agentRegistryService) {
        this.setName("AgentRegistryCleaner");
        this.setDaemon(true);
        this.config = config;
        agentRegistry = agentRegistryService;

    }

    @Override
    public final void run() {
        final long agentRegistryCleanerInterval = config.getAgentRegistryCleanerInterval();
        final long agentRegistryStalenessTime = config.getAgentRegistryStalenessTime();
        try {
            while (!Thread.interrupted()) {
                Thread.sleep(agentRegistryCleanerInterval);
                agentRegistry.cleanStaleAgents(agentRegistryStalenessTime);
            }
        } catch (final InterruptedException e) {
            LOGGER.error("AgentRegistry cleaner thread interrupted.");
        }

    }

}
