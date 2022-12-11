package org.ds2os.vsl.statistics;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.ds2os.vsl.core.config.VslStatisticsConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class StatisticsTest {

    private Statistics stats;

    @Before
    public void setUp() throws Exception {
        final VslStatisticsConfig config = Mockito.mock(VslStatisticsConfig.class);
        when(config.getStatisticsLimitDatapoints()).thenReturn(500);
        stats = new Statistics(config);
    }

    @Test
    public void testLimit() {
        for (int i = 0; i < 5000; i++) {
            stats.begin().end();
        }

        assertThat(stats.getDatapoints().size(), is(equalTo(500)));
    }

}
