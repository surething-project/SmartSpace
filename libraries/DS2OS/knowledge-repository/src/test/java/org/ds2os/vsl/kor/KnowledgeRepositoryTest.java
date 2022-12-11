package org.ds2os.vsl.kor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.modelCache.VslModelCache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 * @author liebald
 *
 */
public class KnowledgeRepositoryTest {

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Mock of an {@link VslIdentity} used for testing.
     */
    private VslIdentity idMock;

    /**
     * Local {@link VslKnowledgeRepository} to test.
     */
    private VslKnowledgeRepository kor;

    /**
     * Mock of the {@link VslModelCache} used for testing.
     */
    private VslModelCache modelCacheMock;

    /**
     * Mock of the {@link VslNodeTree} used for testing.
     */
    private VslNodeTree nodeTreeMock;

    /**
     * Mock of the {@link VslNodeDatabase} used for testing.
     */
    private VslNodeDatabase nodeDBMock;
    /**
     * Address of a service used for tests.
     */
    private String service;

    /**
     * The mocked {@link VslVirtualNodeManager} used by the KOR for operations.
     */
    private VslVirtualNodeManager vManagerMock;

    /**
     * The mocked {@link VslSubscriptionManager} used by the KOR for subscription handling.
     */
    private VslSubscriptionManager subscriptionManagerMock;

    /**
     * The {@link VslNodeFactory} for creating VslNodes.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * Initialize KOR.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        idMock = mock(VslIdentity.class);
        nodeTreeMock = mock(VslNodeTree.class);
        modelCacheMock = mock(VslModelCache.class);
        vManagerMock = mock(VslVirtualNodeManager.class);
        nodeDBMock = mock(VslNodeDatabase.class);
        subscriptionManagerMock = mock(VslSubscriptionManager.class);
        final VslStatisticsProvider statisticsProvider = new DummyStatisticsProvider();
        final VslTypeSearchProvider typeSearchProviderMock = Mockito
                .mock(VslTypeSearchProvider.class);

        kor = new KnowledgeRepository(vManagerMock, subscriptionManagerMock, nodeTreeMock,
                modelCacheMock, InitialConfigFromFile.getInstance(), nodeDBMock, statisticsProvider,
                typeSearchProviderMock, nodeFactory);
        service = "/KA/service";
    }

    /**
     * Test method for
     * {@link KnowledgeRepository#addSubtreeFromModelID(String, String, String, String)} .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testAddSubtreeFromModelID() throws Exception {
        final String modelID = "/model/test/ID";
        final String creatorId = "IdidThis";
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> readers = new LinkedList<String>();
        types.add("reader");
        final LinkedList<String> writers = new LinkedList<String>();
        types.add("writer");
        final Timestamp ts = new Timestamp(System.currentTimeMillis());
        final InternalNode modelNode1 = new InternalNode(types, "1", readers, writers, 1, ts, "",
                "");
        final InternalNode modelNode2 = new InternalNode(types, "2", readers, writers, 2, ts, "",
                "");
        final InternalNode modelNode12 = new InternalNode(types, "12", readers, writers, 12, ts, "",
                "");
        final LinkedHashMap<String, InternalNode> tm = new LinkedHashMap<String, InternalNode>();
        tm.put("service/1", modelNode1);
        tm.put("service/2", modelNode2);
        tm.put("service/1/2", modelNode12);

        when(modelCacheMock.getCompleteModelNodes(modelID, creatorId)).thenReturn(tm);

        kor.addSubtreeFromModelID("/KA", modelID, creatorId, creatorId);

        verify(modelCacheMock).getCompleteModelNodes(modelID, creatorId);
        verify(nodeTreeMock).addNode("/KA/service/1", modelNode1.getType(),
                modelNode1.getReaderIDs(), modelNode1.getWriterIDs(), modelNode1.getRestriction(),
                modelNode1.getCacheParameters(), creatorId);
        verify(nodeTreeMock).addNode("/KA/service/2", modelNode2.getType(),
                modelNode2.getReaderIDs(), modelNode2.getWriterIDs(), modelNode2.getRestriction(),
                modelNode2.getCacheParameters(), creatorId);
        verify(nodeTreeMock).addNode("/KA/service/1/2", modelNode12.getType(),
                modelNode12.getReaderIDs(), modelNode12.getWriterIDs(),
                modelNode12.getRestriction(), modelNode12.getCacheParameters(), creatorId);
    }

    /**
     * Test method for
     * {@link KnowledgeRepository#addSubtreeFromModelID(String, String, String, String)} .
     *
     * @throws Exception
     *             shouldn't happen (tested exception should be catched and handled).
     */
    @Test
    public final void testAddSubtreeFromModelIDExceptionOnAdding() throws Exception {
        final String modelID = "/model/test/ID";
        final String creatorId = "IdidThis";
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> readers = new LinkedList<String>();
        types.add("reader");
        final LinkedList<String> writers = new LinkedList<String>();
        types.add("writer");
        final Timestamp ts = new Timestamp(System.currentTimeMillis());
        final InternalNode modelNode1 = new InternalNode(types, "1", readers, writers, 1, ts, "",
                "");

        final LinkedHashMap<String, InternalNode> tm = new LinkedHashMap<String, InternalNode>();
        tm.put("service/1", modelNode1);

        when(modelCacheMock.getCompleteModelNodes(modelID, creatorId)).thenReturn(tm);
        doThrow(NodeAlreadyExistingException.class).when(nodeTreeMock).addNode("/KA/service/1",
                modelNode1.getType(), modelNode1.getReaderIDs(), modelNode1.getWriterIDs(),
                modelNode1.getRestriction(), modelNode1.getCacheParameters(), creatorId);

        kor.addSubtreeFromModelID("/KA", modelID, creatorId, creatorId);

        verify(modelCacheMock).getCompleteModelNodes(modelID, creatorId);
        verify(nodeTreeMock).removeNode("/KA/service/1");
    }

