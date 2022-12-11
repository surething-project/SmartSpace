package org.ds2os.vsl.multicasttransport;

import static org.ds2os.vsl.netutils.TestHelper.randomData;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.agentregistry.AgentRegistryService;
import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslAlivePingHandler;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.config.VslAgentRegistryConfig;
import org.ds2os.vsl.core.config.VslMulticastTransportConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.node.VslStructureNodeData;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;

/**
 * Test class for MulticastTransport.
 *
 * All tests save the dummy test are commented out because the jenkins server does not have an
 * accessible loopback device and therefore the tests cannot succeed.
 *
 * @author Johannes Straßer
 */
public class MulticastTransportTest {

    /**
     * VslIdentity of the agent.
     */
    private static final VslIdentity IDENTITY = new ServiceIdentity("system/agentRegistryService",
            "system/agentRegistryService");

    /**
     * A MulticastTransport for tests to use.
     */
    private MulticastTransport multicastTransport;

    /**
     * A VslMapper for tests to use.
     */
    private VslMapper mapper;

    /**
     * A VslAlivePingHandler for tests to use.
     */
    private VslAlivePingHandler pingHandler;

    /**
     * A VslKORSyncHandler for tests to use.
     */
    private VslKORSyncHandler korSyncHandler;

    /**
     * Sets up test environment.
     *
     * @throws java.lang.Exception
     *             Exception thrown by various methods
     */
    @Before
    public final void setUp() throws Exception {
        mapper = new DatabindMapperFactory(new VslNodeFactoryImpl()).getMapper(VslMimeTypes.JSON);
        pingHandler = Mockito.mock(VslAlivePingHandler.class);
        korSyncHandler = Mockito.mock(VslKORSyncHandler.class);
        final VslVirtualNodeManager virtualNodeManger = new VirtualNodeManager();
        final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
        final DummyStatisticsProvider dummyStatistics = new DummyStatisticsProvider();
        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final KnowledgeRepository kor = new KnowledgeRepository(virtualNodeManger,
                subscriptionManager, InitialConfigFromFile.getInstance(), dummyStatistics,
                Mockito.mock(VslTypeSearchProvider.class), nodeFactory);
        final RequestHandlerToConnectorBridge reqBridge = new RequestHandlerToConnectorBridge(kor,
                IDENTITY, nodeFactory);
        final AgentRegistryService registry = new AgentRegistryService(reqBridge,
                new VslAgentRegistryConfig() {

                    @Override
                    public String getAgentName() {
                        return "localKA";
                    }

                    @Override
                    public long getAgentRegistryStalenessTime() {
                        return 0;
                    }

                    @Override
                    public long getAgentRegistryCleanerInterval() {
                        return 0;
                    }

                });
        kor.activate();
        registry.activate();
        final VslMulticastTransportConfig config = Mockito.mock(VslMulticastTransportConfig.class);
        when(config.getAgentName()).thenReturn("localKA");
        when(config.getMaxAuthorizedBufferSize()).thenReturn(50000);
        when(config.getMaxUnauthorizedBufferSize()).thenReturn(20000);
        when(config.getMaxSenders()).thenReturn(20);
        when(config.getTLSString()).thenReturn("TLS_PSK_WITH_AES_256_CBC_SHA384");
        final VslX509Authenticator certificateAuthority = Mockito.mock(VslX509Authenticator.class);
        when(certificateAuthority.getCAPublicKey()).thenReturn("111111");
        multicastTransport = new MulticastTransport(12345, null, pingHandler, mapper, registry,
                korSyncHandler, config, certificateAuthority);
        multicastTransport.activate();
    }

    /**
     * Tears down test environment.
     *
     */
    @After
    public final void tearDown() {
        multicastTransport.shutdown();
    }

    /**
     * Dummy test. Fixes error when all other tests are on @Ignore.
     */
    @Test
    public final void testDummy() {
        return;
    }

