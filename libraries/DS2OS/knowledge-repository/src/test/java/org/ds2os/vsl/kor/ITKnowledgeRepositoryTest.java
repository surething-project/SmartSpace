package org.ds2os.vsl.kor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.config.VslInitialConfig;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.NodeLockedException;
import org.ds2os.vsl.exception.SubscriptionNotSupportedException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 * Integration tests for the KOR.
 *
 * @author liebald
 */
public class ITKnowledgeRepositoryTest {

    /**
     * Alternative VslIdentity used for tests.
     */
    private VslIdentity alternativeVslID;

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * The Kor to work with.
     */
    private KnowledgeRepository kor;

    /**
     * The root address of the local KA.
     */
    private String localKorAddress;

    /**
     * Name of the service used for tests.
     */
    private String service;

    /**
     * ID of the service used for tests.
     */
    private String serviceID;

    /**
     * VslIdentity of the service used for tests.
     */
    private VslIdentity serviceVslID;

    /**
     * The Id of the model used for the tests. Make sure it is in the modelcache directory!
     */
    private String testModelId;

    /**
     * The root address of the servicemodel/tree used for tests.
     */
    private String testServiceAddress;

    /**
     * The {@link VslNodeFactory} for creating VslNodes.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    // Structure of the testModel (nodename -- type -- initialValue):
    // .service -- /basic/composed --
    // ..node1 -- /basic/composed --
    // ...bool -- /derived/boolean -- 1
    // ...submodel -- /basic/composed --
    // ....text1 -- /basic/text --
    // ....text2 -- /basic/text -- text2
    // ....number -- /basic/number --
    // ..node2 -- /basic/composed --
    // ...text -- /basic/text -- TestText
    // ...number -- /basic/number -- 42
    // ..listNode1 -- /basic/list --
    // ..listNode2 -- /basic/list --
    // ...abc -- /basic/composed --
    // ....text1 -- /basic/text --
    // ....text2 -- /basic/text -- text2
    // ....number -- /basic/number --
    // ...jkl same as abc
    // ...ghi same as abc
    // ...def same as abc

    // Model1:
    // <model type="/basic/composed">
    // <node1 type="/basic/composed">
    // <bool type="/derived/boolean">1</bool>
    // <submodel type="/test/testModel2"/>
    // </node1>
    // <node2 type="/basic/composed">
    // <text type="/basic/text">TestText</text>
    // <number type="/basic/number">42</number>
    // </node2>
    // <listNode1 type="/basic/list"/>
    // <listNode2 type="/basic/list" restriction="allowedTypes='/test/testModel2'">
    // <elements type="/basic/text">abcdefg</elements>
    // <abc type="/test/testModel2"/>
    // <jkl type="/test/testModel2"/>
    // <ghi type="/test/testModel2"/>
    // <def type="/test/testModel2"/>
    // </listNode2>
    // </model>
    //
    // Model2:
    // <model type="/basic/composed">
    // <text1 type="/basic/text"/>
    // <text2 type="/basic/text">text2</text2>
    // <number type="/basic/number"/>
    // </model>

    /**
     * Setup for the test.
     *
     * @throws Exception
     *             shouldn't happen
     */
    @Before
    public final void setUp() throws Exception {

        final VslInitialConfig configMock = mock(VslInitialConfig.class);
        when(configMock.getBooleanProperty("kor.db.persist", false)).thenReturn(false);
        when(configMock.getProperty("kor.db.location", "hsqldb/db")).thenReturn("hsqldb/db");
        when(configMock.getProperty("kor.db.username", "admin")).thenReturn("ds2os");
        when(configMock.getProperty("kor.db.password", "password")).thenReturn("thisMustBeSecure");
        when(configMock.getBooleanProperty("kor.archive", false)).thenReturn(true);
        when(configMock.getProperty("kor.archive", "0")).thenReturn("1");
        when(configMock.getProperty("kor.db.memoryMode", "CACHED")).thenReturn("CACHED");
        when(configMock.getProperty("kor.db.maxValueLength", "16M")).thenReturn("16M");
        when(configMock.getProperty("kor.db.type", "hsqldb")).thenReturn("hsqldb");

        when(configMock.getProperty("ka.agentName", "agent1")).thenReturn("agent1");

        final VslVirtualNodeManager vManager = new VirtualNodeManager();
        final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
        final DummyStatisticsProvider dummyStatistics = new DummyStatisticsProvider();
        final VslTypeSearchProvider typeSearchProviderMock = Mockito
                .mock(VslTypeSearchProvider.class);

        kor = new KnowledgeRepository(vManager, subscriptionManager, configMock, dummyStatistics,
                typeSearchProviderMock, nodeFactory);
        kor.activate();
        subscriptionManager.activate(mock(VslAgentRegistryService.class),
                mock(VslTransportManager.class), mock(VslAgentName.class));
        localKorAddress = "/agent1";
        service = "service";
        serviceID = "serviceID";
        serviceVslID = new ServiceIdentity(serviceID, serviceID);
        alternativeVslID = new ServiceIdentity("alternativeID", "serviceID");
        testServiceAddress = localKorAddress + "/" + service;
        testModelId = "/test/testModel1";
        // setup testmodel
        kor.addSubtreeFromModelID(localKorAddress, testModelId, serviceID, service);
    }

