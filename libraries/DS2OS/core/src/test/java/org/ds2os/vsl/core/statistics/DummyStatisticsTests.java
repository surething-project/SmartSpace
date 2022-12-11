package org.ds2os.vsl.core.statistics;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

/**
 * Testclass for dummy statistics.
 *
 * @author liebald
 */
public class DummyStatisticsTests {

    /**
     * The tested class.
     */
    private DummyStatisticsProvider unitUnderTest;

    /**
     * Setup.
     */
    @Before
    public final void setUp() {
        unitUnderTest = new DummyStatisticsProvider();
    }

    /**
     * Testcase.
     */
    @Test
    public final void testDummyStatisticsProvider() {
        final VslStatistics d1 = unitUnderTest.getStatistics(String.class, "a");
        final VslStatistics d2 = unitUnderTest.getStatistics(Integer.class, "b");
        final VslStatisticsDatapoint dp1 = d1.begin();
        final VslStatisticsDatapoint dp2 = d2.begin();
        assertThat(d1.equals(d2), is(equalTo(true)));
        assertThat(dp1.equals(dp2), is(equalTo(true)));
        assertThat(d1 instanceof DummyStatistics, is(equalTo(true)));
        assertThat(dp1 instanceof DummyStatisticsDatapoint, is(equalTo(true)));
        dp1.end();
        assertThat(dp1.equals(dp2), is(equalTo(true)));
    }
}
