package org.ds2os.vsl.service.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration test class for Config service.
 *
 * @author liebald
 */
public class ITConfigServiceTest {

    /**
     * VslIdentity of the config.
     */
    private static final VslIdentity IDENTITY = new ServiceIdentity("system/config",
            "system/config");

    /**
     * Config service to test.
     */
    private ConfigService config;

    /**
     * Set up test environment.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        final VslVirtualNodeManager virtualNodeManger = new VirtualNodeManager();
        final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
        final DummyStatisticsProvider dummyStatistics = new DummyStatisticsProvider();

        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final KnowledgeRepository kor = new KnowledgeRepository(virtualNodeManger,
                subscriptionManager, InitialConfigFromFile.getInstance(), dummyStatistics,
                Mockito.mock(VslTypeSearchProvider.class), nodeFactory);
        kor.activate();
        config = new ConfigService(new RequestHandlerToConnectorBridge(kor, IDENTITY, nodeFactory),
                InitialConfigFromFile.getInstance());
    }

    /**
     * Test if stored values are returned correctly.
     */
    @Test
    public final void test() {
        assertThat(config.getAgentName(), is(equalTo("agent1")));

        // Test VslMulticastTransportConfig
        assertThat(config.getTLSString(), is("TLS_PSK_WITH_AES_256_CBC_SHA384"));
        assertThat(config.getMaxSenders(), is(100));
        assertThat(config.getMaxAuthorizedBufferSize(), is(50000000));
        assertThat(config.getMaxUnauthorizedBufferSize(), is(10000));
        assertThat(config.getBufferStaleInterval(), is(10000L));
    }

}