    /**
     * Test for {@link KnowledgeRepository#applyKORUpdate(VslKORUpdate)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testApplyKORUpdate() throws Exception {
        // craft 2 new VslKORUpdates for testing, 1 for creating a new KA structure, 1 for updating
        // it
        final List<String> readers = Arrays.asList("creator", "system", "reader");
        final List<String> writers = Arrays.asList("creator", "system", "writer");

        final Set<String> removedNodes = new HashSet<String>();
        final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
        final VslStructureNodeImpl node1 = new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/treeRoot"), "");
        node1.putChild("service2", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed"), ""));
        node1.putChild("service2/a",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/text"), ""));
        node1.putChild("service2/b", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed", "/basic/number"), ""));
        node1.putChild("service2/b/c",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/number"), ""));
        node1.putChild("service3", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed"), ""));
        node1.putChild("service3/d",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/text"), ""));
        addedNodes.put("/KA2", node1);
        final VslKORUpdate update = new KORUpdate("", "", addedNodes, removedNodes, "KA2");

        final Set<String> removedNodes2 = new HashSet<String>();
        removedNodes2.add("/KA2/service2/a");
        removedNodes2.add("/KA2/service3");

        final Map<String, VslStructureNode> addedNodes2 = new HashMap<String, VslStructureNode>();
        final VslStructureNodeImpl node2 = new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed"), "");
        node2.putChild("d",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/text"), ""));
        node2.putChild("e", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed", "/basic/text"), ""));
        node2.putChild("e/f",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/text"), ""));
        node2.putChild("e/g",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/number"), ""));
        addedNodes2.put("/KA2/service4", node2);
        final VslKORUpdate update2 = new KORUpdate("", "", addedNodes2, removedNodes2, "KA2");

        kor.applyKORUpdate(update);
        kor.applyKORUpdate(update2);

        // get the metadata of the new KA (/KA2) to check if all nodes were added).
        final TreeMap<String, MetaNode> metaData = kor.getVslNodeTree().getMetaData("/", true);

        assertThat(metaData.containsKey("/KA2"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service2"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service2/b"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service2/b/c"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service4"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service4/d"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service4/e"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service4/e/f"), is(equalTo(true)));
        assertThat(metaData.containsKey("/KA2/service4/e/g"), is(equalTo(true)));

        assertThat(metaData.containsKey("/KA2/service3"), is(equalTo(false)));
        assertThat(metaData.containsKey("/KA2/service3/d"), is(equalTo(false)));
        assertThat(metaData.containsKey("/KA2/service2/a"), is(equalTo(false)));

        assertThat(metaData.get("/KA2").getType().toString(), is(equalTo("[/treeRoot]")));

        assertThat(metaData.get("/KA2/service4").getType().toString(),
                is(equalTo("[/basic/composed]")));
        assertThat(metaData.get("/KA2/service4").getReaderIDs().toString(),
                is(equalTo("[creator, system, reader]")));
        assertThat(metaData.get("/KA2/service4").getWriterIDs().toString(),
                is(equalTo("[creator, system, writer]")));
        assertThat(metaData.get("/KA2/service4").getType().toString(),
                is(equalTo("[/basic/composed]")));

        assertThat(metaData.get("/KA2/service4/e").getType().toString(),
                is(equalTo("[/basic/composed, /basic/text]")));
        assertThat(metaData.get("/KA2/service4/e/f").getType().toString(),
                is(equalTo("[/basic/text]")));
    }

    /**
     * Test for {@link KnowledgeRepository#commitSubtree(String, VslIdentity)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testCommitSubtree() throws Exception {
        final String lockedAddress = testServiceAddress + "/node1/submodel";
        kor.lockSubtree(lockedAddress, mock(VslLockHandler.class), serviceVslID);
        kor.set(lockedAddress + "/text2", nodeFactory.createImmutableLeaf("should be set now!"),
                serviceVslID);
        kor.commitSubtree(lockedAddress, serviceVslID);
        assertThat(kor.get(lockedAddress + "/text2", alternativeVslID).getValue(),
                is(equalTo("should be set now!")));
    }

    /**
     * Test for {@link KnowledgeRepository#getKORUpdateFromHash(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetKORUpdateFromHash() throws Exception {

        final String hashFrom = kor.getCurrentKORHash();
        // remove two list elements and add a new one to show that they are correctly shared:
        kor.get(testServiceAddress + "/listNode2/del/def", serviceVslID);
        kor.get(testServiceAddress + "/listNode2/del/ghi", serviceVslID);
        kor.get(testServiceAddress + "/listNode2/add/test/testModel2//newChild", serviceVslID);

        // retrieve update which should only contain the updates since we removed/added the new list
        // elements:
        final VslKORUpdate update = kor.getKORUpdateFromHash(hashFrom);

        assertThat(update.getHashFrom(), is(equalTo(hashFrom)));
        assertThat(update.getHashTo(), is(equalTo(kor.getCurrentKORHash())));
        assertThat(update.getAddedNodes().size(), is(equalTo(1)));
        assertThat(update.getRemovedNodes().size(), is(equalTo(2)));

        assertThat(update.getRemovedNodes().contains(testServiceAddress + "/listNode2/def"),
                is(equalTo(true)));
        assertThat(update.getRemovedNodes().contains(testServiceAddress + "/listNode2/ghi"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().containsKey(testServiceAddress + "/listNode2/newChild"),
                is(equalTo(true)));
    }

    /**
     * Test for {@link KnowledgeRepository#getKORUpdateFromHash(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetKORUpdateFromHashFullUpdate() throws Exception {

        // remove a list element to show that deleted nodes are correctly shared:
        kor.get(testServiceAddress + "/listNode2/del/def", serviceVslID);
        kor.get(testServiceAddress + "/listNode2/del/ghi", serviceVslID);

        final VslKORUpdate update = kor.getKORUpdateFromHash("");

        assertThat(update.getHashFrom(), is(equalTo("")));
        assertThat(update.getHashTo(), is(equalTo(kor.getCurrentKORHash())));

        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress)
                .hasChild("service/node1/submodel/text1"), is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/node2"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/node2/text"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/listNode2/abc"),
                is(equalTo(true)));
        assertThat(
                update.getAddedNodes().get(localKorAddress).hasChild("service/listNode2/abc/text1"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/listNode1/add"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/listNode1/del"),
                is(equalTo(true)));
        assertThat(
                update.getAddedNodes().get(localKorAddress).hasChild("service/listNode1/elements"),
                is(equalTo(true)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("system"),
                is(equalTo(true)));

        // check that the system subtree isn't shared:
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("system/config"),
                is(equalTo(false)));

        // Two list elements def and ghi should be deleted, not added:
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/listNode2/def"),
                is(equalTo(false)));
        assertThat(
                update.getAddedNodes().get(localKorAddress).hasChild("service/listNode2/def/text1"),
                is(equalTo(false)));
        assertThat(update.getAddedNodes().get(localKorAddress).hasChild("service/listNode2/ghi"),
                is(equalTo(false)));
    }

    /**
     * Test ListHandlings.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testListAdd() throws Exception {
        final String listRootAddress = testServiceAddress + "/listNode1";
        // add 3 elements to list and retrieve their new addresses.
        final String addedNodeAddress1 = kor
                .get(listRootAddress + "/add/0/test/testModel2", serviceVslID).getValue();
        final String addedNodeAddress2 = kor
                .get(listRootAddress + "/add/1/test/testModel2", serviceVslID).getValue();
        final String addedNodeAddress3 = kor
                .get(listRootAddress + "/add/2/test/testModel2//assignThisAsAddress", serviceVslID)
                .getValue();

        // Set different values on one of the nodes of the added models in order to be able to check
        // if operations work ok
        final VslNode listElement1Subnode = nodeFactory.createImmutableLeaf("123");
        final VslNode listElement2Subnode = nodeFactory.createImmutableLeaf("456");
        final VslNode listElement3Subnode = nodeFactory.createImmutableLeaf("789");
        kor.set(addedNodeAddress1 + "/text1", listElement1Subnode, serviceVslID);
        kor.set(addedNodeAddress2 + "/text1", listElement2Subnode, serviceVslID);
        kor.set(addedNodeAddress3 + "/text1", listElement3Subnode, serviceVslID);

        // retrieve the values of a node of the models added to the list by fqn or position
        final VslNode listElement1SubnodeByFullAddress = kor.get(addedNodeAddress1 + "/text1",
                serviceVslID);
        final VslNode listElement2SubnodeByFullAddress = kor.get(addedNodeAddress2 + "/text1",
                serviceVslID);
        VslNode listElement2SubnodeByPosition = kor.get(listRootAddress + "/1/text1", serviceVslID);
        final VslNode listElement3SubnodeByFullAddress = kor.get(addedNodeAddress3 + "/text1",
                serviceVslID);

        // check if values match
        assertThat(addedNodeAddress1, is(not(equalTo(addedNodeAddress2))));
        assertThat(addedNodeAddress2, is(not(equalTo(addedNodeAddress3))));
        assertThat(addedNodeAddress1, is(not(equalTo(addedNodeAddress3))));
        assertThat(addedNodeAddress3, is(equalTo(listRootAddress + "/assignThisAsAddress")));
        assertThat(listElement1SubnodeByFullAddress.getValue(),
                is(equalTo(listElement1Subnode.getValue())));
        assertThat(listElement2SubnodeByPosition.getValue(),
                is(equalTo(listElement2Subnode.getValue())));
        assertThat(listElement2SubnodeByFullAddress.getValue(),
                is(equalTo(listElement2Subnode.getValue())));
        assertThat(listElement3SubnodeByFullAddress.getValue(),
                is(equalTo(listElement3Subnode.getValue())));

        // add a new node inbetween and test if the positions are handled correctly.
        final String addedNodeAddress4 = kor
                .get(listRootAddress + "/add/1/test/testModel2", serviceVslID).getValue();
        kor.set(addedNodeAddress4 + "/text1", listElement1Subnode, serviceVslID);

        listElement2SubnodeByPosition = kor.get(listRootAddress + "/2/text1", serviceVslID);

        final VslNode listElement4SubnodeByPosition = kor.get(listRootAddress + "/1/text1",
                serviceVslID);
        // check if positions in list changed accordingly.
        assertThat(listElement2SubnodeByPosition.getValue(),
                is(equalTo(listElement2Subnode.getValue())));
        assertThat(listElement4SubnodeByPosition.getValue(),
                is(equalTo(listElement1Subnode.getValue())));
    }

    /**
     * Test ListHandlings.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testListDel() throws Exception {
        final String listRootAddress = testServiceAddress + "/listNode1";

        // add 3 elements to list and retrieve their new addresses.
        final String addedNodeAddress1 = kor
                .get(listRootAddress + "/add/0/test/testModel2", serviceVslID).getValue();
        final String addedNodeAddress2 = kor
                .get(listRootAddress + "/add/1/test/testModel2//assignThisAsAddress", serviceVslID)
                .getValue();
        final String addedNodeAddress3 = kor
                .get(listRootAddress + "/add/2/test/testModel2", serviceVslID).getValue();

        // Set different values on one of the nodes of the added models in order to be able to check
        // if operations work ok
        final VslNode listElement1Subnode = nodeFactory.createImmutableLeaf("123");
        final VslNode listElement2Subnode = nodeFactory.createImmutableLeaf("456");
        final VslNode listElement3Subnode = nodeFactory.createImmutableLeaf("789");

        kor.set(addedNodeAddress1 + "/text1", listElement1Subnode, serviceVslID);
        kor.set(addedNodeAddress2 + "/text1", listElement2Subnode, serviceVslID);
        kor.set(addedNodeAddress3 + "/text1", listElement3Subnode, serviceVslID);

        // retrieve the values of a node of the models added to the list by fqn or position

        final VslNode listElement2SubnodeByPosition = kor.get(listRootAddress + "/1/text1",
                serviceVslID);

        assertThat(kor.get(listRootAddress + "/elements", serviceVslID).getValue()
                .contains("assignThisAsAddress"), is(equalTo(true)));
        assertThat(kor
                .get(listRootAddress,
                        new AddressParameters().withDepth(-1).withNodeInformationScope(
                                NodeInformationScope.COMPLETE),
                        serviceVslID)
                .hasChild("assignThisAsAddress"), is(equalTo(true)));
        assertThat(listElement2SubnodeByPosition.getValue(),
                is(equalTo(listElement2Subnode.getValue())));

        kor.get(listRootAddress + "/del/assignThisAsAddress", serviceVslID);
        assertThat(kor.get(listRootAddress + "/elements", serviceVslID).getValue()
                .contains("assignThisAsAddress"), is(equalTo(false)));
        assertThat(kor.get(listRootAddress, serviceVslID).hasChild("assignThisAsAddress"),
                is(equalTo(false)));
        // the element on position 1 in the list should now contain 789 instead of 456
        assertThat(kor.get(listRootAddress + "/1/text1", serviceVslID).getValue(),
                is(equalTo(listElement3Subnode.getValue())));
    }

    /**
     * Test for {@link KnowledgeRepository#lockSubtree(String, VslLockHandler, VslIdentity)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testLockSubtree() throws Exception {
        final String lockedAddress = testServiceAddress + "/node1/submodel/text2";
        kor.lockSubtree(lockedAddress, mock(VslLockHandler.class), serviceVslID);
        kor.set(lockedAddress,
                nodeFactory.createImmutableLeaf("should only be set for serviceVslID!"),
                serviceVslID);
        assertThat(kor.get(lockedAddress, serviceVslID).getValue(),
                is(equalTo("should only be set for serviceVslID!")));
        assertThat(kor.get(lockedAddress, alternativeVslID).getValue(), is(equalTo("text2")));
    }

    /**
     * Test the functionality of nested Lists.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testNestedLists() throws Exception {
        final String listRootAddress = testServiceAddress + "/listNode1";
        // add 3 elements to list and retrieve their new addresses.
        final String addedSubList = kor
                .get(listRootAddress + "/add/0/test/testModel1//element1", serviceVslID).getValue();
        kor.get(addedSubList + "/listNode1/add/0/test/testModel2//element2", serviceVslID);
        final VslNode listElement1Subnode = nodeFactory.createImmutableLeaf("123");

        assertThat(kor.get(listRootAddress + "/0/listNode1/0/text2", serviceVslID).getValue(),
                is(equalTo("text2")));

        kor.set(listRootAddress + "/0/listNode1/0/text2", listElement1Subnode, serviceVslID);
        assertThat(kor.get(listRootAddress + "/0/listNode1/0/text2", serviceVslID).getValue(),
                is(equalTo(listElement1Subnode.getValue())));

        final String addedList = kor
                .get(listRootAddress + "/add/1/basic/list//element3", serviceVslID).getValue();
        final String addedList2 = kor.get(addedList + "/add/0/basic/list//element4", serviceVslID)
                .getValue();
        kor.get(addedList2 + "/add/0/basic/text//textNode", serviceVslID).getValue();
        kor.set(addedList2 + "/0", nodeFactory.createImmutableLeaf("test"), serviceVslID);
        assertThat(kor.get(listRootAddress + "/1/0/elements", serviceVslID).getValue(),
                is(equalTo("textNode")));
        assertThat(kor.get(listRootAddress + "/1/0/0", serviceVslID).getValue(),
                is(equalTo("test")));
    }

    /**
     * Test for {@link KnowledgeRepository#commitSubtree(String, VslIdentity)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRollbackSubtree() throws Exception {
        final String lockedAddress = testServiceAddress + "/node1/submodel";
        kor.lockSubtree(lockedAddress, mock(VslLockHandler.class), serviceVslID);
        kor.set(lockedAddress + "/text2", nodeFactory.createImmutableLeaf("should be set now!"),
                serviceVslID);
        kor.rollbackSubtree(lockedAddress, serviceVslID);
        assertThat(kor.get(lockedAddress + "/text2", serviceVslID).getValue(),
                is(equalTo("text2")));
    }

    /**
     * Test for set operation from an id which doesn't own the log.
     *
     * @throws Exception
     *             NodeLockedException is tested, other shouldn't happen.
     */
    @Test
    public final void testSetOnLockedNodeInvalidID() throws Exception {
        final String lockedAddress = testServiceAddress + "/node1/submodel";
        kor.lockSubtree(lockedAddress, mock(VslLockHandler.class), serviceVslID);

        expectedException.expect(NodeLockedException.class);
        // alternativeVslId has the same access IDs as serviceVslId but a different clientId

        kor.set(lockedAddress + "/text2", nodeFactory.createImmutableLeaf("shouldn't be set yet!"),
                alternativeVslID);
    }