    // /**
    // * Test method for {@link KnowledgeRepository#getKORUpdateFull()}.
    // *
    // * @throws Exception
    // * shouldn't happen.
    // */
    // @Test
    // public final void testGetKORUpdateFull() throws Exception {
    //
    // final TreeMap<String, MetaNode> ka1Structure = new TreeMap<String, MetaNode>();
    // ka1Structure.put("/ka1", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka1Structure.put("/ka1/service1", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka1Structure.put("/ka1/service1/node", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka1Structure.put("/ka1/system", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka1Structure.put("/ka1/system/config", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    //
    // final TreeMap<String, MetaNode> ka2Structure = new TreeMap<String, MetaNode>();
    // ka2Structure.put("/ka2", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka2Structure.put("/ka2/service2", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka2Structure.put("/ka2/service2/node", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // ka2Structure.put("/ka2/system", new MetaNode(Arrays.asList("/basic/composed"),
    // Arrays.asList("reader"), Arrays.asList("writer"), ""));
    // when(nodeDBMock.getDirectChildrenAddresses("/"))
    // .thenReturn(Arrays.asList("/ka1", "/ka2"));
    // when(nodeTreeMock.getMetaData("/ka1", true)).thenReturn(ka1Structure);
    // when(nodeTreeMock.getMetaData("/ka2", true)).thenReturn(ka2Structure);
    //
    // final List<VslKORUpdate> updates = kor.getKORUpdateFull();
    // assertThat(updates.size(), is(equalTo(2)));
    // assertThat(updates.get(0).getRemovedNodes().size(), is(equalTo(0)));
    // assertThat(updates.get(1).getRemovedNodes().size(), is(equalTo(0)));
    //
    // assertThat(updates.get(0).getAddedNodes().keySet().size(), is(equalTo(1)));
    // assertThat(updates.get(0).getAddedNodes().keySet().contains("/ka1"), is(equalTo(true)));
    // assertThat(updates.get(0).getAddedNodes().get("/ka1").getStructureChildren().size(),
    // is(equalTo(3)));
    // assertThat(updates.get(0).getAddedNodes().get("/ka1").getStructureChildren().keySet()
    // .contains("service1"), is(equalTo(true)));
    // assertThat(updates.get(0).getAddedNodes().get("/ka1").getStructureChildren().keySet()
    // .contains("service1/node"), is(equalTo(true)));
    // assertThat(updates.get(0).getAddedNodes().get("/ka1").getStructureChildren().keySet()
    // .contains("system"), is(equalTo(true)));
    // assertThat(updates.get(0).getAddedNodes().get("/ka1").getStructureChildren().keySet()
    // .contains("system/config"), is(equalTo(false)));
    //
    // assertThat(updates.get(1).getAddedNodes().keySet().size(), is(equalTo(1)));
    // assertThat(updates.get(1).getAddedNodes().keySet().contains("/ka2"), is(equalTo(true)));
    // assertThat(updates.get(1).getAddedNodes().get("/ka2").getStructureChildren().size(),
    // is(equalTo(3)));
    // assertThat(updates.get(1).getAddedNodes().get("/ka2").getStructureChildren().keySet()
    // .contains("service2"), is(equalTo(true)));
    // assertThat(updates.get(1).getAddedNodes().get("/ka2").getStructureChildren().keySet()
    // .contains("service2/node"), is(equalTo(true)));
    // assertThat(updates.get(1).getAddedNodes().get("/ka2").getStructureChildren().keySet()
    // .contains("system"), is(equalTo(true)));
    //
    // }