    /**
     * Tests activate(). Models the case there is already a key saved to the agentRegistry. The case
     * that there is no key is tested in setUp.
     *
     * @throws Exception
     *             Thrown if a general error occurs
     */
    @Test
    public final void testActivate() throws Exception {
        final String hash = "deadbeef";
        final String tlsString = "TLS_PSK_WITH_AES_128_CBC_SHA";
        final byte[] key = new KeyStringParser(tlsString).createKey();
        final VslAgentRegistryService registry = Mockito.mock(VslAgentRegistryService.class);
        final VslSymmetricKeyStore keyStore = Mockito.mock(VslSymmetricKeyStore.class);
        when(keyStore.addKey(Matchers.<byte[]>any(), Matchers.anyString())).thenReturn(true);
        when(registry.getKeyStore()).thenReturn(keyStore);
        when(registry.getMulticastGroupKeyHash()).thenReturn(hash);
        when(registry.getMulticastGroupKey(hash)).thenReturn(DatatypeConverter.printHexBinary(key));
        when(registry.getMulticastGroupKeyString(hash)).thenReturn(tlsString);
        final VslMulticastTransportConfig config = Mockito.mock(VslMulticastTransportConfig.class);
        when(config.getAgentName()).thenReturn("localKA");
        when(config.getMaxAuthorizedBufferSize()).thenReturn(50000);
        when(config.getMaxUnauthorizedBufferSize()).thenReturn(20000);
        when(config.getMaxSenders()).thenReturn(20);
        final VslX509Authenticator certificateAuthority = Mockito.mock(VslX509Authenticator.class);
        when(certificateAuthority.getCAPublicKey()).thenReturn("111111");
        final MulticastTransport transport = new MulticastTransport(12345, null, pingHandler,
                mapper, registry, korSyncHandler, config, certificateAuthority);
        transport.activate();
        transport.shutdown();
    }

    /**
     * Tests the sendAlivePingMethod using a small non fragmented alive ping. This test may fail due
     * to threading inconsistencies when more than one loopback device is present. The threading
     * will be replaced in the future.
     */
    @Ignore
    @Test
    public final void testSendAlivePing0() {
        assumeTrue(hasLoopback());
        final VslAlivePing alivePing = new AlivePing("11111", 5, "222222",
                multicastTransport.getConnectors(), "srgd", "");
        final ArgumentCaptor<VslAlivePing> alivePingCaptor = ArgumentCaptor
                .forClass(VslAlivePing.class);
        final ArgumentCaptor<Boolean> isAutheticatedCaptor = ArgumentCaptor.forClass(Boolean.class);
        multicastTransport.sendAlivePing(alivePing);
        verify(pingHandler, Mockito.timeout(500)).handleAlivePing(alivePingCaptor.capture(),
                isAutheticatedCaptor.capture());
        assertThat(alivePingCaptor.getValue().getAgentId(), is(alivePing.getAgentId()));
        assertThat(alivePingCaptor.getValue().getCaPub(), is(alivePing.getCaPub()));
        assertThat(alivePingCaptor.getValue().getNumKAs(), is(alivePing.getNumKAs()));
        assertThat(alivePingCaptor.getValue().getGroupID(), is(alivePing.getGroupID()));
        assertThat(alivePingCaptor.getValue().getKorHash(), is(alivePing.getKorHash()));
        assertThat(isAutheticatedCaptor.getValue(), is(true));
    }

    /**
     * Tests the sendAlivePingMethod using a big fragmented alive ping.
     */
    @Ignore
    @Test
    public final void testSendAlivePing1() {
        assumeTrue(hasLoopback());
        final VslAlivePing alivePing = new AlivePing(
                DatatypeConverter.printHexBinary(randomData(3000)), 30, "222222",
                multicastTransport.getConnectors(), "srgd", "");
        final ArgumentCaptor<VslAlivePing> alivePingCaptor = ArgumentCaptor
                .forClass(VslAlivePing.class);
        final ArgumentCaptor<Boolean> isAutheticatedCaptor = ArgumentCaptor.forClass(Boolean.class);
        multicastTransport.sendAlivePing(alivePing);
        verify(pingHandler, Mockito.timeout(500)).handleAlivePing(alivePingCaptor.capture(),
                isAutheticatedCaptor.capture());
        assertThat(alivePingCaptor.getValue().getAgentId(), is(alivePing.getAgentId()));
        assertThat(alivePingCaptor.getValue().getCaPub(), is(alivePing.getCaPub()));
        assertThat(alivePingCaptor.getValue().getNumKAs(), is(alivePing.getNumKAs()));
        assertThat(alivePingCaptor.getValue().getGroupID(), is(alivePing.getGroupID()));
        assertThat(alivePingCaptor.getValue().getKorHash(), is(alivePing.getKorHash()));
        assertThat(isAutheticatedCaptor.getValue(), is(true));
    }

    /**
     * Tests the sendAlivePingMethod using a too big fragmented alive ping.
     */
    @Ignore
    @Test
    public final void testSendAlivePing2() {
        assumeTrue(hasLoopback());
        final VslAlivePing alivePing = new AlivePing(
                DatatypeConverter.printHexBinary(randomData(30000)), 30, "222222",
                multicastTransport.getConnectors(), "srgd", "");
        final ArgumentCaptor<VslAlivePing> alivePingCaptor = ArgumentCaptor
                .forClass(VslAlivePing.class);
        final ArgumentCaptor<Boolean> isAutheticatedCaptor = ArgumentCaptor.forClass(Boolean.class);
        multicastTransport.sendAlivePing(alivePing);
        verify(pingHandler, never()).handleAlivePing(alivePingCaptor.capture(),
                isAutheticatedCaptor.capture());
    }

