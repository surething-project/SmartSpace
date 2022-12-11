package org.ds2os.vsl.multicasttransport;

import java.net.SocketException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import org.ds2os.vsl.core.config.VslTransportConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the Broadcaster. Note that these tests have no fail conditions as the results are
 * heavily dependent of the physical environment of the tests. These tests outputs must be verified
 * manually.
 *
 * @author Johannes Straßer
 *
 */
public class BroadcasterTest {

    /**
     * A fully initialized Broadcaster for the tests to use.
     */
    private Broadcaster brd;

    /**
     * Sets up the test environment.
     *
     * @throws SocketException
     *             Thrown if there are errors during socket initialization
     */
    @Before
    public final void setUp() throws SocketException {
        brd = new Broadcaster(12345, null);
        brd.initializeInterfaces(new VslTransportConfig() {
            @Override
            public Set<String> getUsableInterfaces() {
                return Collections.singleton("*");
            }

            @Override
            public int getCallbackTimeout() {
                return 1;
            }

            @Override
            public boolean isLoopbackAllowed() {
                return false;
            }
        });
    }

    /**
     * Tests the getSourceAdresses method. Note that this test has no fail conditions as the results
     * are heavily dependent of the physical environment of the test. This tests output must be
     * verified manually.
     */
    @Test
    public final void testGetSourceAdresses() {
        for (final Entry<String, Integer> entry : brd.getSourceAdresses().entrySet()) {
            System.out.println(
                    "Source address: " + entry.getKey() + " with MTU: " + entry.getValue());
        }
    }

    /**
     * Tests the getURLs method. Note that this test has no fail conditions as the results are
     * heavily dependent of the physical environment of the test. This tests output must be verified
     * manually.
     */
    @Test
    public final void testGetURLs() {
        for (final String entry : brd.getURLs()) {
            System.out.println("URL: " + entry);
        }
    }

    /**
     * Tears down the test environment.
     */
    @After
    public final void tearDown() {
        brd.shutdown();
    }

}
