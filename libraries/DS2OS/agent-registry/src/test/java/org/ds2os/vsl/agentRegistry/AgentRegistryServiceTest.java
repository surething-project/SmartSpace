package org.ds2os.vsl.agentRegistry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.ds2os.vsl.agentregistry.AgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.config.VslAgentRegistryConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author jay
 * @author Johannes Straßer
 * @author liebald
 */
public final class AgentRegistryServiceTest {

    /**
     * VslIdentity of the agent.
     */
    private static final VslIdentity IDENTITY = new ServiceIdentity("system/agentRegistryService",
            "system/agentRegistryService");

    /**
     * A fully initialized AgentRegistryService for the tests to use.
     */
    private AgentRegistryService agentRegistry;

    /**
     * The KOR the agentRegistry uses.
     */
    private KnowledgeRepository kor;

    /**
     * Setting up the test environment.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public void setUp() throws Exception {
        final VslVirtualNodeManager virtualNodeManger = new VirtualNodeManager();
        final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
        final DummyStatisticsProvider dummyStatistics = new DummyStatisticsProvider();
        final VslTypeSearchProvider typeSearchProviderMock = Mockito
                .mock(VslTypeSearchProvider.class);
        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        this.kor = new KnowledgeRepository(virtualNodeManger, subscriptionManager,
                InitialConfigFromFile.getInstance(), dummyStatistics, typeSearchProviderMock,
                nodeFactory);
        kor.activate();
        final VslParametrizedConnector reqBridge = new RequestHandlerToConnectorBridge(this.kor,
                IDENTITY, nodeFactory);
        agentRegistry = new AgentRegistryService(reqBridge, new VslAgentRegistryConfig() {

            @Override
            public String getAgentName() {
                return "agent1";
            }

            @Override
            public long getAgentRegistryStalenessTime() {
                return 60000;
            }

            @Override
            public long getAgentRegistryCleanerInterval() {
                return 60000;
            }
        });
        agentRegistry.activate();
    }

    /**
     * Test the isAgentConnected() method.
     */
    @Test
    public void testIsAgentConnected() {
        agentRegistry.addAgentToConnectedKAList("agent1", new Date(10000L), new Date(30000L));
        assertThat(agentRegistry.isAgentConnected("agent1"), is(true));
        agentRegistry.addAgentToUnconnectedKAList("agent2", new Date(11000L));
        assertThat(agentRegistry.isAgentConnected("agent2"), is(false));
        assertThat(agentRegistry.isAgentConnected("agent3"), is(false));
    }

