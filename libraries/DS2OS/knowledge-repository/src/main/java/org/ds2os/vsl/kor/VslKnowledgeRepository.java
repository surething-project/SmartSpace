package org.ds2os.vsl.kor;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.VslKORHash;
import org.ds2os.vsl.core.VslKORStructureHandler;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.config.VslKORConfig;

/**
 * Interface for the KnowledgeRepository.
 *
 * @author liebald
 *
 */
public interface VslKnowledgeRepository extends VslRequestHandler, VslModelInstantiationHandler,
        VslKORUpdateHandler, VslKORCacheHandler, VslKORStructureHandler, VslKORHash {
    /**
     * Sets the configuration service. Until this is done the KOR will use an intermediate
     * configuration.
     *
     * @param configService
     *            The config service to use.
     * @param connector
     *            The {@link VslConnector} used to communicate with other KAs.
     * @throws Exception
     *             Thrown if something unexpected happens.
     */
    void activate(VslKORConfig configService, VslConnector connector) throws Exception;

}
