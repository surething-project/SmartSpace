package org.ds2os.vsl.core.statistics;

/**
 * Dummy implementation of {@link VslStatistics} that can be used when no statistics should be
 * stored.
 *
 * @author liebald
 */
public class DummyStatistics implements VslStatistics {
    /**
     * Dummy statisticsDatapoint that is handed out to everyone.
     */
    private final DummyStatisticsDatapoint dummy = new DummyStatisticsDatapoint();

    @Override
    public final VslStatisticsDatapoint begin() {
        return dummy;
    }

}
