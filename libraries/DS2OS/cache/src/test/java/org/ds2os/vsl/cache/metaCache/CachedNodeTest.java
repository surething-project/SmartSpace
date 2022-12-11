package org.ds2os.vsl.cache.metaCache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Test class for the {@link CachedNode} class.
 *
 * @author liebald
 */
public class CachedNodeTest {

    /**
     * Sleep for the given amount of time.
     *
     * @param time
     *            The time to sleep.
     */
    private void delay(final long time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException e) {
        }
    }

    /**
     * Test the {@link CachedNode#getAmountAccessed()} method.
     */
    @Test
    public final void testGetAmountAccessed() {
        final CachedNode unitUnderTest = new CachedNode(3, 10, 10);
        assertThat(unitUnderTest.getAmountAccessed(), is(equalTo(0)));
        unitUnderTest.nodeAccessed();
        assertThat(unitUnderTest.getAmountAccessed(), is(equalTo(1)));
        unitUnderTest.nodeAccessed();
        assertThat(unitUnderTest.getAmountAccessed(), is(equalTo(2)));
    }

    /**
     * Test the {@link CachedNode#getInitialCacheTimestamp()} method.
     */
    @Test
    public final void testGetInitialCacheTimestamp() {
        final long initTime = System.currentTimeMillis();
        final CachedNode unitUnderTest = new CachedNode(3, 10, 10);
        assertThat(unitUnderTest.getLastAccessed() - initTime < 30, is(equalTo(true)));
    }

    /**
     * Test the {@link CachedNode#getLastAccessed()} method.
     */
    @Test
    public final void testGetLastAccessed() {
        final long initTime = System.currentTimeMillis();
        final CachedNode unitUnderTest = new CachedNode(3, 10, 10);
        assertThat(unitUnderTest.getLastAccessed() - initTime < 30, is(equalTo(true)));
    }

    /**
     * Test the {@link CachedNode#getNodeVersion()} method.
     */
    @Test
    public final void testGetNodeVersion() {
        final CachedNode unitUnderTest = new CachedNode(3, 1, 10);
        assertThat(unitUnderTest.getNodeVersion(), is(equalTo(3L)));
    }

    /**
     * Test the {@link CachedNode#isExpired()} method.
     */
    @Test
    public final void testIsExpired() {
        final CachedNode unitUnderTest = new CachedNode(3, 1, 10);
        assertThat(unitUnderTest.isExpired(), is(equalTo(false)));
        delay(1200);
        assertThat(unitUnderTest.isExpired(), is(equalTo(true)));
    }

    /**
     * Test the {@link CachedNode#nodeAccessed()} method.
     */
    @Test
    public final void testNodeAccessed() {
        final long initTime = System.currentTimeMillis();
        final CachedNode unitUnderTest = new CachedNode(3, 10, 10);

        delay(200);
        long lastAccessedTime = System.currentTimeMillis();
        unitUnderTest.nodeAccessed();
        assertThat(unitUnderTest.getAmountAccessed(), is(equalTo(1)));
        assertThat(unitUnderTest.getNodeVersion(), is(equalTo(3L)));
        assertThat(unitUnderTest.getInitialCacheTimestamp() - initTime < 30, is(equalTo(true)));
        assertThat(unitUnderTest.getLastAccessed() - lastAccessedTime < 30, is(equalTo(true)));
        assertThat(unitUnderTest.getLastAccessed() - initTime < 30, is(equalTo(false)));

        delay(200);
        lastAccessedTime = System.currentTimeMillis();
        unitUnderTest.nodeAccessed();
        assertThat(unitUnderTest.getAmountAccessed(), is(equalTo(2)));
        assertThat(unitUnderTest.getNodeVersion(), is(equalTo(3L)));
        assertThat(unitUnderTest.getInitialCacheTimestamp() - initTime < 30, is(equalTo(true)));
        assertThat(unitUnderTest.getLastAccessed() - lastAccessedTime < 30, is(equalTo(true)));
        assertThat(unitUnderTest.getLastAccessed() - initTime < 30, is(equalTo(false)));

    }

}
