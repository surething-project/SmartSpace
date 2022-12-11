package org.ds2os.vsl.rest.jetty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.StaticConfig;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.HttpHandler;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.jetty.server.JettyServer;
import org.junit.Test;

/**
 * Module tests for the Jetty server instantiation.
 *
 * @author felix
 */
public final class JettyServerTest {

    /**
     * Test starting a Jetty server.
     *
     * @throws IOException
     *             should not happen.
     * @throws GeneralSecurityException
     *             If the JVM's SSL capabilities are insufficient.
     * @throws InterruptedException
     *             If the current thread is interrupted.
     */
    @Test
    public void testStart() throws IOException, GeneralSecurityException, InterruptedException {
        // Settings for SSL - TODO: dummy
        final String keystorePath = "agent1.jks";
        final String keystorePassword = "K3yst0r3";

        final RestTransportContext context = new RestTransportContext(
                StaticConfig.DEFAULT_REST_CONFIG,
                SSLUtils.loadKeyStore(keystorePath, keystorePassword), keystorePassword,
                new DatabindMapperFactory(new VslNodeFactoryImpl()));
        final JettyServer toTest = new JettyServer(context, mock(HttpHandler.class));

        toTest.addHttpsConnector(Arrays.asList(InetAddress.getLocalHost()), 23456);
        toTest.start();
        toTest.stop();
        toTest.join();

        assertThat(toTest, is(not(nullValue())));
    }
}
