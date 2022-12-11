package org.ds2os.vsl.slmr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.config.VslModelRepositoryConfig;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.junit.Before;
import org.junit.Test;

/**
 * Testclass for the SLMR.
 *
 * @author liebald
 */
public class SlmrHandlerTest {

    /**
     * unit under test.
     */
    SlmrHandler handler;

    /**
     * Config Mock for the slmr test.
     */
    VslModelRepositoryConfig configMock;

    /**
     * Mock of the VslConnector.
     */
    VslConnector conMock;

    /**
     * Setup the models, store them in the cache before the tests begin.
     */
    @Before
    public final void setUp() {
        configMock = mock(VslModelRepositoryConfig.class);
        when(configMock.getCMRurl())
                .thenReturn("https://gitlab.dev.ds2os.org/ds2os-devs/vsl_models/raw/master/models");
        when(configMock.getCharset()).thenReturn("UTF-8");
        handler = new SlmrHandler("/agent1/SLMR", configMock, conMock);

    }

    /**
     * Tests the loading of models from the CMR.
     *
     * @throws ModelNotFoundException
     *             exception
     */
    @Test
    public void testTryLoadFromCMR() throws ModelNotFoundException {
        System.out.println(handler.tryLoadFromCMR("/basic/text"));
    }

    // /**
    // * Tests the loading of models from the CMR.
    // *
    // * @throws VslException
    // * exception
    // */
    // @Test
    // public void testGet() throws VslException {
    // System.out.println(handler.get("/agent1/SLMR/basic/text", mock(VslIdentity.class)));
    // }
}
