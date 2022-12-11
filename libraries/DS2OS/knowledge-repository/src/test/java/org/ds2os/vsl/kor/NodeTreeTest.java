package org.ds2os.vsl.kor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.InvalidValueException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeLockedException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.ParentNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.ds2os.vsl.kor.locking.VslLocker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

/**
 * @author liebald
 *
 */
public class NodeTreeTest {

    /**
     * The access IDs that are seen as valid during the testing.
     */
    private LinkedList<String> access;

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * the mocked {@link VslIdentity} Object used for access control.
     */
    private VslIdentity id;

    /**
     * ID of the local KA.
     */
    private final String localID = "localKA";

    /**
     * The mocked {@link HSQLDatabaseOld} the Nodetree works with.
     */
    private VslNodeDatabase mockedDB;

    /**
     * The {@link NodeTree} Object used for tests.
     */
    private VslNodeTree nt;

    /**
     * The {@link VslNodeFactory} for creating VslNodes.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * Initialize the NodeTree object.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws NodeNotExistingException {
        mockedDB = mock(VslNodeDatabase.class);
        // expected behavior from the Nodetree constructor:

        id = mock(VslIdentity.class);
        access = new LinkedList<String>();
        access.add("system");
        when(id.getAccessIDs()).thenReturn(access);
        nt = new NodeTree(localID, mockedDB, mock(VslLocker.class), nodeFactory);
        // nt.activate(); //This would only initialize the Database and would lead to (caught)
        // exceptions on a mock
        final List<String> types = new LinkedList<String>();
        types.add("/treeRoot");
        // verify(mockedDB).addNode("/" + localID, types, access, access, "", "");
        // verify(mockedDB).addNode("/" + localID + "/system", types, access, access, "");
        when(mockedDB.nodeExists("/")).thenReturn(true);

        access.remove("system");
        access.add("ID1");

    }

    /**
     * Test method for {@link NodeTree#addNode(String, List, List, List, String, String, String)}.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws ParentNotExistingException
     *             shouldn't happen.
     * @throws NodeAlreadyExistingException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testAddNode() throws NodeAlreadyExistingException, ParentNotExistingException,
            InvalidValueException, NodeNotExistingException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String parent = "/" + localID + "/Hallo";
        final String restriction = "";
        final List<String> types = new LinkedList<String>();
        types.add("/type/1");
        when(mockedDB.nodeExists(parent)).thenReturn(true);
        when(mockedDB.nodeExists(address)).thenReturn(false);

        // act
        nt.addNode(address, types, access, access, restriction, "TTL='5'", "myId");

        // assert/verify
        access.add(0, "system");
        access.add(0, "myId");

        verify(mockedDB).nodeExists(address);
        verify(mockedDB).nodeExists(parent);
        verify(mockedDB).addNode(address, types, access, access, restriction, "TTL='5'");
    }

    /**
     * Test method for {@link NodeTree#addNode(String, List, List, List, String, String, String)}.
     * Test NodeAlreadyExistingException.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws ParentNotExistingException
     *             shouldn't happen.
     * @throws NodeAlreadyExistingException
     *             tested.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testAddNodeNodeAlreadyExistingException() throws NodeAlreadyExistingException,
            ParentNotExistingException, InvalidValueException, NodeNotExistingException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String parent = "/" + localID + "/Hallo";
        final String restriction = "";
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        when(mockedDB.nodeExists(parent)).thenReturn(true);
        when(mockedDB.nodeExists(address)).thenReturn(true);

        expectedException.expect(NodeAlreadyExistingException.class);
        // act
        nt.addNode(address, types, access, access, restriction, "myId", "TestValue");

        // assert/verify
        // nothing, check exception
    }

    /**
     * Test method for {@link NodeTree#addNode(String, List, List, List, String, String, String)}.
     * Test ParentNotExistingException.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws ParentNotExistingException
     *             tested.
     * @throws NodeAlreadyExistingException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testAddNodeParentNotExistingException() throws NodeAlreadyExistingException,
            ParentNotExistingException, InvalidValueException, NodeNotExistingException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String parent = "/" + localID + "/Hallo";
        final String restriction = "";
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        when(mockedDB.nodeExists(parent)).thenReturn(false);
        expectedException.expect(ParentNotExistingException.class);
        // act
        nt.addNode(address, types, access, access, restriction, "myId", "TestValue");

        // assert/verify
        // nothing, check exception
    }

    // /**
    // * Test method for {@link NodeTree#getAddressesOfType(String, String)}.
    // */
    // @Test
    // public final void testGetAddressesOfType() {
    // nt.getAddressesOfType("/", "basic/text");
    // verify(mockedDB).getAddressesOfType("/", "basic/text");
    // }
    //
    // /**
    // * Test method for {@link VslNodeTree#getHashOfSubtree(String, List)}.
    // */
    // @Test
    // public final void testGetHashOfSubtree() {
    // nt.getHashOfSubtree("/test/address", Arrays.asList("system"));
    // verify(mockedDB).getHashOfSubtree(eq("/test/address"), anyListOf(String.class));
    //
    // }

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

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)} . Tests getting a whole
     * subtree.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultiple()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String intermediaryAddress = address + "/intermediary";
        final String leafAddress = intermediaryAddress + "/leaf";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        // define Database behavior (mocked)
        final TreeMap<String, InternalNode> queryResult = new TreeMap<String, InternalNode>();
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final Date ts = new Date(System.currentTimeMillis());
        queryResult.put(address,
                new InternalNode(types, "TestResult", access, access, 0, ts, "", ""));
        queryResult.put(intermediaryAddress,
                new InternalNode(types, "intermediaryValue", access, access, 3, ts, "", ""));
        queryResult.put(leafAddress,
                new InternalNode(types, "leafValue", access, access, 5, ts, "", ""));
        when(mockedDB.getNodeRecord(address, params)).thenReturn(queryResult);

        // act
        final VslNode result = nt.get(address, params, id);

        // assert
        verify(id, times(9)).getAccessIDs();
        verify(mockedDB).getNodeRecord(address, params);
        assertThat(result.getValue(), is(equalTo("TestResult")));
        assertThat(result.getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getDirectChildren()), is(equalTo(1)));
        assertThat(getSize(result.getAllChildren()), is(equalTo(2)));
        assertThat(result.getVersion(), is(equalTo(0L)));
        assertThat(result.getTimestamp(), is(equalTo(ts)));

        assertThat(result.getChild("intermediary").getValue(), is(equalTo("intermediaryValue")));
        assertThat(result.getChild("intermediary").getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getAllChildren()), is(equalTo(1)));
        assertThat(result.getChild("intermediary").getVersion(), is(equalTo(3L)));
        assertThat(result.getChild("intermediary").getTimestamp(), is(equalTo(ts)));

        assertThat(result.getChild("intermediary").getChild("leaf").getValue(),
                is(equalTo("leafValue")));
        assertThat(result.getChild("intermediary").getChild("leaf").getTypes().toString(),
                is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getChild("leaf").getAllChildren()),
                is(equalTo(0)));
        assertThat(result.getChild("intermediary").getChild("leaf").getVersion(), is(equalTo(5L)));
        assertThat(result.getChild("intermediary").getChild("leaf").getTimestamp(),
                is(equalTo(ts)));

    }

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)}. Tests getting a whole
     * subtree. Test case that reader has access on no node.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultipleNoPermissionOnAll()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String intermediaryAddress = address + "/intermediary";
        final String leafAddress = intermediaryAddress + "/leaf";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        // define Database behavior (mocked)
        final TreeMap<String, InternalNode> queryResult = new TreeMap<String, InternalNode>();
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> invalidAccess = new LinkedList<String>();
        invalidAccess.add("ID2");
        queryResult.put(address, new InternalNode(types, "TestResult", invalidAccess, invalidAccess,
                0, null, "", ""));
        queryResult.put(intermediaryAddress, new InternalNode(types, "intermediaryValue",
                invalidAccess, invalidAccess, 0, null, "", ""));
        queryResult.put(leafAddress, new InternalNode(types, "leafValue", invalidAccess,
                invalidAccess, 0, null, "", ""));
        when(mockedDB.getNodeRecord(address, new AddressParameters().withDepth(-1)))
                .thenReturn(queryResult);

        expectedException.expect(NoPermissionException.class);
        // act
        nt.get(address, params, id);

        // assert
        // nothing, expect exception

    }

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)} . Tests getting a whole
     * subtree. Tests if intermediary nodes without read access are set to empty.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultipleNoPermissionOnIntermediary()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String intermediaryAddress = address + "/intermediary";
        final String leafAddress = intermediaryAddress + "/leaf";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        // define Database behavior (mocked)
        final TreeMap<String, InternalNode> queryResult = new TreeMap<String, InternalNode>();
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> invalidAccess = new LinkedList<String>();
        invalidAccess.add("ID2");
        queryResult.put(address,
                new InternalNode(types, "TestResult", access, access, 0, null, "", ""));
        queryResult.put(intermediaryAddress, new InternalNode(types, "intermediaryValue",
                invalidAccess, invalidAccess, 0, null, "", ""));
        queryResult.put(leafAddress,
                new InternalNode(types, "leafValue", access, access, 0, null, "", ""));
        when(mockedDB.getNodeRecord(address, params)).thenReturn(queryResult);

        // act
        final VslNode result = nt.get(address, params, id);

        // assert
        // first child should be empty, everything else ok.
        verify(id, times(10)).getAccessIDs();
        verify(mockedDB).getNodeRecord(address, params);
        assertThat(result.getValue(), is(equalTo("TestResult")));
        assertThat(result.getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getDirectChildren()), is(equalTo(1)));
        assertThat(getSize(result.getAllChildren()), is(equalTo(2)));

        assertThat(result.getChild("intermediary").getValue(), is(nullValue()));
        assertThat(result.getChild("intermediary").getTypes().toString(), is(equalTo("[]")));
        assertThat(result.getChild("intermediary").getTimestamp(), is(nullValue()));
        assertThat(result.getChild("intermediary").getVersion(), is(equalTo(-1L)));

        assertThat(getSize(result.getChild("intermediary").getAllChildren()), is(equalTo(1)));

        assertThat(result.getChild("intermediary").getChild("leaf").getValue(),
                is(equalTo("leafValue")));
        assertThat(result.getChild("intermediary").getChild("leaf").getTypes().toString(),
                is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getChild("leaf").getAllChildren()),
                is(equalTo(0)));
    }

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)}. Tests getting a whole
     * subtree. Test if leafs without read access are correctly ommited. *
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultipleNoPermissionOnLeaf()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String intermediaryAddress = address + "/intermediary";
        final String leafAddress = intermediaryAddress + "/leaf";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        // define Database behavior (mocked)
        final TreeMap<String, InternalNode> queryResult = new TreeMap<String, InternalNode>();
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> invalidAccess = new LinkedList<String>();
        invalidAccess.add("ID2");
        queryResult.put(address,
                new InternalNode(types, "TestResult", access, access, 0, null, "", ""));
        queryResult.put(intermediaryAddress,
                new InternalNode(types, "intermediaryValue", access, access, 0, null, "", ""));
        queryResult.put(leafAddress, new InternalNode(types, "leafValue", invalidAccess,
                invalidAccess, 0, null, "", ""));
        when(mockedDB.getNodeRecord(address, params)).thenReturn(queryResult);

        // act
        final VslNode result = nt.get(address, params, id);

        // assert
        // leaf should be missing (hiding structure), everything else ok.
        verify(id, times(8)).getAccessIDs();
        verify(mockedDB).getNodeRecord(address, params);
        assertThat(result.getValue(), is(equalTo("TestResult")));
        assertThat(result.getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getDirectChildren()), is(equalTo(1)));
        assertThat(getSize(result.getAllChildren()), is(equalTo(1))); // leaf should be missing now.

        assertThat(result.getChild("intermediary").getValue(), is(equalTo("intermediaryValue")));
        assertThat(result.getChild("intermediary").getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getAllChildren()), is(equalTo(0)));
    }

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)} . Tests getting a whole
     * subtree. Tests if root nodes without read access are set to empty.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultipleNoPermissionOnRoot()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final String intermediaryAddress = address + "/intermediary";
        final String leafAddress = intermediaryAddress + "/leaf";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        // define Database behavior (mocked)
        final TreeMap<String, InternalNode> queryResult = new TreeMap<String, InternalNode>();
        final LinkedList<String> types = new LinkedList<String>();
        types.add("/type/1");
        final LinkedList<String> invalidAccess = new LinkedList<String>();
        invalidAccess.add("ID2");
        queryResult.put(address, new InternalNode(types, "TestResult", invalidAccess, invalidAccess,
                0, null, "", ""));
        queryResult.put(intermediaryAddress,
                new InternalNode(types, "intermediaryValue", access, access, 0, null, "", ""));
        queryResult.put(leafAddress,
                new InternalNode(types, "leafValue", access, access, 0, null, "", ""));
        when(mockedDB.getNodeRecord(address, params)).thenReturn(queryResult);

        // act
        final VslNode result = nt.get(address, params, id);

        // assert
        // intermediary node should be empty, everything else ok.

        verify(id, times(10)).getAccessIDs();
        verify(mockedDB).getNodeRecord(address, params);
        assertThat(result.getValue(), is(nullValue()));
        assertThat(result.getTypes().toString(), is(equalTo("[]")));
        assertThat(result.getTimestamp(), is(nullValue()));
        assertThat(result.getVersion(), is(equalTo(-1L)));
        assertThat(getSize(result.getDirectChildren()), is(equalTo(1)));
        assertThat(getSize(result.getAllChildren()), is(equalTo(2)));

        assertThat(result.getChild("intermediary").getValue(), is(equalTo("intermediaryValue")));
        assertThat(result.getChild("intermediary").getTypes().toString(), is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getAllChildren()), is(equalTo(1)));

        assertThat(result.getChild("intermediary").getChild("leaf").getValue(),
                is(equalTo("leafValue")));
        assertThat(result.getChild("intermediary").getChild("leaf").getTypes().toString(),
                is(equalTo("[/type/1]")));
        assertThat(getSize(result.getChild("intermediary").getChild("leaf").getAllChildren()),
                is(equalTo(0)));
    }

    /**
     * Test method for {@link NodeTree#get(String, String, VslIdentity)} . Tests getting a whole
     * subtree
     *
     * @throws NodeNotExistingException
     *             under test.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws InvalidValueException
     *             shouldn't happen.
     */
    @Test
    public final void testGetValueMultipleRootNodeNotExisting()
            throws NoPermissionException, NodeNotExistingException, InvalidValueException {
        // arrange
        final String address = "/" + localID + "/Hallo/Welt";
        final VslAddressParameters params = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        doThrow(NodeNotExistingException.class).when(mockedDB).getNodeRecord(address, params);
        // set expected Exception.
        expectedException.expect(NodeNotExistingException.class);
        // act
        nt.get(address, params, id);
        // assert
        // nothing, expect exception
    }

    /**
     * Test method for {@link NodeTree#removeNode(java.lang.String)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen
     */
    @Test
    public final void testRemoveNode() throws NodeNotExistingException {
        final String address = "/" + localID + "/Hallo/Welt";
        when(mockedDB.nodeExists(address)).thenReturn(true);
        nt.removeNode(address);
        verify(mockedDB).removeNode(address);
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests what happens
     * if the value that should be written doesn't match the restriction.
     *
     * @throws InvalidValueException
     *             under test.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueInvalidValueException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set
        final String address = "/" + localID + "/Hallo/Welt";

        // initialize TestNode we want to set
        final VslNode node = nodeFactory.createImmutableLeaf("TestValue");
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address,
                new MetaNode(null, access, access, "regularExpression='NotTestValue'", ""));
        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);
        // set expected Exception.
        // also check message to make sure the correct exception is thrown (root node not any child)
        expectedException.expect(InvalidValueException.class);
        expectedException.expectMessage("Invalid value: " + node.getValue() + " for restriction: "
                + metanodes.get(address).getRestriction() + " at node: " + address);
        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests setting a
     * subtree. Tests what happens if the child value that should be written doesn't match the
     * restriction.
     *
     * @throws InvalidValueException
     *             under test.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueMultipleInvalidValueException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set
        final String address = "/" + localID + "/Hallo/Welt";
        final String childaddress = address + "/subnode";
        // initialize TestNode we want to set
        final Map<String, VslNode> nodes = new HashMap<String, VslNode>();
        nodes.put("", nodeFactory.createImmutableLeaf("TestValue"));
        nodes.put("subnode", nodeFactory.createImmutableLeaf("TestValue2"));
        final VslNode node = nodeFactory.createImmutableNode(nodes.entrySet());

        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        metanodes.put(childaddress,
                new MetaNode(null, access, access, "regularExpression='NotTestValue'", ""));

        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);
        // set expected Exception.
        // also check message to make sure the correct exception is thrown (root node not any child)
        expectedException.expect(InvalidValueException.class);
        expectedException.expectMessage("Invalid value: " + node.getChild("subnode").getValue()
                + " for restriction: " + metanodes.get(childaddress).getRestriction() + " at node: "
                + childaddress);
        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests setting a
     * subtree. Tests what happens if the child node doesn't exist.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             under test.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueMultipleNodeNotExistingException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set
        final String address = "/" + localID + "/Hallo/Welt";
        final String childaddress = address + "/subnode";

        // initialize TestNode we want to set
        final Map<String, VslNode> nodes = new HashMap<String, VslNode>();
        nodes.put("", nodeFactory.createImmutableLeaf("TestValue"));
        nodes.put("subnode", nodeFactory.createImmutableLeaf("TestValue2"));
        final VslNode node = nodeFactory.createImmutableNode(nodes.entrySet());
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        // return only root node, assume child doesn't exist
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);

        // set expected Exception.
        expectedException.expect(NodeNotExistingException.class);
        expectedException.expectMessage("Node " + childaddress + " not existing");
        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests setting a
     * subtree. Tests what happens if the child node can't be written due to the lack of access
     * rights.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             under test.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueMultipleNoPermissionException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set. Set different ids for node access and set
        // request.
        final String address = "/" + localID + "/Hallo/Welt";
        final String childaddress = address + "/subnode";

        final LinkedList<String> access2 = new LinkedList<String>();
        access2.add("ID2");

        // initialize TestNode we want to set
        final Map<String, VslNode> nodes = new HashMap<String, VslNode>();
        nodes.put("", nodeFactory.createImmutableLeaf("TestValue"));
        nodes.put("subnode", nodeFactory.createImmutableLeaf("TestValue2"));
        final VslNode node = nodeFactory.createImmutableNode(nodes.entrySet());
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        metanodes.put(childaddress, new MetaNode(null, access2, access2, "", ""));

        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);
        // set expected Exception.
        // also check message to make sure the correct exception is thrown (root node not any child)
        expectedException.expect(NoPermissionException.class);
        expectedException
                .expectMessage("[ID1] cannot write node at " + childaddress + ", nothing changed.");

        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)} . Tests setting a
     * subtree.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueMultipleSuccess() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set
        final String address = "/" + localID + "/Hallo/Welt";
        final String childaddress = address + "/subnode";

        // initialize TestNode we want to set
        final Map<String, VslNode> nodes = new HashMap<String, VslNode>();
        nodes.put("", nodeFactory.createImmutableLeaf("TestValue"));
        nodes.put("subnode", nodeFactory.createImmutableLeaf("TestValue2"));
        final VslNode node = nodeFactory.createImmutableNode(nodes.entrySet());

        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        metanodes.put(childaddress, new MetaNode(null, access, access, "", ""));

        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);

        // act
        nt.setValue(address, id, node);

        // assert/verify
        verify(id, times(2)).getAccessIDs();
        verify(mockedDB).getNodeMetaData(address, true);
        verify(mockedDB, times(1)).setValueTree(Matchers.<Map<String, String>>any());
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests what happens
     * if the node doesn't exist.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             under test.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueNodeNotExistingException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address and Node to set
        final String address = "/" + localID + "/Hallo/Welt";

        // initialize TestNode we want to set
        final VslNode node = nodeFactory.createImmutableLeaf("TestValue");
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        doThrow(NodeNotExistingException.class).when(mockedDB).getNodeMetaData(address, true);
        // set expected Exception.
        expectedException.expect(NodeNotExistingException.class);
        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)}. Tests what happens
     * if the node can't be written due to the lack of access rights.
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             under test.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueNoPermissionException() throws NoPermissionException,
            NodeNotExistingException, InvalidValueException, NodeLockedException {
        // arrange
        // initialize address, access ids and Node to set. Set different ids for node access and set
        // request.
        final String address = "/" + localID + "/Hallo/Welt";
        final LinkedList<String> access2 = new LinkedList<String>();
        access2.add("ID2");
        // initialize TestNode we want to set
        final VslNode node = nodeFactory.createImmutableLeaf("TestValue");
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access2, access2, "", ""));
        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);
        // set expected Exception.
        // also check message to make sure the correct exception is thrown (root node not any child)
        expectedException.expect(NoPermissionException.class);
        expectedException
                .expectMessage("[ID1] cannot write node at " + address + ", nothing changed.");

        // act
        nt.setValue(address, id, node);

        // assert/verify
        // nothing here, done with the expectedException before act.
    }

    /**
     * Test method for {@link NodeTree#setValue(String, VslIdentity, VslNode)} .
     *
     * @throws InvalidValueException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NoPermissionException
     *             shouldn't happen.
     * @throws NodeLockedException
     *             shouldn't happen.
     */
    @Test
    public final void testSetValueSuccess() throws NoPermissionException, NodeNotExistingException,
            InvalidValueException, NodeLockedException {
        // arrange
        // initialize address and Node to set
        final String address = "/" + localID + "/Hallo/Welt";

        // initialize TestNode we want to set
        final VslNode node = nodeFactory.createImmutableLeaf("TestValue");
        // define Database behavior (mocked)
        final TreeMap<String, MetaNode> metanodes = new TreeMap<String, MetaNode>();
        metanodes.put(address, new MetaNode(null, access, access, "", ""));
        when(mockedDB.getNodeMetaData(address, true)).thenReturn(metanodes);

        // act
        nt.setValue(address, id, node);

        // assert/verify
        verify(id).getAccessIDs();
        verify(mockedDB).getNodeMetaData(address, true);
        verify(mockedDB).setValueTree(Matchers.<Map<String, String>>any());
    }
}
