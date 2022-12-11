package org.ds2os.vsl.statistics.service;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.config.VslStatisticsConfig;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.statistics.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistic service that allows access to internal KA statistics via the given
 * {@link StatisticsProvider}. Access happens via virtualNodes.
 *
 * @author liebald
 */
public class StatisticService extends AbstractVslModule {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticService.class);

    /**
     * The {@link StatisticsProvider} used to generate statistics.
     */
    private final StatisticsProvider statisticsProvider;

    /**
     * The {@link VslConnector} used for accessing the local KA.
     */
    private final VslConnector con;

    /**
     * Access to the statistic service related configuration.
     */
    private final VslStatisticsConfig config;

    /**
     * Model ID of the statistics service.
     */
    private static final String MODEL_ID = "/statistics/statisticsService";

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
     *            The {@link StatisticsProvider} used to generate statistics.
     * @param con
     *            The {@link VslConnector} used for accessing the local KA.
     * @param config
     *            Access to the statistic service related configuration.
     */
    public StatisticService(final StatisticsProvider statisticsProvider, final VslConnector con,
            final VslStatisticsConfig config) {
        this.statisticsProvider = statisticsProvider;
        this.con = con;
        this.config = config;

    }

    @Override
    public final void activate() throws Exception {
        statisticsProvider.setConfig(config);
        try {
            serviceAddress = con.registerService(MANIFEST);
            LOGGER.debug("Registered StatisticsService at {}", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on statistics service registration: ", e);
            throw e;
        }
        try {
            con.registerVirtualNode(serviceAddress + "/availableStatistics",
                    new AvailableStatisticsHandler(statisticsProvider, con.getNodeFactory()));
            LOGGER.debug("Registered virtual Node {}/availableStatistics", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on registering virtual node at {}/availableStatistics ",
                    serviceAddress, e);
            throw e;
        }
        try {
            con.registerVirtualNode(serviceAddress + "/getStatisticsFor",
                    new GetStatisticsHandler(serviceAddress + "/getStatisticsFor",
                            statisticsProvider, con.getNodeFactory()));
            LOGGER.debug("Registered virtual Node {}/getStatisticsFor", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on registering virtual node at {}/getStatisticsFor ",
                    serviceAddress, e);
            throw e;
        }
        try {
            con.registerVirtualNode(serviceAddress + "/getStatisticsSummaryFor",
                    new GetStatisticsSummaryHandler(serviceAddress + "/getStatisticsSummaryFor",
                            statisticsProvider, con.getNodeFactory()));
            LOGGER.debug("Registered virtual Node {}/getStatisticsSummaryFor", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on registering virtual node at {}/getStatisticsSummaryFor ",
                    serviceAddress, e);
            throw e;
        }
    }

    @Override
    public void shutdown() {
    }

}
