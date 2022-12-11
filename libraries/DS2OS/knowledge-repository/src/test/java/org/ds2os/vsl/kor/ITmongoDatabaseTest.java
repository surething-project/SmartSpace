package org.ds2os.vsl.kor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.kor.dataStructures.MetaNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test for {@link VslNodeDatabase} using the specific implementation
 * {@link MongodbDatabase}.
 *
 * @author liebald
 * @author gogrichiani
 */
// disabled by default, since only working when mongoDB is actually used/configured
@Ignore
public class ITmongoDatabaseTest {

    /**
     * The database used for tests.
     */
    private VslNodeDatabase db;

    /**
     * Address of the localKA node.
     */
    private final String localKA = "/localKA";

    /**
     * Generic ReaderIDs for the tests.
     */
    private ArrayList<String> readers;

    /**
     * Address of the service1.
     */
    private final String service1 = "/localKA/s1";

    /**
     * Address of the service1.
     */
    private final String service2 = "/localKA/s2";

    /**
     * Generic nodeTypes for the tests.
     */
    private ArrayList<String> types;

    /**
     * Generic WriterIDs for the tests.
     */
    private ArrayList<String> writers;

    private VslAddressParameters paramsIncludeSubtree;
    private VslAddressParameters paramsExcludeSubtree;

    private VslAddressParameters paramsOnlyDirectSubnodes;
    private VslAddressParameters paramsVersionRetrieval;

    /**
     * Config mock used for the database.
     */
    VslKORDatabaseConfig configMock;

    /**
     * create a NodeDatabase for the tests.
     */
    @Before
    public final void setUp() {
        paramsIncludeSubtree = new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        paramsExcludeSubtree = new AddressParameters().withDepth(0)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);
        paramsOnlyDirectSubnodes = new AddressParameters().withDepth(1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE);

        configMock = mock(VslKORDatabaseConfig.class);
        when(configMock.isDatabasePersistent()).thenReturn(false);
        when(configMock.getDatabasePath()).thenReturn("hsqldb/db");
        when(configMock.getDatabaseUsername()).thenReturn("ds2os");
        when(configMock.getDatabaseType()).thenReturn("mongodb");
        when(configMock.getDatabasePassword()).thenReturn("thisMustBeSecure");
        when(configMock.isArchiveEnabled()).thenReturn(true);
        when(configMock.getArchiveNodeVersionLimit()).thenReturn(10);
        when(configMock.getDatabaseMemoryMode()).thenReturn("CACHED");
        when(configMock.getDatabaseMaxValueLength()).thenReturn("16M");

        // Quick connection test to fail early if no local MongoDB is there
        try {
            new Socket("localhost", 27017).close();
        } catch (final IOException e) {
            fail("Mongodb instance on localhost:27017 is not available");
        }

        db = new MongodbDatabase(configMock);
        db.activate();
        types = new ArrayList<String>();
        types.add("/type1");
        types.add("/type2");

        readers = new ArrayList<String>();
        readers.add("ID1");
        readers.add("ID2");

