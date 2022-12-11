package org.ds2os.vsl.typeSearch.service;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslKORStructureHandler;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Type search service that allows for forward or reverse lookups of type to address mappings.
 * Access happens via virtualNodes.
 *
 * @author liebald
 */
public class TypeSearchService extends AbstractVslModule {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeSearchService.class);

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
     * Model ID of the statistics service.
     */
    private static final String MODEL_ID = "/searchProvider/type";

    /**
     * Address of the statistics service.
     */
    private String serviceAddress;

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
    public TypeSearchService(final VslConnector con, final VslStatisticsProvider statisticsProvider,
            final VslTypeSearchProvider typeSearchProvider) {
        this.con = con;
        this.statisticsProvider = statisticsProvider;
        this.typeSearchProvider = typeSearchProvider;
    }

    @Override
    public final void activate() throws Exception {
        try {
            serviceAddress = con.registerService(MANIFEST);
            LOGGER.debug("Registered type search service at {}", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on type search service registration: ", e);
            throw e;
        }

        try {
            con.registerVirtualNode(serviceAddress, new TypeSearchHandler(serviceAddress,
                    statisticsProvider, typeSearchProvider, con));
            LOGGER.debug("Registered virtual Node {}", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on registering virtual node at {} ", serviceAddress, e);
            throw e;
        }
//        try {
//            con.registerVirtualNode(serviceAddress + "/forward", new ForwardTypeSearchHandler(
//                    serviceAddress + "/forward", statisticsProvider, typeSearchProvider, con));
//            LOGGER.debug("Registered virtual Node {}", serviceAddress + "/forward");
//        } catch (final VslException e) {
//            LOGGER.error("Error on registering virtual node at {} ", serviceAddress + "/forward",
//                    e);
//            throw e;
//        }
//        try {
//            con.registerVirtualNode(serviceAddress + "/reverse", new ReverseTypeSearchHandler(
//                    serviceAddress + "/reverse", statisticsProvider, korStructureAccess, con));
//            LOGGER.debug("Registered virtual Node {}", serviceAddress + "/reverse");
//        } catch (final VslException e) {
//            LOGGER.error("Error on registering virtual node at {} ", serviceAddress + "/backward",
//                    e);
//            throw e;
//        }

    }

    @Override
    public void shutdown() {
    }

}
