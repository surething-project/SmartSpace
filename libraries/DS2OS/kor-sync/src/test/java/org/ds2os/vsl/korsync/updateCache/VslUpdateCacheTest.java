package org.ds2os.vsl.korsync.updateCache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.config.VslKORSyncConfig;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.junit.Before;
import org.junit.Test;

/**
 * Testclass for {@link KorUpdateCache}.
 *
 * @author liebald
 *
 */
public class VslUpdateCacheTest {

    /**
     * Unit under test.
     */
    KorUpdateCache updateCache;

    /**
     * config mock.
     */
    VslKORSyncConfig configMock;

    /**
     * Setup.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        configMock = mock(VslKORSyncConfig.class);
        when(configMock.getMaxKORUpdateCacheTime()).thenReturn(60000L);
        updateCache = new KorUpdateCache(configMock);
        updateCache.activate();
    }

    /**
     * Test adding new stuff to the cache.
     */
    @Test
    public final void testAddGet() {
        final VslKORUpdate update1 = new KORUpdate("123", "456", null, null, "agent1");
        final VslKORUpdate update2 = new KORUpdate("456", "789", null, null, "agent2");

        updateCache.add(update1);
        updateCache.add(update2);

        assertThat(updateCache.getUpdate("agent1", "123"), is(equalTo(update1)));
        assertThat(updateCache.getUpdate("agent2", "456"), is(equalTo(update2)));

    }

    /**
     * Test adding new stuff to the cache.
     */
    @Test
    public final void testValidityTimeout() {
        final VslKORUpdate update1 = new KORUpdate("123", "456", null, null, "agent1");
        final VslKORUpdate update2 = new KORUpdate("456", "789", null, null, "agent2");

        updateCache.add(update1);
        updateCache.add(update2);

        assertThat(updateCache.getUpdate("agent1", "123"), is(equalTo(update1)));
        assertThat(updateCache.getUpdate("agent2", "456"), is(equalTo(update2)));

    }

    /**
     * Test if non existing updates are stored correctly.
     */
    @Test
    public final void testNoUpdate() {
        when(configMock.getMaxKORUpdateCacheTime()).thenReturn(500L);
        final VslKORUpdate update1 = new KORUpdate("123", "456", null, null, "agent1");
        updateCache.add(update1);
        // after a second the update should be removed from the cache.
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            System.out.println(e.getMessage());
        }
        assertThat(updateCache.getUpdate("agent1", "123"), is(equalTo(null)));

    }

}
