package org.ds2os.vsl.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.junit.Test;

/**
 * Test class for the JsonKORUpdateMapper.
 *
 * @author Johannes Straßer
 */
public class JsonKORUpdateMapperTest {

    /**
     * The unit under test.
     */
    private final VslMapper unitUnderTest = new DatabindMapperFactory(new VslNodeFactoryImpl())
            .getMapper(VslMimeTypes.JSON);

    /**
     * Test for write and read KORUpdate.
     *
     * @throws IOException
     *             THis should not be thrown
     */
    @Test
    public final void testKORUpdate() throws IOException {
        /*
         * Create Data
         */
        // Node 1
        List<String> newTypes = new ArrayList<String>();
        newTypes.add("/basic/text");
        List<String> newReaderIDs = new ArrayList<String>();
        newReaderIDs.add("group1");
        newReaderIDs.add("group2");
        List<String> newWriterIDs = new ArrayList<String>();
        newWriterIDs.add("group1");
        final VslStructureNodeImpl addedNode1 = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                "restriction1", newTypes, "");
        // Node 2
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
        // Create child nodes of Node2
        newTypes = new ArrayList<String>();
        newTypes.add("/basic/text");
        newTypes.add("/derived/boolean");
        final VslStructureNodeImpl child1 = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                "rest", newTypes, "");

        // VslNode child1 = new VslNode(newTypes, "child1Value", new
        // Timestamp(12345), 1234);
        newTypes = new ArrayList<String>();
        newTypes.add("/basic/text");
        newTypes.add("/derived/boolean");
        final VslStructureNodeImpl child2 = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                "rest", newTypes, "");
        // VslNode child2 = new VslNode(newTypes, "child2Value", new
        // Timestamp(23456), 2345);
        addedNode2.putChild("child1", child1);
        addedNode2.putChild("child2", child2);
        // Add nodes to addedNodes
        final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
        addedNodes.put("Node1", addedNode1);
        addedNodes.put("Node2", addedNode2);
        // Create removedNodes
        final Set<String> removedNodes = new HashSet<String>();
        removedNodes.add("Node3");
        removedNodes.add("Node4");
        removedNodes.add("Node5");
        // Create Update
        final KORUpdate update = new KORUpdate("stjzr6", "trshjj", addedNodes, removedNodes,
                "agent1");
        /*
         * Write and read update
         */
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        unitUnderTest.writeValue(output, update);
        final String json = Charset.forName(unitUnderTest.getContentEncoding())
                .decode(ByteBuffer.wrap(output.toByteArray())).toString();
        System.out.println(json);
        final InputStream input = new ByteArrayInputStream(
                json.getBytes(unitUnderTest.getContentEncoding()));
        final VslKORUpdate korUpdate = unitUnderTest.readValue(input, VslKORUpdate.class);

        /*
         * Compare results with data
         */
        assertThat(korUpdate.getHashFrom(), is("stjzr6"));
        assertThat(korUpdate.getHashTo(), is("trshjj"));

        // Check added node 1
        final VslStructureNode node1 = korUpdate.getAddedNodes().get("Node1");

        assertThat(node1.getReaderIds().remove("group1"), is(true));
        assertThat(node1.getReaderIds().remove("group2"), is(true));
        assertThat(node1.getReaderIds().isEmpty(), is(true));
        assertThat(node1.getWriterIds().remove("group1"), is(true));
        assertThat(node1.getWriterIds().isEmpty(), is(true));
        assertThat(node1.getRestrictions(), is("restriction1"));
        assertThat(node1.getTypes().remove("/basic/text"), is(true));
        assertThat(node1.getTypes().isEmpty(), is(true));

        // Check added node 2
        final VslStructureNode node2 = korUpdate.getAddedNodes().get("Node2");

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

        // // Check child 1 of added node 1
        // VslStructureNode node2child1 =
        // node2.getDirectChildren().get("child1");
        //
        // // Check child 2 of added node 1
        // VslStructureNode node2child2 = node2.getChild("child2");
        //

        // Check remaining fields
        assertThat(korUpdate.getRemovedNodes().remove("Node3"), is(true));
        assertThat(korUpdate.getRemovedNodes().remove("Node4"), is(true));
        assertThat(korUpdate.getRemovedNodes().remove("Node5"), is(true));
        assertThat(korUpdate.getRemovedNodes().isEmpty(), is(true));
        assertThat(korUpdate.getAgentName(), is("agent1"));
    }
}
