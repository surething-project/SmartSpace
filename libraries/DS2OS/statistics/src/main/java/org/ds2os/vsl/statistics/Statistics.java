package org.ds2os.vsl.statistics;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.ds2os.vsl.core.config.VslStatisticsConfig;
import org.ds2os.vsl.core.statistics.VslStatistics;
import org.ds2os.vsl.core.statistics.VslStatisticsDatapoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VslStatistics}.
 *
 * @author liebald
 */
public class Statistics implements VslStatistics {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Statistics.class);

    /**
     * List of all {@link StatisticsDatapoint}s created.
     */
    private final List<StatisticsDatapoint> datapoints;

    /**
     * The configuration for the Statistics service and related classes.
     */
    private VslStatisticsConfig config;

    /**
     * Constructor.
     *
     * @param config
     *            The configuration for the Statistics service and related classes.
     */
    public Statistics(final VslStatisticsConfig config) {
        datapoints = new LinkedList<StatisticsDatapoint>();
        this.config = config;
    }

    @Override
    public final VslStatisticsDatapoint begin() {
        final StatisticsDatapoint dp;
        synchronized (datapoints) {
            try {
                while (datapoints.size() >= config.getStatisticsLimitDatapoints()) {
                    datapoints.remove(0);
                    // TODO: probably not the most efficient way
                }
            } catch (final Exception e) {
                LOGGER.error("Exception on removing datapoints from the VslStatistics: {}", e);
                System.out.println("error");
            }
            dp = new StatisticsDatapoint();
            datapoints.add(dp);
        }
        return dp;
    }

    /**
     * Returns all available {@link StatisticsDatapoint}s.
     *
     * @return All available {@link StatisticsDatapoint}s as List.
     */
    public final List<StatisticsDatapoint> getDatapoints() {
        synchronized (datapoints) {
            return Collections.unmodifiableList(datapoints);
        }
    }

    /**
     * This method can be used to hand over a {@link VslStatisticsConfig} that should be used by
     * {@link Statistics} for configuration.
     *
     * @param newConfig
     *            The new {@link VslStatisticsConfig}.
     */
    public final void setConfig(final VslStatisticsConfig newConfig) {
        this.config = newConfig;
    }

    // TODO: add some statistics methods that work on the datapoints list.
}