    /**
     * Test method for {@link KnowledgeRepository#get(String, VslIdentity)} .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetNormalNode() throws Exception {
        final String node = service + "/node1";
        final VslNode test = nodeFactory.createImmutableLeaf(Arrays.asList("/ab/c"), "123");
        when(nodeTreeMock.get(eq(node), any(VslAddressParameters.class), eq(idMock)))
                .thenReturn(test);
        final VslNode result = kor.get(node, idMock);
        assertThat(result, is(equalTo(test)));
        verify(vManagerMock).isVirtualNode(node);
        verify(vManagerMock, never()).getVirtualNodeHandler(node);
        verify(nodeTreeMock).get(eq(node), any(VslAddressParameters.class), eq(idMock));
    }

    /**
     * Test method for {@link KnowledgeRepository#get(String, VslIdentity)} . Assumes the node is
     * virtual.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetVirtualNode() throws Exception {
        final String node = service + "/node1";
        final VslNode realNode = nodeFactory.createImmutableLeaf(Arrays.asList("/ab/c"), "123");
        final VslNode virtualNode = nodeFactory.createImmutableLeaf(Arrays.asList("/v/node"),
                "v123");
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> readers = new LinkedList<String>();

        readers.add("reader1");

        when(nodeTreeMock.get(eq(node), any(VslAddressParameters.class), eq(idMock)))
                .thenReturn(realNode);
        when(vManagerMock.isVirtualNode(node)).thenReturn(true);
        when(vManagerMock.getVirtualNodeHandler(node)).thenReturn(vHandlerMock);

        when(vHandlerMock.get(eq(node), any(VslAddressParameters.class), any(VslIdentity.class)))
                .thenReturn(virtualNode);

        final VslNode result = kor.get(node, idMock);

        assertThat(result, is(equalTo(virtualNode)));
        verify(vManagerMock).getVirtualNodeHandler(node);
        verify(vHandlerMock).get(eq(node), any(VslAddressParameters.class), eq(idMock));
        verify(nodeTreeMock).get(eq(node), any(VslAddressParameters.class), eq(idMock));
    }

    /**
     * Test method for {@link KnowledgeRepository#get(String, VslIdentity)} .Assumes the node
     * doesn't exist but a parent is virtual.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetVirtualParentNode() throws Exception {
        final String node = service + "/node1/node2/node3";
        final String virtualParent = service + "/node1";
        final VslNode virtualNode = nodeFactory.createImmutableLeaf(Arrays.asList("/v/node"),
                "v123");
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> readers = new LinkedList<String>();
        readers.add("reader1");
        doThrow(NodeNotExistingException.class).when(nodeTreeMock).get(eq(node),
                any(VslAddressParameters.class), eq(idMock));

        when(vManagerMock.getFirstVirtualParent(node)).thenReturn(virtualParent);

        when(vManagerMock.getVirtualNodeHandler(virtualParent)).thenReturn(vHandlerMock);
        when(vHandlerMock.get(eq(node), any(VslAddressParameters.class), any(VslIdentity.class)))
                .thenReturn(virtualNode);

        final VslNode result = kor.get(node, idMock);

        assertThat(result, is(equalTo(virtualNode)));
        verify(vManagerMock).getVirtualNodeHandler(virtualParent);
        verify(vManagerMock, never()).getVirtualNodeHandler(node);

        verify(vHandlerMock).get(eq(node), any(VslAddressParameters.class), eq(idMock));
        verify(vHandlerMock, never()).get(eq(virtualParent), any(VslAddressParameters.class),
                eq(idMock));

        verify(nodeTreeMock).get(eq(node), any(VslAddressParameters.class), eq(idMock));
    }

    /**
     * Test method for
     * {@link KnowledgeRepository#registerVirtualNode(String, VslVirtualNodeHandler, VslIdentity)} .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRegisterVirtualNode() throws Exception {
        final String virtualAddress = service + "/vnode";
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> accessIDs = new LinkedList<String>();
        accessIDs.add("serviceID");
        when(idMock.getClientId()).thenReturn("serviceID");
        when(nodeTreeMock.getNodeCreatorId(virtualAddress)).thenReturn("serviceID");

        kor.registerVirtualNode(virtualAddress, vHandlerMock, idMock);

        verify(vManagerMock).registerVirtualNode(virtualAddress, vHandlerMock);
        verify(nodeTreeMock).getNodeCreatorId(virtualAddress);

    }

    /**
     * Test method for
     * {@link KnowledgeRepository#registerVirtualNode(String, VslVirtualNodeHandler, VslIdentity)} .
     *
     * @throws Exception
     *             tested
     */
    @Test
    public final void testRegisterVirtualNodeException() throws Exception {
        final String virtualAddress = service + "/vnode";
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> accessIDs = new LinkedList<String>();
        accessIDs.add("serviceID");
        when(idMock.getClientId()).thenReturn("serviceID1");
        when(nodeTreeMock.getNodeCreatorId(virtualAddress)).thenReturn("serviceID2");

        expectedException.expect(NoPermissionException.class);
        kor.registerVirtualNode(virtualAddress, vHandlerMock, idMock);

    }