    /**
     * Test for the isGroupReachable() method. Also test the getLeader() method.
     */
    @Test
    public void testIsGroupReachable() {
        // Initialize environment
        final Date timestamp = new Date(123456L);
        // Group 0 is as it should be, Group1 has one node too many, Group2 has 3 nodes too few;
        final String[] groupID = { "frzj", "fxtz", "onjn" };
        final int[] realSize = { 3, 5, 2 };
        final int[] size = { 3, 4, 5 };

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < realSize[i]; j++) {
                final String name = "agentG" + i + "N" + j;
                agentRegistry.addAgentToUnconnectedKAList(name, timestamp);
                agentRegistry.storeAlivePingToUnConnectedKAs(
                        new AlivePing(name, size[i], "CA ID",
                                Collections.<VslTransportConnector>emptySet(), groupID[i], ""),
                        timestamp);
            }
        }
        // Test isGroupReachable()
        assertThat(agentRegistry.isGroupReachable(groupID[0], size[0]), is(true));
        assertThat(agentRegistry.isGroupReachable(groupID[1], size[1]), is(true));
        assertThat(agentRegistry.isGroupReachable(groupID[2], size[2]), is(false));

        // Test getLeader()
        assertThat(agentRegistry.getLeader(groupID[0]), is("agentG0N2"));
        assertThat(agentRegistry.getLeader(groupID[1]), is("agentG1N4"));
        assertThat(agentRegistry.getLeader(groupID[2]), is("agentG2N1"));
    }

    /**
     * Test the isLeader() method.
     */
    @Test
    public void testIsLeader() {
        final Date certificateExpiration = new Date(2345678L);
        final Date timeStamp = new Date(1234567L);

        assertThat(agentRegistry.isLeader(), is(true));
        agentRegistry.addAgentToConnectedKAList("agent0", timeStamp, certificateExpiration);
        assertThat(agentRegistry.isLeader(), is(true));
        // Our name should be "agent1"
        agentRegistry.addAgentToConnectedKAList("agent2", timeStamp, certificateExpiration);

        assertThat(agentRegistry.isLeader(), is(false));
    }

    /**
     * Test set/getNetworkSize().
     */
    @Test
    public void testNetworkSize() {
        // 1 is default value
        final Date certificateExpiration = new Date(2345678L);
        final Date timeStamp = new Date(1234567L);
        assertThat(agentRegistry.getNetworkSize(), is(1));
        agentRegistry.addAgentToConnectedKAList("agent0", timeStamp, certificateExpiration);
        agentRegistry.addAgentToConnectedKAList("agent2", timeStamp, certificateExpiration);
        assertThat(agentRegistry.getNetworkSize(), is(3));
    }

    /**
     * Test for the addAgentToUnconnectedKAList() method.
     *
     * @throws VslException
     *             Thrown if there are uncaught errors while dealing with the KOR
     */
    @Test
    public void testAddAgentToUnconnectedKAList() throws VslException {
        // Check agentID and time stamp
        final String agentID = "agent2";
        agentRegistry.addAgentToUnconnectedKAList(agentID, new Date(123456L));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/" + agentID + "/agentID",
                        IDENTITY)
                .getValue(), is(agentID));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/" + agentID + "/timestamp",
                        IDENTITY)
                .getValue(), is("123456"));
    }

    /**
     * Test for the addAgentToConnectedKAList() method.
     *
     * @throws VslException
     *             Thrown if there are uncaught errors while dealing with the KOR
     */
    @Test
    public void testAddAgentToConnectedKAList() throws VslException {
        // Initialize variables
        final String agentID = "agent1";
        final String agentID2 = "agent2";
        final Date timeStamp = new Date(123456L);
        final Date certificateExpiration = new Date(2345678L);

        // Test simple add
        agentRegistry.addAgentToConnectedKAList(agentID, timeStamp, certificateExpiration);
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/" + agentID + "/agentID",
                        IDENTITY).getValue(),
                is(agentID));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/connectedKAs/" + agentID + "/timestamp",
                        IDENTITY)
                .getValue(), is("123456"));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/" + agentID
                        + "/certificateExpiration", IDENTITY).getValue(),
                is(certificateExpiration.toString()));

        // Test if agent is removed from unconnected list properly
        agentRegistry.addAgentToUnconnectedKAList(agentID2, timeStamp);
        assertThat(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/" + agentID2 + "/agentID",
                        IDENTITY)
                .getValue(), is(agentID2));
        agentRegistry.addAgentToConnectedKAList(agentID2, timeStamp, certificateExpiration);
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/" + agentID2 + "/agentID",
                        IDENTITY).getValue(),
                is(agentID2));
        try {
            kor.get("/agent1/system/agentRegistryService/unconnectedKAs/" + agentID2 + "/agentID",
                    IDENTITY);
            fail("Agent has not been removed from unconnected list.");
        } catch (final NodeNotExistingException e) {
            // pass
        }

    }

    /**
     * Test for the storeAlivePingToConnectedKAList() method.
     *
     * @throws VslException
     *             Thrown if there are uncaught errors while dealing with the KOR
     */
    @Test
    public void testStoreAlivePingToUnconnectedKAList() throws VslException {
        // Initialize variables
        final HashSet<VslTransportConnector> connectors = new HashSet<VslTransportConnector>();
        VslAlivePing pingFromUnconnected;
        VslNode transports;
        String[] storedURLs;
        final String groupID = "argg";
        final String korHash = "9365";
        final int numKAs = 10;
        connectors.add(new TransportConnector("https://168.192.10.4:5000"));
        connectors.add(new TransportConnector("xmpp://20b3::1:5000"));
        connectors.add(new TransportConnector("coap://www.example.com"));

        // Process ping
        pingFromUnconnected = new AlivePing("agent1", numKAs, "CA Pub Key", connectors, groupID,
                korHash);
        agentRegistry.addAgentToUnconnectedKAList(pingFromUnconnected.getAgentId(),
                new Date(123456L));
        agentRegistry.storeAlivePingToUnConnectedKAs(pingFromUnconnected, new Date(45678L));
        assertThat(kor.get("/agent1/system/agentRegistryService/unconnectedKAs/agent1/timestamp",
                IDENTITY).getValue(), is("45678"));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/agent1/groupID", IDENTITY)
                .getValue(), is(groupID));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/agent1/korHash", IDENTITY)
                .getValue(), is(korHash));
        assertThat(Integer.parseInt(kor
                .get("/agent1/system/agentRegistryService/unconnectedKAs/agent1/numKAs", IDENTITY)
                .getValue()), is(numKAs));

        transports = kor.get(
                "/agent1/system/agentRegistryService/unconnectedKAs/agent1/supportedTransports",
                new AddressParameters().withDepth(-1), IDENTITY);
        storedURLs = transports.getChild("elements").getValue().split(";");
        assertThat(storedURLs.length, is(connectors.size()));

        assertThat(compareConnectors(connectors, agentRegistry.getTransports("agent1")), is(true));
        agentRegistry.storeAlivePingToUnConnectedKAs(pingFromUnconnected, new Date(12345L));
        assertThat(kor.get("/agent1/system/agentRegistryService/unconnectedKAs/agent1/timestamp",
                IDENTITY).getValue(), is("12345"));
    }

    /**
     * Test for the storeAlivePingToConnectedKAList() method.
     *
     * @throws VslException
     *             Thrown if there are uncaught errors while dealing with the KOR
     */
    @Test
    public void testStoreAlivePingToConnectedKAList() throws VslException {
        // Initialize variables
        final HashSet<VslTransportConnector> connectors = new HashSet<VslTransportConnector>();
        VslAlivePing pingFromConnected;
        VslNode transports;
        String[] storedURLs;
        String groupID;
        String korHash;
        int numKAs;

        // Add agents to connected list, process first ping
        groupID = "argg";
        korHash = "9365";
        numKAs = 10;
        connectors.add(new TransportConnector("https://168.192.10.4:5000"));
        connectors.add(new TransportConnector("xmpp://20b3::1:5000"));
        connectors.add(new TransportConnector("coap://www.example.com"));
        pingFromConnected = new AlivePing("agent1", numKAs, "CA Pub Key", connectors, groupID,
                korHash);

        agentRegistry.addAgentToConnectedKAList(pingFromConnected.getAgentId(), new Date(123456L),
                new Date(456789L));
        agentRegistry.storeAlivePingToConnectedKAs(pingFromConnected, new Date(45678L));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/connectedKAs/agent1/timestamp", IDENTITY)
                .getValue(), is("45678"));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/groupID", IDENTITY)
                        .getValue(),
                is(groupID));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/korHash", IDENTITY)
                        .getValue(),
                is(korHash));
        assertThat(Integer.parseInt(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/numKAs", IDENTITY)
                        .getValue()),
                is(numKAs));

        transports = kor.get(
                "/agent1/system/agentRegistryService/connectedKAs/agent1/supportedTransports",
                new AddressParameters().withDepth(-1), IDENTITY);
        storedURLs = transports.getChild("elements").getValue().split(";");
        assertThat(storedURLs.length, is(connectors.size()));
        assertThat(compareConnectors(connectors, agentRegistry.getTransports("agent1")), is(true));
        agentRegistry.storeAlivePingToConnectedKAs(pingFromConnected, new Date(12345L));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/connectedKAs/agent1/timestamp", IDENTITY)
                .getValue(), is("12345"));

        // Send second ping containing more transports than before. The korHash changes.
        korHash = "4566";
        connectors.add(new TransportConnector("multicast://168.192.10.5:5000"));
        connectors.add(new TransportConnector("https://www.example2.com"));
        pingFromConnected = new AlivePing("agent1", numKAs, "CA Pub Key", connectors, groupID,
                korHash);

        agentRegistry.storeAlivePingToConnectedKAs(pingFromConnected, new Date(45678L));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/connectedKAs/agent1/timestamp", IDENTITY)
                .getValue(), is("45678"));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/groupID", IDENTITY)
                        .getValue(),
                is(groupID));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/korHash", IDENTITY)
                        .getValue(),
                is(korHash));
        assertThat(Integer.parseInt(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/numKAs", IDENTITY)
                        .getValue()),
                is(numKAs));

        transports = kor.get(
                "/agent1/system/agentRegistryService/connectedKAs/agent1/supportedTransports",
                new AddressParameters().withDepth(-1), IDENTITY);
        storedURLs = transports.getChild("elements").getValue().split(";");
        assertThat(compareConnectors(connectors, agentRegistry.getTransports("agent1")), is(true));

        // Send second ping containing less transports than before. The korHash, the groupID and the
        // numKAs
        // change.
        korHash = "2346";
        groupID = "ghkj";
        numKAs = 6;
        connectors.clear();
        connectors.add(new TransportConnector("https://168.192.10.4:5000"));
        connectors.add(new TransportConnector("xmpp://20b3::1:5000"));
        connectors.add(new TransportConnector("coap://www.example.com"));
        pingFromConnected = new AlivePing("agent1", numKAs, "CA Pub Key", connectors, groupID,
                korHash);

        agentRegistry.storeAlivePingToConnectedKAs(pingFromConnected, new Date(45678L));
        assertThat(kor
                .get("/agent1/system/agentRegistryService/connectedKAs/agent1/timestamp", IDENTITY)
                .getValue(), is("45678"));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/groupID", IDENTITY)
                        .getValue(),
                is(groupID));
        assertThat(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/korHash", IDENTITY)
                        .getValue(),
                is(korHash));
        assertThat(Integer.parseInt(
                kor.get("/agent1/system/agentRegistryService/connectedKAs/agent1/numKAs", IDENTITY)
                        .getValue()),
                is(numKAs));

        transports = kor.get(
                "/agent1/system/agentRegistryService/connectedKAs/agent1/supportedTransports",
                new AddressParameters().withDepth(-1), IDENTITY);
        storedURLs = transports.getChild("elements").getValue().split(";");
        assertThat(storedURLs.length, is(connectors.size()));
        assertThat(compareConnectors(connectors, agentRegistry.getTransports("agent1")), is(true));
    }

    /**
     * Test for setting, and getting keys and keyStrings.
     */
    @Test
    public void testMulticastGroupKey() {
        // Initialize test keys and hashes
        final String key1 = "södltheörkl";
        final String key2 = "dfkjnrkölge";
        final String hash1 = "sruh";
        final String hash2 = "iuki";
        final String keyString1 = "TLS_PSK_WITH_NULL_SHA384";
        final String keyString2 = "TLS_PSK_WITH_AES_128_CBC_SHA256";

        // Check non initialized state
        assertThat(agentRegistry.getMulticastGroupKeyHash(), is(""));

        // Check adding of the first key
        agentRegistry.setMulticastGroupKey(key1, hash1, keyString1);
        assertThat(agentRegistry.getMulticastGroupKeyHash(), is(hash1));
        assertThat(agentRegistry.getMulticastGroupKey(hash1), is(key1));
        assertThat(agentRegistry.getMulticastGroupKeyString(hash1), is(keyString1));

        // Check adding of the second key and accessibility of the first
        // afterwards
        agentRegistry.setMulticastGroupKey(key2, hash2, keyString2);
        assertThat(agentRegistry.getMulticastGroupKeyHash(), is(hash2));
        assertThat(agentRegistry.getMulticastGroupKey(hash1), is(key1));
        assertThat(agentRegistry.getMulticastGroupKeyString(hash1), is(keyString1));
        assertThat(agentRegistry.getMulticastGroupKey(hash2), is(key2));
        assertThat(agentRegistry.getMulticastGroupKeyString(hash2), is(keyString2));
    }

    /**
     * Tests the getAttribute method.
     */
    @Test
    public void testGetAttribute() {
        agentRegistry.addAgentToConnectedKAList("agent1", new Date(12345L), new Date(23456L));
        agentRegistry.storeAlivePingToConnectedKAs(new AlivePing("agent1", 10, "ca",
                new HashSet<VslTransportConnector>(), "groupID", "hash"), new Date(12345L));
        assertThat(agentRegistry.getAttribute("agent1", "agentID"), is("agent1"));
        assertThat(agentRegistry.getAttribute("agent1", "timestamp"), is("12345"));
        assertThat(agentRegistry.getAttribute("agent1", "certificateExpiration"),
                is(new Date(23456L).toString()));
        assertThat(agentRegistry.getAttribute("agent1", "korHash"), is("hash"));
        assertThat(agentRegistry.getAttribute("agent1", "groupID"), is("groupID"));
        assertThat(agentRegistry.getAttribute("agent1", "numKAs"), is("10"));
        assertThat(agentRegistry.getAttribute("agent1", "notARealAttribute"), is(nullValue()));
    }

    /**
     * Test the cleanStaleAgentsMethod with 0. This should purge all agents.
     */
    @Test
    public void testCleanStaleAgents0() {
        final Date now = new Date(System.currentTimeMillis());

        agentRegistry.cleanStaleAgents(0);

        agentRegistry.addAgentToUnconnectedKAList("KA0", now);
        agentRegistry.addAgentToUnconnectedKAList("KA2", now);
        agentRegistry.addAgentToUnconnectedKAList("KA3", now);
        agentRegistry.addAgentToConnectedKAList("KA0", now, now);

        agentRegistry.cleanStaleAgents(0);

        assertThat(agentRegistry.isAgentConnected("KA0"), is(false));

    }

    /**
     * Test the cleanStaleAgentsMethod with 1000. This should purge a part of the agents.
     */
    @Test
    public void testCleanStaleAgents1() {
        final Date now = new Date(System.currentTimeMillis());
        final Date before = new Date(System.currentTimeMillis() - 20000);

        agentRegistry.addAgentToUnconnectedKAList("KA0", now);
        agentRegistry.addAgentToUnconnectedKAList("KA2", before);
        agentRegistry.addAgentToUnconnectedKAList("KA3", before);
        agentRegistry.addAgentToConnectedKAList("KA0", now, now);
        agentRegistry.addAgentToConnectedKAList("KA2", before, now);
        agentRegistry.addAgentToConnectedKAList("KA3", before, now);

        agentRegistry.cleanStaleAgents(10000);

        assertThat(agentRegistry.isAgentConnected("KA0"), is(true));
        assertThat(agentRegistry.isAgentConnected("KA2"), is(false));
        assertThat(agentRegistry.isAgentConnected("KA3"), is(false));
    }

    /**
     * Compare two VslTransportConnector collections.
     *
     * @param coll1
     *            The first collection
     * @param coll2
     *            The second collection
     * @return True if the contents of the collections are equal, false if not.
     */
    private boolean compareConnectors(final Collection<VslTransportConnector> coll1,
            final Collection<VslTransportConnector> coll2) {
        // Create Set set2 of urls from coll2
        final Set<String> set2 = new HashSet<String>();
        for (final VslTransportConnector entry : coll2) {
            set2.add(entry.getURL());
        }

        // Remove all urls of coll1 from set2
        for (final VslTransportConnector entry : coll2) {
            final String url1 = entry.getURL();
            if (!set2.remove(url1)) {
                return false;
            }
        }
        return set2.isEmpty();
    }

}
