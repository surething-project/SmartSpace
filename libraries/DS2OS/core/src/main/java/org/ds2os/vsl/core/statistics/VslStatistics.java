package org.ds2os.vsl.core.statistics;

/**
 * Interface for modules to generate statistics.
 *
 * @author liebald
 */
public interface VslStatistics {

    /**
     * Creates a new datapoint and sets its starting point. The {@link VslStatisticsDatapoint} is
     * also added to the list of all Datapointsmanaged by this {@link VslStatistics}.
     *
     * @return new {@link VslStatisticsDatapoint}
     */
    VslStatisticsDatapoint begin();
}