    /**
     * Test method for {@link KnowledgeRepository#set(String,VslNode, VslIdentity)} .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testSetNormalNode() throws Exception {
        final String node = service + "/node1";
        final VslNode test = nodeFactory.createImmutableLeaf(Arrays.asList("/ab/c"), "123");
        kor.set(node, test, idMock);
        verify(nodeTreeMock).setValue(node, idMock, test);
        verify(vManagerMock).isVirtualNode(node);
        verify(vManagerMock, never()).getVirtualNodeHandler(node);
    }

    /**
     * Test method for {link KnowledgeRepository#set(String,VslNode, VslIdentity)} . Assumes the
     * node is virtual.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testSetVirtualNode() throws Exception {
        final String node = service + "/node1";
        final VslNode virtualNode = nodeFactory.createImmutableLeaf(Arrays.asList("/v/node"),
                "v123");
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> writers = new LinkedList<String>();
        writers.add("writers1");

        when(vManagerMock.isVirtualNode(node)).thenReturn(true);
        when(vManagerMock.getVirtualNodeHandler(node)).thenReturn(vHandlerMock);

        when(vHandlerMock.get(eq(node), any(VslAddressParameters.class), any(VslIdentity.class)))
                .thenReturn(virtualNode);

        kor.set(node, virtualNode, idMock);

        verify(vManagerMock).getVirtualNodeHandler(node);
        verify(nodeTreeMock).setValue(node, idMock, virtualNode);
        verify(vHandlerMock).set(node, virtualNode, idMock);
    }

    /**
     * Test method for {@link KnowledgeRepository#set(String, VslNode, VslIdentity)} . Assumes the
     * node doesn't exist but a parent is virtual.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testSetVirtualParentNode() throws Exception {
        final String node = service + "/node1/node2/node3";
        final String virtualParent = service + "/node1";
        final VslNode virtualNode = nodeFactory.createImmutableLeaf(Arrays.asList("/v/node"),
                "v123");
        final VslVirtualNodeHandler vHandlerMock = mock(VslVirtualNodeHandler.class);
        final LinkedList<String> writers = new LinkedList<String>();
        writers.add("writer1");
        doThrow(NodeNotExistingException.class).when(nodeTreeMock).setValue(node, idMock,
                virtualNode);

        when(vManagerMock.getFirstVirtualParent(node)).thenReturn(virtualParent);

        when(vManagerMock.getVirtualNodeHandler(virtualParent)).thenReturn(vHandlerMock);

        kor.set(node, virtualNode, idMock);

        verify(vManagerMock).getVirtualNodeHandler(virtualParent);
        verify(vManagerMock, never()).getVirtualNodeHandler(node);

        verify(vHandlerMock).set(node, virtualNode, idMock);
        verify(vHandlerMock, never()).get(eq(virtualParent), any(VslAddressParameters.class),
                eq(idMock));

        verify(nodeTreeMock).setValue(node, idMock, virtualNode);
        verify(nodeTreeMock, never()).setValue(virtualParent, idMock, virtualNode);

    }

    /**
     * Test method for {@link KnowledgeRepository#unregisterVirtualNode(String, VslIdentity)} .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testUnregisterVirtualNode() throws Exception {
        final String virtualAddress = service + "/vnode";

        final LinkedList<String> accessIDs = new LinkedList<String>();
        accessIDs.add("serviceID");
        when(idMock.getAccessIDs()).thenReturn(accessIDs);
        when(nodeTreeMock.getNodeCreatorId(virtualAddress)).thenReturn("serviceID");
        kor.unregisterVirtualNode(virtualAddress, idMock);

        verify(vManagerMock).unregisterVirtualNode(virtualAddress);
        verify(nodeTreeMock).getNodeCreatorId(virtualAddress);
    }

    /**
     * Test method for {@link KnowledgeRepository#unregisterVirtualNode(String, VslIdentity)} .
     *
     * @throws Exception
     *             test NoPermission.
     */
    @Test
    public final void testUnregisterVirtualNodeException() throws Exception {
        final String virtualAddress = service + "/vnode";

        final LinkedList<String> accessIDs = new LinkedList<String>();
        accessIDs.add("serviceID");
        when(idMock.getAccessIDs()).thenReturn(accessIDs);
        when(nodeTreeMock.getNodeCreatorId(virtualAddress)).thenReturn("serviceID2");

        expectedException.expect(NoPermissionException.class);
        kor.unregisterVirtualNode(virtualAddress, idMock);

    }

}