        writers = new ArrayList<String>();
        writers.add("ID1");
        writers.add("ID2");
    }

    /**
     * Test method for {@link VslNodeDatabase#addNode(String, List, List, List, String, String)} .
     *
     * @throws NodeNotExistingException
     *             shouldn't happen (from nodeExists()).
     */
    @Test
    public final void testAddNode() throws NodeNotExistingException {

        db.addNode(localKA, types, readers, writers, "regularExpression='(a|b)c'", "TTL='12'");

        assertThat(db.nodeExists(localKA), is(equalTo(true)));
        // assertThat(db.getNumberOfNodes(), is(equalTo(1))); // the just added localKA node
        final InternalNode testNode = db.getNodeRecord(localKA, paramsExcludeSubtree).get(localKA);
        assertThat(testNode.getValue(), is(nullValue()));
        assertThat(testNode.getVersion(), is(equalTo(-1L)));
        assertThat(testNode.getType().toString(), is(equalTo(types.toString())));
        assertThat(testNode.getReaderIDs().toString(), is(equalTo(readers.toString())));
        assertThat(testNode.getWriterIDs().toString(), is(equalTo(writers.toString())));
        assertThat(testNode.getRestriction(), is(equalTo("regularExpression='(a|b)c'")));
        assertThat(testNode.getCacheParameters(), is(equalTo("TTL='12'")));

    }

    /**
     * Test method for {@link VslNodeDatabase#getAddressesOfType(String, String)} .
     */
    @Test
    public final void testGetAddressesOfType() {
        final LinkedList<String> matchingTypes = new LinkedList<String>(types);
        matchingTypes.add("/basic/text");
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, matchingTypes, readers, writers, "regularExpression='serviceValue'",
                "");
        db.addNode(service1 + "/a", types, readers, writers, "minValue='4'", "");
        db.addNode(service1 + "/b", matchingTypes, readers, writers, "minValue='4'", "");
        db.addNode(service1 + "/c", types, readers, writers, "minValue='4'", "");
        db.addNode(service1 + "/d", matchingTypes, readers, writers, "minValue='4'", "");

        final List<String> addressesBelowService = db.getAddressesOfType(service1 + "/",
                "/basic/text");
        final List<String> addressesFromRoot = db.getAddressesOfType("/", "/basic/text");

        assertThat(addressesBelowService.size(), is(equalTo(2)));
        assertThat(addressesBelowService.contains(service1 + "/b"), is(equalTo(true)));
        assertThat(addressesBelowService.contains(service1 + "/d"), is(equalTo(true)));

        assertThat(addressesFromRoot.size(), is(equalTo(3)));
        assertThat(addressesFromRoot.contains(service1), is(equalTo(true)));
        assertThat(addressesFromRoot.contains(service1 + "/b"), is(equalTo(true)));
        assertThat(addressesFromRoot.contains(service1 + "/d"), is(equalTo(true)));
    }

    // /**
    // * Test method for {@link VslNodeDatabase#getAllChildAddresses(String)} .
    // *
    // * @throws NodeNotExistingException
    // * shouldnt happen (from getNodeMetadata from getAllChildAddresses)
    // */
    // @Test
    // public final void testGetAllChildAddresses() throws NodeNotExistingException {
    // final String service1child = service1 + "/a";
    //
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "", "");
    // db.addNode(service2, types, readers, writers, "", "");
    // db.addNode(service1child, types, readers, writers, "", "");
    //
    // assertThat(db.getAllChildAddresses(localKA).toString(),
    // is(equalTo("[" + service1 + ", " + service1child + ", " + service2 + "]")));
    // assertThat(db.getAllChildAddresses(service1).toString(),
    // is(equalTo("[" + service1child + "]")));
    // assertThat(db.getAllChildAddresses(service2).toString(), is(equalTo("[]")));
    // }

    // /**
    // * Test method for {@link VslNodeDatabase#getDirectChildrenAddresses(String)}.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen.
    // */
    // @Test
    // public final void testGetDirectChildrenAddresses() throws NodeNotExistingException {
    // final String service1child = service1 + "/a";
    //
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "", "");
    // db.addNode(service2, types, readers, writers, "", "");
    // db.addNode(service1child, types, readers, writers, "", "");
    //
    // assertThat(db.getDirectChildrenAddresses(localKA).toString(),
    // is(equalTo("[" + service1 + ", " + service2 + "]")));
    // assertThat(db.getDirectChildrenAddresses(service1).toString(),
    // is(equalTo("[" + service1child + "]")));
    // assertThat(db.getDirectChildrenAddresses(service2).toString(), is(equalTo("[]")));
    // }

    /**
     * Test method for {@link VslNodeDatabase#getHashOfSubtree(String, List)}.
     */
    @Test
    public final void testGetHashOfSubtree() {
        db.addNode("/", Arrays.asList("/treeRoot"), readers, writers, "", "");
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");

        final String hash0 = db.getHashOfSubtree("/", Collections.<String>emptyList());
        final String hash1 = db.getHashOfSubtree(localKA, Collections.<String>emptyList());
        final String hash2 = db.getHashOfSubtree(service1, Collections.<String>emptyList());
        final String hash3 = db.getHashOfSubtree(service2, Collections.<String>emptyList());
        final String hash4 = db.getHashOfSubtree(localKA,
                Arrays.asList(service2.substring(service2.lastIndexOf("/") + 1)));
        assertThat(hash0, is(equalTo("fff3136b71500ffc")));
        assertThat(hash1, is(equalTo("200c8b0b88aa")));
        assertThat(hash2, is(equalTo("fffffff2ec411d0c")));
        assertThat(hash3, is(equalTo("11")));
        assertThat(hash4, is(equalTo(hash1)));

        db.addNode(service2, types, readers, writers, "", "");
        final String hash01 = db.getHashOfSubtree("/", Collections.<String>emptyList());
        final String hash11 = db.getHashOfSubtree(localKA, Collections.<String>emptyList());
        final String hash21 = db.getHashOfSubtree(service1, Collections.<String>emptyList());
        final String hash31 = db.getHashOfSubtree(service2, Collections.<String>emptyList());
        final String hash41 = db.getHashOfSubtree(localKA,
                Arrays.asList(service2.substring(service2.lastIndexOf("/") + 1)));
        assertThat(hash01, is(equalTo("bae2d983e1663ce4")));
        assertThat(hash11, is(equalTo("ab63067cee9762")));
        assertThat(hash21, is(equalTo(hash2)));
        assertThat(hash31, is(equalTo("fffffff2ec411d31")));
        assertThat(hash41, is(equalTo(hash4)));

    }

    /**
     * Test method for {@link VslNodeDatabase#getNodeMetaData(String, boolean)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testGetNodeMetaData() throws NodeNotExistingException {
        final String subnode = service1 + "/a";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'",
                "TTL='12'");
        db.addNode(subnode, types, readers, writers, "minValue='4'", "");

        final TreeMap<String, MetaNode> result = db.getNodeMetaData(localKA, true);

        assertThat(result.size(), is(equalTo(3)));

        assertThat(result.get(localKA).getReaderIDs().toString(), is(equalTo(readers.toString())));
        assertThat(result.get(localKA).getWriterIDs().toString(), is(equalTo(writers.toString())));
        assertThat(result.get(localKA).getType().toString(), is(equalTo(types.toString())));
        assertThat(result.get(localKA).getRestriction(), is(equalTo("")));

        assertThat(result.get(service1).getReaderIDs().toString(), is(equalTo(readers.toString())));
        assertThat(result.get(service1).getWriterIDs().toString(), is(equalTo(writers.toString())));
        assertThat(result.get(service1).getType().toString(), is(equalTo(types.toString())));
        assertThat(result.get(service1).getRestriction(),
                is(equalTo("regularExpression='serviceValue'")));
        assertThat(result.get(service1).getCacheParameters(), is(equalTo("TTL='12'")));

        assertThat(result.get(subnode).getReaderIDs().toString(), is(equalTo(readers.toString())));
        assertThat(result.get(subnode).getWriterIDs().toString(), is(equalTo(writers.toString())));
        assertThat(result.get(subnode).getType().toString(), is(equalTo(types.toString())));
        assertThat(result.get(subnode).getRestriction(), is(equalTo("minValue='4'")));
    }

    /**
     * Test method for {@link VslNodeDatabase#getNodeMetaData(String, boolean)}. Tests
     * NodeNotExistingException;
     *
     * @throws NodeNotExistingException
     *             tested.
     */
    @Test(expected = NodeNotExistingException.class)
    public final void testGetNodeMetaDataNodeNotExisting() throws NodeNotExistingException {
        db.getNodeMetaData(localKA, true);
    }

    /**
     * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testGetNodeRecord() throws NodeNotExistingException {
        final String subnode = service1 + "/a";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'",
                "TTL='5'");
        db.addNode(subnode, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
        final long time = System.currentTimeMillis();
        nodesToSet.put(subnode, "subValue");
        db.setValueTree(nodesToSet);

        nodesToSet.clear();
        nodesToSet.put(service1, "serviceValue");
        db.setValueTree(nodesToSet);

        final TreeMap<String, InternalNode> testNodes = db.getNodeRecord(service1,
                paramsExcludeSubtree);
        assertThat(testNodes.size(), is(equalTo(1)));
        assertThat(testNodes.get(service1).getReaderIDs().toString(),
                is(equalTo(readers.toString())));
        assertThat(testNodes.get(service1).getWriterIDs().toString(),
                is(equalTo(writers.toString())));
        assertThat(testNodes.get(service1).getType().toString(), is(equalTo(types.toString())));
        assertThat(testNodes.get(service1).getValue(), is(equalTo("serviceValue")));
        assertThat(testNodes.get(service1).getVersion(), is(equalTo(1L)));
        assertThat(testNodes.get(service1).getRestriction(),
                is(equalTo("regularExpression='serviceValue'")));
        assertThat(testNodes.get(service1).getCacheParameters(), is(equalTo("TTL='5'")));
        assertThat((double) (time - testNodes.get(service1).getTimestamp().getTime()),
                is(closeTo(0, 1000))); // we assume a difference of 1 second in the timestamp is
                                       // ok
    }

    // /**
    // * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen.
    // */
    // @Test
    // @Ignore
    // public final void testGetNodeRecordByTimestamp() throws NodeNotExistingException {
    // // TODO: temporarily disabled the test while versioning is getting fixed
    // final String subnode = service1 + "/a";
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
    // db.addNode(subnode, types, readers, writers, "", "");
    //
    // final Timestamp ts1 = new Timestamp(System.currentTimeMillis());
    // delay(100);
    // db.setValue(service1, "service1"); // service1 v=0, subnode v=null
    // db.setValue(subnode, "sub1"); // service1 v=1, subnode v=0
    // delay(100);
    // final Timestamp ts3 = new Timestamp(System.currentTimeMillis());
    // delay(100);
    // db.setValue(service1, "service2"); // service1 v=2, subnode v=0
    // delay(100);
    // final Timestamp ts4 = new Timestamp(System.currentTimeMillis());
    // delay(100);
    // db.setValue(subnode, "sub2"); // service1 v=3, subnode v=1
    // delay(100);
    // final Timestamp ts5 = new Timestamp(System.currentTimeMillis());
    // delay(100);
    // db.setValue(subnode, "sub3"); // service1 v=4, subnode v=2
    // db.setValue(service1, "service3"); // service1 v=5, subnode v=2
    // delay(100);
    // final Timestamp ts7 = new Timestamp(System.currentTimeMillis());
    //
    // final TreeMap<String, InternalNode> nodesV1 = db.getNodeRecord(service1, true, ts1, ts3);
    // final TreeMap<String, InternalNode> nodesV2 = db.getNodeRecord(service1, true, ts3, ts4);
    // final TreeMap<String, InternalNode> nodesV3 = db.getNodeRecord(service1, true, ts3, ts5);
    // final TreeMap<String, InternalNode> nodesV4 = db.getNodeRecord(service1, true, ts3, ts7);
    // final TreeMap<String, InternalNode> nodesV5 = db.getNodeRecord(service1, true, ts1, ts7);
    // final TreeMap<String, InternalNode> nodesV6 = db.getNodeRecord(service1, true, null, ts4);
    // final TreeMap<String, InternalNode> nodesV7 = db.getNodeRecord(service1, true, ts1, null);
    // final TreeMap<String, InternalNode> nodesV8 = db.getNodeRecord(service1, true, null, null);
    //
    // assertThat(nodesV1.size(), is(equalTo(2)));
    // assertThat(nodesV2.size(), is(equalTo(2)));
    // assertThat(nodesV3.size(), is(equalTo(2)));
    // assertThat(nodesV5.size(), is(equalTo(2)));
    //
    // assertThat(nodesV1.get(service1).getValue(), is(equalTo("service1")));
    // assertThat(nodesV1.get(service1).getVersion(), is(equalTo(1L)));
    // assertThat(nodesV1.get(subnode).getValue(), is(equalTo("sub1")));
    // assertThat(nodesV1.get(subnode).getVersion(), is(equalTo(0L)));
    //
    // assertThat(nodesV2.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV2.get(service1).getVersion(), is(equalTo(2L)));
    // assertThat(nodesV2.get(subnode).getValue(), is(equalTo("sub1")));
    // assertThat(nodesV2.get(subnode).getVersion(), is(equalTo(0L)));
    //
    // assertThat(nodesV3.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV3.get(service1).getVersion(), is(equalTo(3L)));
    // assertThat(nodesV3.get(subnode).getValue(), is(equalTo("sub2")));
    // assertThat(nodesV3.get(subnode).getVersion(), is(equalTo(1L)));
    //
    // assertThat(nodesV4.get(service1).getValue(), is(equalTo("service3")));
    // assertThat(nodesV4.get(service1).getVersion(), is(equalTo(5L)));
    // assertThat(nodesV4.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV4.get(subnode).getVersion(), is(equalTo(2L)));
    //
    // assertThat(nodesV5.get(service1).getValue(), is(equalTo("service3")));
    // assertThat(nodesV5.get(service1).getVersion(), is(equalTo(5L)));
    // assertThat(nodesV5.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV5.get(subnode).getVersion(), is(equalTo(2L)));
    //
    // assertThat(nodesV6.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV6.get(service1).getVersion(), is(equalTo(2L)));
    // assertThat(nodesV6.get(subnode).getValue(), is(equalTo("sub1")));
    // assertThat(nodesV6.get(subnode).getVersion(), is(equalTo(0L)));
    //
    // assertThat(nodesV7.get(service1).getValue(), is(equalTo("service3")));
    // assertThat(nodesV7.get(service1).getVersion(), is(equalTo(5L)));
    // assertThat(nodesV7.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV7.get(subnode).getVersion(), is(equalTo(2L)));
    //
    // assertThat(nodesV8.get(service1).getValue(), is(equalTo("service3")));
    // assertThat(nodesV8.get(service1).getVersion(), is(equalTo(5L)));
    // assertThat(nodesV8.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV8.get(subnode).getVersion(), is(equalTo(2L)));
    // }
    //
    // /**
    // * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen.
    // */
    // @Test
    // public final void testGetNodeRecordByTimestampNodeNotExistingException()
    // throws NodeNotExistingException {
    // final String subnode = service1 + "/a";
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
    // db.addNode(subnode, types, readers, writers, "", "");
    //
    // db.setValue(service1, "service1"); // service1 v=0, subnode v=null
    // db.setValue(subnode, "sub1"); // service1 v=1, subnode v=0
    //
    // delay(1); // make sure the timespan is out of range
    // final Timestamp ts1 = new Timestamp(System.currentTimeMillis());
    // delay(50);
    //
    // final Timestamp ts2 = new Timestamp(System.currentTimeMillis());
    //
    // expectedException.expect(NodeNotExistingException.class);
    //
    // db.getNodeRecord(service1, true, ts1, ts2);
    // }
    //
    // /**
    // * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen.
    // */
    // @Test
    // public final void testGetNodeRecordByVersion() throws NodeNotExistingException {
    // final String subnode = service1 + "/a";
    //
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
    // db.addNode(subnode, types, readers, writers, "", "");
    //
    // db.setValue(service1, "service1"); // service1 v=0, subnode v=null
    // db.setValue(subnode, "sub1"); // service1 v=1, subnode v=0
    // db.setValue(service1, "service2"); // service1 v=2, subnode v=0
    // db.setValue(subnode, "sub2"); // service1 v=3, subnode v=1
    // db.setValue(subnode, "sub3"); // service1 v=4, subnode v=2
    // db.setValue(service1, "service3"); // service1 v=5, subnode v=2
    // final TreeMap<String, InternalNode> nodesV1 = db.getNodeRecord(service1, true, 1);
    // final TreeMap<String, InternalNode> nodesV2 = db.getNodeRecord(service1, true, 2);
    // final TreeMap<String, InternalNode> nodesV3 = db.getNodeRecord(service1, true, 3);
    // final TreeMap<String, InternalNode> nodesV4 = db.getNodeRecord(service1, true, 4);
    // final TreeMap<String, InternalNode> nodesV5 = db.getNodeRecord(service1, true, 5);
    //
    // assertThat(nodesV1.size(), is(equalTo(2)));
    // assertThat(nodesV2.size(), is(equalTo(2)));
    // assertThat(nodesV3.size(), is(equalTo(2)));
    // assertThat(nodesV5.size(), is(equalTo(2)));
    //
    // assertThat(nodesV1.get(service1).getValue(), is(equalTo("service1")));
    // assertThat(nodesV1.get(service1).getVersion(), is(equalTo(1L)));
    // assertThat(nodesV1.get(subnode).getValue(), is(equalTo("sub1")));
    // assertThat(nodesV1.get(subnode).getVersion(), is(equalTo(0L)));
    //
    // assertThat(nodesV2.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV2.get(service1).getVersion(), is(equalTo(2L)));
    // assertThat(nodesV2.get(subnode).getValue(), is(equalTo("sub1")));
    // assertThat(nodesV2.get(subnode).getVersion(), is(equalTo(0L)));
    //
    // assertThat(nodesV3.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV3.get(service1).getVersion(), is(equalTo(3L)));
    // assertThat(nodesV3.get(subnode).getValue(), is(equalTo("sub2")));
    // assertThat(nodesV3.get(subnode).getVersion(), is(equalTo(1L)));
    //
    // assertThat(nodesV4.get(service1).getValue(), is(equalTo("service2")));
    // assertThat(nodesV4.get(service1).getVersion(), is(equalTo(4L)));
    // assertThat(nodesV4.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV4.get(subnode).getVersion(), is(equalTo(2L)));
    //
    // assertThat(nodesV5.get(service1).getValue(), is(equalTo("service3")));
    // assertThat(nodesV5.get(service1).getVersion(), is(equalTo(5L)));
    // assertThat(nodesV5.get(subnode).getValue(), is(equalTo("sub3")));
    // assertThat(nodesV5.get(subnode).getVersion(), is(equalTo(2L)));
    //
    // }
    //
    // /**
    // * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen.
    // */
    // @Test
    // public final void testGetNodeRecordByVersionNodeNotExistingException()
    // throws NodeNotExistingException {
    // final String subnode = service1 + "/a";
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
    // db.addNode(subnode, types, readers, writers, "", "");
    //
    // db.setValue(service1, "service1"); // service1 v=0, subnode v=null
    // db.setValue(subnode, "sub1"); // service1 v=1, subnode v=0
    //
    // expectedException.expect(NodeNotExistingException.class);
    //
    // db.getNodeRecord(service1, true, 6);
    // }

    /**
     * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testGetNodeRecordNewestVersion() throws NodeNotExistingException {
        final String subnode = service1 + "/a";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
        db.addNode(subnode, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
        nodesToSet.put(service1, "service1");
        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(subnode, "sub1");
        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(service1, "service2");
        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(subnode, "sub2");
        db.setValueTree(nodesToSet);

        final TreeMap<String, InternalNode> testNodes = db.getNodeRecord(service1,
                paramsIncludeSubtree);
        assertThat(testNodes.size(), is(equalTo(2)));
        assertThat(testNodes.get(service1).getReaderIDs().toString(),
                is(equalTo(readers.toString())));
        assertThat(testNodes.get(service1).getWriterIDs().toString(),
                is(equalTo(writers.toString())));
        assertThat(testNodes.get(service1).getType().toString(), is(equalTo(types.toString())));
        assertThat(testNodes.get(service1).getValue(), is(equalTo("service2")));
        assertThat(testNodes.get(service1).getVersion(), is(equalTo(3L)));
        assertThat(testNodes.get(service1).getRestriction(),
                is(equalTo("regularExpression='serviceValue'")));

        assertThat(testNodes.get(subnode).getReaderIDs().toString(),
                is(equalTo(readers.toString())));
        assertThat(testNodes.get(subnode).getWriterIDs().toString(),
                is(equalTo(writers.toString())));
        assertThat(testNodes.get(subnode).getType().toString(), is(equalTo(types.toString())));
        assertThat(testNodes.get(subnode).getValue(), is(equalTo("sub2")));
        assertThat(testNodes.get(subnode).getVersion(), is(equalTo(1L)));
        assertThat(testNodes.get(subnode).getRestriction(), is(equalTo("")));

    }

    /**
     * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}. Test the
     * NodeNotExistingException.
     *
     * @throws NodeNotExistingException
     *             is tested.
     */
    @Test(expected = NodeNotExistingException.class)
    public final void testGetNodeRecordNodeNotExistingException() throws NodeNotExistingException {
        db.getNodeRecord(service1, paramsIncludeSubtree);
    }

    /**
     * Test method for {@link VslNodeDatabase#getNodeRecord(String, boolean)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testGetNodeRecordSubtree() throws NodeNotExistingException {
        final String subnode = service1 + "/a";
        final String subsubnode = subnode + "/ultralamp";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
        db.addNode(subnode, types, readers, writers, "", "");
        db.addNode(subsubnode, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();

        final long time = System.currentTimeMillis();
        nodesToSet.put(service1, "serviceValue");
        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(subnode, "subValue");
        db.setValueTree(nodesToSet);

        nodesToSet.clear();
        nodesToSet.put(subsubnode, "bright");
        db.setValueTree(nodesToSet);

        final TreeMap<String, InternalNode> testNodes = db.getNodeRecord(service1,
                paramsIncludeSubtree);
        assertThat(testNodes.size(), is(equalTo(3)));
        assertThat(testNodes.get(service1).getReaderIDs().toString(),
                is(equalTo(readers.toString())));
        assertThat(testNodes.get(service1).getWriterIDs().toString(),
                is(equalTo(writers.toString())));
        assertThat(testNodes.get(service1).getType().toString(), is(equalTo(types.toString())));
        assertThat(testNodes.get(service1).getValue(), is(equalTo("serviceValue")));
        assertThat(testNodes.get(service1).getVersion(), is(equalTo(2L)));
        assertThat(testNodes.get(service1).getRestriction(),
                is(equalTo("regularExpression='serviceValue'")));
        assertThat((double) (time - testNodes.get(service1).getTimestamp().getTime()),
                is(closeTo(0, 500))); // we assume a difference of 0.5 second in the timestamp is ok

        assertThat(testNodes.get(subnode).getReaderIDs().toString(),
                is(equalTo(readers.toString())));
        assertThat(testNodes.get(subnode).getWriterIDs().toString(),
                is(equalTo(writers.toString())));
        assertThat(testNodes.get(subnode).getType().toString(), is(equalTo(types.toString())));
        assertThat(testNodes.get(subnode).getValue(), is(equalTo("subValue")));
        assertThat(testNodes.get(subnode).getVersion(), is(equalTo(1L)));
        assertThat(testNodes.get(subnode).getRestriction(), is(equalTo("")));
        assertThat((double) (time - testNodes.get(subnode).getTimestamp().getTime()),
                is(closeTo(0, 500))); // we assume a difference of 0.5 second in the timestamp is ok

        assertThat(testNodes.get(subsubnode).getValue(), is(equalTo("bright")));
        assertThat(testNodes.get(subsubnode).getVersion(), is(equalTo(0L)));

        final String nodeThirdLevel = subsubnode + "/bulb";
        db.addNode(nodeThirdLevel, types, readers, writers, "", "");

        nodesToSet.clear();
        nodesToSet.put(nodeThirdLevel, "1000_lumens");
        db.setValueTree(nodesToSet);

        assertThat(db.getNodeRecord(service1, paramsOnlyDirectSubnodes).size(), is(equalTo(2)));

        final TreeMap<String, InternalNode> updatedTestNodes = db.getNodeRecord(service1,
                paramsIncludeSubtree);
        assertThat(updatedTestNodes.size(), is(equalTo(4)));

        assertThat(updatedTestNodes.get(subnode).getVersion(), is(equalTo(2L)));

        assertThat(updatedTestNodes.get(subsubnode).getVersion(), is(equalTo(1L)));
        assertThat(updatedTestNodes.get(subsubnode).getValue(), is(equalTo("bright")));

        assertThat(updatedTestNodes.get(nodeThirdLevel).getVersion(), is(equalTo(0L)));
        assertThat(updatedTestNodes.get(nodeThirdLevel).getValue(), is(equalTo("1000_lumens")));

    }

    /**
     * Test method to retrieval of a node with specific version.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testVersionRetrieval() throws NodeNotExistingException {

        paramsVersionRetrieval = new AddressParameters().withDepth(1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE).withVersion(1);
        final VslAddressParameters paramsVersionRetrievalWholeSubtree = new AddressParameters()
                .withDepth(-1).withNodeInformationScope(NodeInformationScope.COMPLETE)
                .withVersion(1);

        final String subnode = service1 + "/a";
        final String subsubnode = subnode + "/ultralamp";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "regularExpression='serviceValue'", "");
        db.addNode(subnode, types, readers, writers, "", "");
        db.addNode(subsubnode, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();

        // final long time = System.currentTimeMillis();
        nodesToSet.put(service1, "serviceValue");
        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(subnode, "subValue");
        db.setValueTree(nodesToSet);

        nodesToSet.clear();
        nodesToSet.put(subsubnode, "bright");
        db.setValueTree(nodesToSet);

        final String nodeThirdLevel = subsubnode + "/bulb";
        db.addNode(nodeThirdLevel, types, readers, writers, "", "");

        nodesToSet.clear();
        nodesToSet.put(nodeThirdLevel, "1000_lumens");
        db.setValueTree(nodesToSet);

        TreeMap<String, InternalNode> testNodes = db.getNodeRecord(service1,
                paramsVersionRetrieval);
        System.out.println(testNodes.toString());

        assertThat(testNodes.size(), is(equalTo(2)));

        testNodes = db.getNodeRecord(service1, paramsVersionRetrievalWholeSubtree);

        System.out.println(testNodes.toString());

        assertThat(testNodes.size(), is(equalTo(4)));

    }

    // /**
    // * Test method for {@link VslNodeDatabase#getNumberOfNodes()}.
    // */
    // @Test
    // public final void testGetNumberOfNodes() {
    // final String service1child = service1 + "/a";
    // assertThat(db.getNumberOfNodes(), is(equalTo(0)));
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "", "");
    // assertThat(db.getNumberOfNodes(), is(equalTo(2)));
    // db.addNode(service2, types, readers, writers, "", "");
    // db.addNode(service1child, types, readers, writers, "", "");
    // assertThat(db.getNumberOfNodes(), is(equalTo(4)));
    //
    // }

    /**
     * Test method for {@link VslNodeDatabase#nodeExists(String)}.
     */
    @Test
    public final void testNodeExists() {
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");

        assertThat(db.nodeExists(localKA), is(equalTo(true)));
        assertThat(db.nodeExists(service1), is(equalTo(true)));
        assertThat(db.nodeExists(service2), is(equalTo(false)));

    }

    // /**
    // * Test method for {@link VslNodeDatabase#removeNode(String)}.
    // */
    // @Test
    // public final void testRemoveNode() {
    // db.addNode(localKA, types, readers, writers, "", "");
    // assertThat(db.getNumberOfNodes(), is(equalTo(1)));
    // assertThat(db.nodeExists(localKA), is(equalTo(true)));
    // db.removeNode(localKA);
    // assertThat(db.getNumberOfNodes(), is(equalTo(0)));
    // assertThat(db.nodeExists(localKA), is(equalTo(false)));
    // }

    /**
     * Test method for {@link VslNodeDatabase#setValueTree(Map)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen
     */
    @Test
    public final void testSetValueTree() throws NodeNotExistingException {
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");
        db.addNode(service1 + "/subnode1", types, readers, writers, "", "");
        db.addNode(service1 + "/subnode2", types, readers, writers, "", "");
        db.addNode(service1 + "/subnode1/subnode11", types, readers, writers, "", "");

        Map<String, String> toSet = new HashMap<String, String>();
        toSet.put(service1, "service1");
        toSet.put(service1 + "/subnode1", "subnode1");
        toSet.put(service1 + "/subnode1/subnode11", "subnode11");
        toSet.put(service1 + "/subnode2", "subnode2");
        db.setValueTree(toSet);
        toSet = new HashMap<String, String>();
        toSet.put(service1 + "/subnode2", "subnode2.1");
        toSet.put(service1 + "/subnode1/subnode11", "subnode11.1");
        db.setValueTree(toSet);

        final TreeMap<String, InternalNode> testNode = db.getNodeRecord(service1,
                paramsIncludeSubtree);

        assertThat(testNode.get(service1).getValue(), is(equalTo("service1")));
        assertThat(testNode.get(service1).getVersion(), is(equalTo(1L)));
        assertThat(testNode.get(service1 + "/subnode1").getValue(), is(equalTo("subnode1")));
        assertThat(testNode.get(service1 + "/subnode1").getVersion(), is(equalTo(1L)));
        assertThat(testNode.get(service1 + "/subnode1/subnode11").getValue(),
                is(equalTo("subnode11.1")));
        assertThat(testNode.get(service1 + "/subnode1/subnode11").getVersion(), is(equalTo(1L)));
        assertThat(testNode.get(service1 + "/subnode2").getValue(), is(equalTo("subnode2.1")));
        assertThat(testNode.get(service1 + "/subnode2").getVersion(), is(equalTo(1L)));
    }

    /**
     * Test method for {@link VslNodeDatabase#setValue(String, String)}.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen
     */
    @Test
    public final void testSetValue() throws NodeNotExistingException {
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
        nodesToSet.put(service1, "value1");
        db.setValueTree(nodesToSet);
        final InternalNode testNode = db.getNodeRecord(service1, paramsExcludeSubtree)
                .get(service1);

        assertThat(testNode.getValue(), is(equalTo("value1")));
        assertThat(testNode.getVersion(), is(equalTo(0L)));

    }

    /**
     * Test method for {@link VslNodeDatabase#setValue(String, String)}. Tests if a new version is
     * stored correctly.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen
     */
    @Test
    public final void testSetValueNewVersion() throws NodeNotExistingException {
        final String subnode = service1 + "/a";
        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");
        db.addNode(subnode, types, readers, writers, "", "");
        final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
        nodesToSet.put(service1, "");

        db.setValueTree(nodesToSet);
        nodesToSet.clear();
        nodesToSet.put(subnode, "value1");

        db.setValueTree(nodesToSet);
        // parent nodes version should be incremented on changes
        assertThat(db.getNodeRecord(service1, paramsIncludeSubtree).get(service1).getVersion(),
                is(equalTo(1L)));
        // incrementing should stop on service level
        assertThat(db.getNodeRecord(localKA, paramsIncludeSubtree).get(localKA).getVersion(),
                is(equalTo(-1L))); // DAVID Question about first versioning number DAVID
        nodesToSet.clear();
        nodesToSet.put(subnode, "value2");
        db.setValueTree(nodesToSet);
        assertThat(db.getNodeRecord(service1, paramsIncludeSubtree).get(service1).getVersion(),
                is(equalTo(2L)));
        assertThat(db.getNodeRecord(localKA, paramsIncludeSubtree).get(localKA).getVersion(),
                is(equalTo(-1L))); // DAVID Question about first versioning number DAVID

        final InternalNode testNode = db.getNodeRecord(subnode, paramsExcludeSubtree).get(subnode);

        assertThat(testNode.getValue(), is(equalTo("value2")));
        assertThat(testNode.getVersion(), is(equalTo(1L)));
    }

    // /**
    // * Test method for {@link VslNodeDatabase#setValue(String, String)}. Tests if a new version is
    // * stored correctly.
    // *
    // * @throws NodeNotExistingException
    // * shouldn't happen
    // */
    // @Test
    // @Ignore
    // public final void testSetValueDropOldVersion() throws NodeNotExistingException {
    // // TODO: temporarily disabled while fixing the versioning
    // when(configMock.getArchiveNodeVersionLimit()).thenReturn(2);
    //
    // final String subnode = service1 + "/a";
    // db.addNode(localKA, types, readers, writers, "", "");
    // db.addNode(service1, types, readers, writers, "", "");
    // db.addNode(subnode, types, readers, writers, "", "");
    //
    // db.setValue(service1, "");
    //
    // db.setValue(subnode, "value1");
    // db.setValue(subnode, "value2");
    // db.setValue(subnode, "value3");
    // db.setValue(subnode, "value4");
    //
    // // we set the limit to 2, so value4 and value3 should be stored
    // InternalNode testNode = db.getNodeRecord(subnode, paramsExcludeSubtree).get(subnode);
    // assertThat(testNode.getValue(), is(equalTo("value4")));
    // assertThat(testNode.getVersion(), is(equalTo(3L)));
    // testNode = db.getNodeRecord(subnode, paramsExcludeSubtree, 2).get(subnode);
    // assertThat(testNode.getValue(), is(equalTo("value3")));
    // assertThat(testNode.getVersion(), is(equalTo(2L)));
    //
    // // value2 should have been dropped after adding value4.
    // expectedException.expect(NodeNotExistingException.class);
    // testNode = db.getNodeRecord(subnode, paramsExcludeSubtree).get(subnode);
    // }

    /**
     * Test Method for {@link HSQLDatabase#cacheVslNode(String, VslNode)}.
     *
     * @throws Exception
     *             shouldn't happen
     */
    @Test
    public final void testCacheVslNode() throws Exception {

        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

        db.addNode(localKA, types, readers, writers, "", "");
        db.addNode(service1, types, readers, writers, "", "");
        db.addNode(service1 + "/a", types, readers, writers, "", "");
        db.addNode(service1 + "/a/c", types, readers, writers, "", "");
        db.addNode(service1 + "/b", types, readers, writers, "", "");
        db.addNode(service1 + "/b/d", types, readers, writers, "", "");

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

        db.cacheVslNode(service1, testNode);

        final TreeMap<String, InternalNode> result = db.getNodeRecord(service1,
                paramsIncludeSubtree);

        assertThat(result.size(), is(equalTo(5)));
        assertThat(result.get(service1).getValue(), is(equalTo("rootValue")));
        assertThat(result.get(service1 + "/a").getValue(), is(equalTo("child_a")));
        assertThat(result.get(service1 + "/a/c").getValue(), is(nullValue()));
        assertThat(result.get(service1 + "/b").getValue(), is(nullValue()));
        assertThat(result.get(service1 + "/b/d").getValue(), is(equalTo("child_b_d")));
        assertThat(result.get(service1).getTimestamp(), is(equalTo(new Timestamp(1234))));
        assertThat(result.get(service1 + "/a").getTimestamp(), is(equalTo(new Timestamp(1235))));
        assertThat(result.get(service1 + "/a/c").getTimestamp(), is(nullValue()));
        assertThat(result.get(service1 + "/b").getTimestamp(), is(nullValue()));
        assertThat(result.get(service1 + "/b/d").getTimestamp(), is(equalTo(new Timestamp(1236))));
        assertThat(result.get(service1).getVersion(), is(equalTo(4L)));
        assertThat(result.get(service1 + "/a").getVersion(), is(equalTo(3L)));
        assertThat(result.get(service1 + "/a/c").getVersion(), is(equalTo(0L)));
        assertThat(result.get(service1 + "/b").getVersion(), is(equalTo(0L)));
        assertThat(result.get(service1 + "/b/d").getVersion(), is(equalTo(1L)));
    }

    @After
    public final void wipeDatabase() {
        db.shutdown();
    }

}
