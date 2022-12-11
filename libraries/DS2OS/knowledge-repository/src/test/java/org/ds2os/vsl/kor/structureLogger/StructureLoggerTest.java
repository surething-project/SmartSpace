package org.ds2os.vsl.kor.structureLogger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

/**
 * @author liebald
 *
 */
public class StructureLoggerTest {

    /**
     * The initial Hash the StructureLogger works with.
     */
    private final String initHash = "1234567890";

    /**
     * The logger used for the tests.
     */
    private StructureLogger log;

    /**
     * Setup the logger.
     */
    @Before
    public final void setUp() {
        log = new StructureLogger("localKA");
        log.activate(initHash);
    }

    /**
     * Test method for {@link StructureLogger#getChangeLogSincehash(String)} .
     */
    @Test
    public final void testGetChangeLogSincehash() {
        final String hash1 = "1111111111";
        final String hash2 = "2222222222";
        log.logChangedAddress("/1/2/3");
        log.logChangedAddress("/1/2/4");
        log.newLogpointHash(hash1);
        log.logChangedAddress("/1/2/5");
        log.newLogpointHash(hash2);

        final String logSinceInit = log.getChangeLogSincehash(initHash).toString();
        final String logSincehash1 = log.getChangeLogSincehash(hash1).toString();
        final String logSincehash2 = log.getChangeLogSincehash(hash2).toString();

        assertThat(logSinceInit, is(equalTo("[/1/2/3, /1/2/4, /1/2/5]")));
        assertThat(logSincehash1, is(equalTo("[/1/2/5]")));
        assertThat(logSincehash2, is(equalTo("[]")));
    }

    /**
     * Test method for {@link StructureLogger#getChangeLogSincehash(String)} .
     */
    @Test
    public final void testGetChangeLogSincehashOptimizeAddresses() {
        final String hash1 = "1111111111";
        final String hash2 = "2222222222";

        log.logChangedAddress("/1/2");
        log.logChangedAddress("/1/2/4");
        log.logChangedAddress("/1/2/5");
        log.logChangedAddress("/1/3/5");
        log.logChangedAddress("/1/3/7");
        log.logChangedAddress("/1/3");
        log.newLogpointHash(hash1);
        log.logChangedAddress("/1/3/6");
        log.logChangedAddress("/1/32");
        log.logChangedAddress("/1/4");
        log.newLogpointHash(hash2);

        assertThat(log.getChangeLogSincehash(initHash).toString(),
                is(equalTo("[/1/2, /1/3, /1/32, /1/4]")));
    }

    /**
     * Test method for {@link StructureLogger#getCurrentLogHash()} .
     */
    @Test
    public final void testGetCurrentLogHash() {
        assertThat(log.getCurrentLogHash(), is(equalTo(initHash)));
    }

    /**
     * Test method for {@link StructureLogger#logChangedAddress(String)} .
     */
    @Test
    public final void testLogChangedAddress() {
        final String hash1 = "1111111111";
        log.logChangedAddress("/1/2/3");
        log.newLogpointHash(hash1);
        assertThat(log.getChangeLogSincehash(initHash).toString(), is(equalTo("[/1/2/3]")));
    }

    /**
     * Test method for {@link StructureLogger#newLogpointHash(String)}.
     */
    @Test
    public final void testNewLogpointHash() {
        final String hash1 = "1111111111";
        log.newLogpointHash(hash1);

        assertThat(log.getCurrentLogHash(), is(equalTo(hash1)));
    }

}
