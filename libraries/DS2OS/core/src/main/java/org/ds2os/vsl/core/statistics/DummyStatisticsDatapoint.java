package org.ds2os.vsl.core.statistics;

/**
 * Dummy implementation of {@link VslStatisticsDatapoint} that can be used when no statistics should
 * be stored.
 *
 * @author liebald
 */
public class DummyStatisticsDatapoint implements VslStatisticsDatapoint {

    @Override
    public final void end() {

    }

}