    /**
     * Tests if the setup was successful.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testSetup() throws VslException {

        final VslNode result = kor.get(testServiceAddress, new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE), serviceVslID);

        // check if model was initialized correctly.
        // first the normal nodes
        assertThat(result.hasChild("node1"), is(equalTo(true)));
        assertThat(result.hasChild("node2"), is(equalTo(true)));
        assertThat(result.hasChild("node1/bool"), is(equalTo(true)));
        assertThat(result.getChild("node1/bool").getValue(), is(equalTo("1")));
        assertThat(result.getChild("node1/bool").getTypes().toString().contains("/derived/boolean"),
                is(equalTo(true)));

        assertThat(result.hasChild("node1/submodel"), is(equalTo(true)));
        assertThat(result.hasChild("node1/submodel/text1"), is(equalTo(true)));
        assertThat(result.getChild("node1/submodel/text1").getValue(), is(equalTo("")));
        assertThat(result.getChild("node1/submodel/text1").getTypes().toString(),
                is(equalTo("[/basic/text]")));
        assertThat(result.hasChild("node1/submodel/text2"), is(equalTo(true)));
        assertThat(result.getChild("node1/submodel/text2").getValue(), is(equalTo("text2")));
        assertThat(result.hasChild("node1/submodel/number"), is(equalTo(true)));
        assertThat(result.getChild("node1/submodel/number").getValue(), is(equalTo("")));
        assertThat(result.hasChild("node2/text"), is(equalTo(true)));
        assertThat(result.getChild("node2/text").getValue(), is(equalTo("TestText")));
        assertThat(result.hasChild("node2/number"), is(equalTo(true)));
        assertThat(result.getChild("node2/number").getValue(), is(equalTo("42")));
        assertThat(result.getChild("node2/number").getTypes().toString(),
                is(equalTo("[/basic/number]")));

        // check list initialization
        assertThat(result.hasChild("listNode1"), is(equalTo(true)));
        assertThat(result.getChild("listNode1").getTypes().toString().contains("/basic/list"),
                is(equalTo(true)));

        assertThat(result.hasChild("listNode1/elements"), is(equalTo(true)));
        assertThat(result.getChild("listNode1/elements").getValue(), is(equalTo("")));
        assertThat(result.hasChild("listNode1/add"), is(equalTo(true)));
        assertThat(result.hasChild("listNode1/del"), is(equalTo(true)));
        // check list with an initial init
        assertThat(result.hasChild("listNode2"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/elements"), is(equalTo(true)));
        assertThat(result.getChild("listNode2/elements").getValue(),
                is(equalTo("abc;jkl;ghi;def")));
        assertThat(result.hasChild("listNode2/add"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/del"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/abc"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/abc/text1"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/abc/text2"), is(equalTo(true)));
        assertThat(result.getChild("listNode2/abc/text2").getValue(), is(equalTo("text2")));
        assertThat(result.hasChild("listNode2/abc/number"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/def"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/ghi"), is(equalTo(true)));
        assertThat(result.hasChild("listNode2/jkl"), is(equalTo(true)));

    }

    /**
     * Test VirtualNode Handling.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testVirtualNodes() throws Exception {
        final String virtualNodeAddress = testServiceAddress + "/node1";
        // use internal class for this test as virtualNodeHandler.
        final VslVirtualNodeHandler testHandler = new VslVirtualNodeHandler() {
            HashMap<String, VslNode> storage = new HashMap<String, VslNode>();

            @Override
            public VslNode get(final String address, final VslAddressParameters params,
                    final VslIdentity identity) throws VslException {
                return storage.get(address);
            }

            @Override
            public void set(final String address, final VslNode value, final VslIdentity identity)
                    throws VslException {
                storage.put(address, value);
            }

            @Override
            public void subscribe(final String address)
                    throws SubscriptionNotSupportedException, VslException {
                // doNothing, not relevant
            }

            @Override
            public void unsubscribe(final String address) {
                // doNothing, not relevant
            }

            @Override
            public InputStream getStream(String address, VslIdentity identity) throws VslException {
                // doNothing, not relevant
                return null;
            }

            @Override
            public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
                // doNothing, not relevant
            }
        };
        kor.registerVirtualNode(virtualNodeAddress, testHandler, serviceVslID);
        final String testAddress1 = virtualNodeAddress;
        final String testAddress2 = virtualNodeAddress + "/bool";
        final String testAddress3 = virtualNodeAddress + "/bool/this/is/only/virtual";
        final VslNode testNode1 = nodeFactory.createImmutableLeaf("test1");
        final VslNode testNode2 = nodeFactory.createImmutableLeaf("0");
        final VslNode testNode3 = nodeFactory.createImmutableLeaf("test3");

        // virtual:
        kor.set(testAddress1, testNode1, serviceVslID);
        // this should set the REAL node value, since the node exists and is not virtual:
        kor.set(testAddress2, testNode2, serviceVslID);
        // virtual:
        kor.set(testAddress3, testNode3, serviceVslID);

        final VslNode testNode1Get = kor.get(testAddress1, serviceVslID);
        final VslNode testNode2Get = kor.get(testAddress2, serviceVslID);
        final VslNode testNode3Get = kor.get(testAddress3, serviceVslID);

        // Our test VslVirtualNodehandler simply stores VslNodes on set and returns them on get
        // requests.
        assertThat(testNode1Get, is(equalTo(testNode1)));
        assertThat(testNode1Get.getValue(), is(equalTo(testNode1.getValue())));

        // for the real node, value should be the same, but a new Vsl node object is generated
        assertThat(testNode2Get, is(not(equalTo(testNode2))));
        assertThat(testNode2Get.getValue(), is(equalTo(testNode2.getValue())));

        // again virtual
        assertThat(testNode3Get, is(equalTo(testNode3)));
        assertThat(testNode3Get.getValue(), is(equalTo(testNode3.getValue())));

    }

    /**
     * Test Subscription Handling.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testLocalSubscriptions() throws Exception {
        final AtomicReference<String> var = new AtomicReference<String>("");
        final VslSubscriber sub1 = new VslSubscriber() {
            @Override
            public void notificationCallback(final String address) {
                var.set(var.get() + address + "subscriber1");
            }
        };
        final VslSubscriber sub2 = new VslSubscriber() {
            @Override
            public void notificationCallback(final String address) {
                var.set(var.get() + address + "subscriber2");
            }
        };
        final VslSubscriber sub3 = new VslSubscriber() {
            @Override
            public void notificationCallback(final String address) {
                var.set(var.get() + address + "subscriber3");
            }
        };
        kor.subscribe(testServiceAddress + "/node1", sub1, serviceVslID);
        kor.subscribe(testServiceAddress + "/node1/submodel/text1", sub2, serviceVslID);
        kor.subscribe(testServiceAddress + "/node2", sub3, serviceVslID);

        kor.set(testServiceAddress + "/node1/submodel/text1",
                nodeFactory.createImmutableLeaf("TestValue"), serviceVslID);
        Thread.sleep(1000);
        assertThat(var.get(),
                not(containsString(testServiceAddress + "/node1/submodel/text1" + "subscriber1")));
        assertThat(var.get(),
                containsString(testServiceAddress + "/node1/submodel/text1" + "subscriber2"));
        assertThat(var.get(),
                not(containsString(testServiceAddress + "/node1/submodel/text1" + "subscriber3")));
    }

    /**
     * Test caching of nodes.
     *
     * @throws VslException
     *             Shouldn't happen
     */
    @Test
    public final void testCacheVslNode() throws VslException {

        // create a fake remote KA for cache testing, using the KORUpdate functionality
        final List<String> readers = Arrays.asList("creator", "system", "reader");
        final List<String> writers = Arrays.asList("creator", "system", "writer");
        final Set<String> removedNodes = new HashSet<String>();
        final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
        final VslStructureNodeImpl structure = new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/treeRoot"), "");
        structure.putChild("cachable", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed"), ""));
        structure.putChild("cachable/a",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/text"), ""));
        structure.putChild("cachable/a/c", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed", "/basic/number"), ""));
        structure.putChild("cachable/b",
                new VslStructureNodeImpl(readers, writers, "", Arrays.asList("/basic/number"), ""));
        structure.putChild("cachable/b/d", new VslStructureNodeImpl(readers, writers, "",
                Arrays.asList("/basic/composed"), ""));
        addedNodes.put("/KA2", structure);
        kor.applyKORUpdate(new KORUpdate("", "", addedNodes, removedNodes, "KA2"));

        final String address = "/KA2/cachable";
        final ArrayList<String> type1 = new ArrayList<String>();
        type1.add("/type/1");

        final Map<String, VslNode> nodes = new HashMap<String, VslNode>();
        nodes.put("", nodeFactory.createImmutableLeaf(type1, "rootValue", new Date(1234), 4, null,
                Collections.<String, String>emptyMap()));
        nodes.put("a", nodeFactory.createImmutableLeaf(type1, "child_a", new Date(1235), 3, null,
                Collections.<String, String>emptyMap()));
        nodes.put("a/c", nodeFactory.createImmutableLeaf(new ArrayList<String>(), null, null, -1,
                null, Collections.<String, String>emptyMap()));
        nodes.put("b", nodeFactory.createImmutableLeaf(new ArrayList<String>(), null, null, -1,
                null, Collections.<String, String>emptyMap()));
        nodes.put("b/d", nodeFactory.createImmutableLeaf(type1, "child_b_d", new Date(1236), 1,
                null, Collections.<String, String>emptyMap()));
        final VslNode testNode = nodeFactory.createImmutableNode(nodes.entrySet());

        kor.cacheVslNodes(address, testNode);

        final VslNode node = kor
                .get(address,
                        new AddressParameters().withDepth(-1)
                                .withNodeInformationScope(NodeInformationScope.COMPLETE),
                        new ServiceIdentity("creator", "creator"));

        assertThat(getSize(node.getAllChildren()), is(equalTo(4)));
        assertThat(node.getValue(), is(equalTo("rootValue")));
        assertThat(node.getChild("a").getValue(), is(equalTo("child_a")));
        assertThat(node.getChild("a").getChild("c").getValue(), is(nullValue()));
        assertThat(node.getChild("b").getValue(), is(nullValue()));
        assertThat(node.getChild("b").getChild("d").getValue(), is(equalTo("child_b_d")));

        assertThat(node.getVersion(), is(equalTo(4L)));
        assertThat(node.getChild("a").getVersion(), is(equalTo(3L)));
        assertThat(node.getChild("a").getChild("c").getVersion(), is(equalTo(0L)));
        assertThat(node.getChild("b").getVersion(), is(equalTo(0L)));
        assertThat(node.getChild("b").getChild("d").getVersion(), is(equalTo(1L)));

        assertThat(node.getTimestamp(), is(equalTo(new Date(1234))));
        assertThat(node.getChild("a").getTimestamp(), is(equalTo(new Date(1235))));
        assertThat(node.getChild("a").getChild("c").getTimestamp(), is(nullValue()));
        assertThat(node.getChild("b").getTimestamp(), is(nullValue()));
        assertThat(node.getChild("b").getChild("d").getTimestamp(), is(equalTo(new Date(1236))));

    }

    /**
     * Returns the size of an Iterable.
     *
     * @param iterable
     *            The iterable to test.
     * @return Size.
     */
    private int getSize(final Iterable<Entry<String, VslNode>> iterable) {
        final Iterator<Entry<String, VslNode>> iterator = iterable.iterator();
        int size = 0;
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }

}
