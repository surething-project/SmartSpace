package org.ds2os.vsl.core.statistics;

/**
 * Dummy implementation of {@link VslStatisticsProvider} that can be used when no statistics should
 * be stored.
 *
 * @author liebald
 */
public class DummyStatisticsProvider implements VslStatisticsProvider {
    /**
     * Dummy statistics that is handed out to everyone.
     */
    private final DummyStatistics dummy = new DummyStatistics();

    @Override
    public final VslStatistics getStatistics(final Class<?> c, final String purpose) {
        return dummy;
    }

}
