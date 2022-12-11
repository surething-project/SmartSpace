package org.ds2os.vsl.searchProvider;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search provider services that allows easy mapping between requests to the local /search subnode
 * and the corresponding search providers.
 *
 * @author liebald
 */
public class SearchProviderService extends AbstractVslModule {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchProviderService.class);

    /**
     * The {@link VslConnector} used for accessing the local KA.
     */
    private final VslConnector con;

    /**
     * The {@link VslStatisticsProvider} used to generate statistics.
     */
    private final VslStatisticsProvider statisticsProvider;

    /**
     * The {@link VslTypeSearchProvider} used for retrieving the mapping between types and
     * addresses.
     */
    private final VslTypeSearchProvider typeSearchProvider;

    /**
     * Model ID of the searchprovider service.
     */
    private static final String MODEL_ID = "/searchProvider/search";

    /**
     * Service manifest object. At the moment, created dummy instance.
     */
    private static final VslServiceManifest MANIFEST = new VslServiceManifest() {

        @Override
        public String getModelId() {
            return MODEL_ID;
        }

        @Override
        public String getModelHash() {
            return "";
        }

        @Override
        public String getBinaryHash() {
            return "";
        }
    };

    /**
     * Constructor.
     *
     * @param statisticsProvider
     *            The {@link VslStatisticsProvider} used to generate statistics.
     * @param con
     *            The {@link VslConnector} used for accessing the local KA.
     * @param typeSearchProvider
     *            The {@link VslTypeSearchProvider} used for retrieving the mapping between types
     *            and addresses.
     */
    public SearchProviderService(final VslConnector con,
            final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearchProvider) {
        this.con = con;
        this.statisticsProvider = statisticsProvider;
        this.typeSearchProvider = typeSearchProvider;
    }

    @Override
    public final void activate() throws Exception {
        try {
            con.registerService(MANIFEST);
            LOGGER.debug("Registered type search service at {}", con.getRegisteredAddress());
        } catch (final VslException e) {
            LOGGER.error("Error on type search service registration: ", e);
            throw e;
        }

        try {
            con.registerVirtualNode(con.getRegisteredAddress(),
                    new SearchProviderHandler(con, statisticsProvider, typeSearchProvider));
            LOGGER.debug("Registered search provider service virtual node at {}",
                    con.getRegisteredAddress());
        } catch (final VslException e) {
            LOGGER.error("Error on search provider service registration: ", e);
            throw e;
        }

    }

    @Override
    public void shutdown() {
    }

}