    /**
     * Tests the sendKORUpdate method.
     */
    @Ignore
    @Test
    public final void testSendKORUpdate() {
        assumeTrue(hasLoopback());
        /*
         * Create Data
         */
        List<String> newTypes = new ArrayList<String>();
        newTypes.add("/basic/text");
        List<String> newReaderIDs = new ArrayList<String>();
        newReaderIDs.add("group1");
        newReaderIDs.add("group2");
        List<String> newWriterIDs = new ArrayList<String>();
        newWriterIDs.add("group1");
        final VslStructureNodeImpl addedNode1 = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                "restriction1", newTypes, "");
        newTypes = new ArrayList<String>();
        newTypes.add("/basic/text");
        newTypes.add("/derived/boolean");
        newReaderIDs = new ArrayList<String>();
        newReaderIDs.add("group1");
        newReaderIDs.add("group2");
        newReaderIDs.add("group3");
        newWriterIDs = new ArrayList<String>();
        newWriterIDs.add("group1");
        newWriterIDs.add("group2");
        newWriterIDs.add("group3");
        final VslStructureNodeImpl addedNode2 = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                "restriction2", newTypes, "");
        final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
        addedNodes.put("Node1", addedNode1);
        addedNodes.put("Node2", addedNode2);
        final Set<String> removedNodes = new HashSet<String>();
        removedNodes.add("Node3");
        removedNodes.add("Node4");
        removedNodes.add("Node5");
        /*
         * Send update
         */
        multicastTransport.sendKORUpdate(
                new KORUpdate("stjzr6", "trshjj", addedNodes, removedNodes, "localKA"));
        final ArgumentCaptor<VslKORUpdate> korUpdateCaptor = ArgumentCaptor
                .forClass(VslKORUpdate.class);
        verify(korSyncHandler, Mockito.timeout(500)).handleKORUpdate(korUpdateCaptor.capture());

        /*
         * Compare results with data
         */
        final VslKORUpdate korUpdate = korUpdateCaptor.getValue();
        assertThat(korUpdate.getHashFrom(), is("stjzr6"));
        assertThat(korUpdate.getHashTo(), is("trshjj"));

        // Check added node 1
        final VslStructureNodeData node1 = korUpdate.getAddedNodes().get("Node1");

        assertThat(node1.getReaderIds().remove("group1"), is(true));
        assertThat(node1.getReaderIds().remove("group2"), is(true));
        assertThat(node1.getReaderIds().isEmpty(), is(true));
        assertThat(node1.getWriterIds().remove("group1"), is(true));
        assertThat(node1.getWriterIds().isEmpty(), is(true));
        assertThat(node1.getRestrictions(), is("restriction1"));
        assertThat(node1.getTypes().remove("/basic/text"), is(true));
        assertThat(node1.getTypes().isEmpty(), is(true));

        // Check added node 2
        final VslStructureNodeData node2 = korUpdate.getAddedNodes().get("Node2");
        assertThat(node2.getReaderIds().remove("group1"), is(true));
        assertThat(node2.getReaderIds().remove("group2"), is(true));
        assertThat(node2.getReaderIds().remove("group3"), is(true));
        assertThat(node2.getReaderIds().isEmpty(), is(true));
        assertThat(node2.getWriterIds().remove("group1"), is(true));
        assertThat(node2.getWriterIds().remove("group2"), is(true));
        assertThat(node2.getWriterIds().remove("group3"), is(true));
        assertThat(node2.getWriterIds().isEmpty(), is(true));
        assertThat(node2.getRestrictions(), is("restriction2"));
        assertThat(node2.getTypes().remove("/basic/text"), is(true));
        assertThat(node2.getTypes().remove("/derived/boolean"), is(true));
        assertThat(node2.getTypes().isEmpty(), is(true));

        // Check remaining fields
        assertThat(korUpdate.getRemovedNodes().remove("Node3"), is(true));
        assertThat(korUpdate.getRemovedNodes().remove("Node4"), is(true));
        assertThat(korUpdate.getRemovedNodes().remove("Node5"), is(true));
        assertThat(korUpdate.getRemovedNodes().isEmpty(), is(true));
        assertThat(korUpdate.getAgentName(), is("localKA"));

    }

    /**
     * Check if the current machine has an accessible loopback device.
     *
     * @return True if the current machine has at least one loopback device, false if not
     */
    private boolean hasLoopback() {
        // TODO This may return true on Jenkins despite the lack off usable
        // loopback devices
        try {
            for (final NetworkInterface netIf : Collections
                    .list(NetworkInterface.getNetworkInterfaces())) {
                if (netIf.isUp() && netIf.isLoopback()) {
                    System.out.println("Found loopback device: " + netIf.getDisplayName());
                    return true;
                }
            }
        } catch (final SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

}
