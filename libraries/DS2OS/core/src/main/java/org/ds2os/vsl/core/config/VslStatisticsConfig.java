package org.ds2os.vsl.core.config;

/**
 * Interface for Statistics related configurations.
 *
 * @author liebald
 */
public interface VslStatisticsConfig {

    /**
     * Returns the maximum amount of {@link org.ds2os.vsl.core.statistics.VslStatisticsDatapoint}s a
     * {@link org.ds2os.vsl.core.statistics.VslStatistics} object will store. The oldest one is
     * dropped if to many are stored. The default value is 500.
     *
     * @return The maximum amount of {@link org.ds2os.vsl.core.statistics.VslStatisticsDatapoint}s a
     *         {@link org.ds2os.vsl.core.statistics.VslStatistics} object will store.
     */
    @ConfigDescription(description = "The amount of datapoints each Statistis object"
            + " will store at max before dropping the oldest ones. ", id = "statistics"
                    + ".limitDatapoints", defaultValue = "500", restrictions = ">0")
    int getStatisticsLimitDatapoints();
}
