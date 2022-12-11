package org.ds2os.vsl.mapper.protobuf;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.junit.Test;

/**
 * Test mapping of VSL datatypes to Google protocol buffers with the {@link ProtobufMapper}.
 *
 * @author felix
 */
public final class ProtobufMapperTest {

    /**
     * Vsl node factory.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * The unit under test.
     */
    private final VslMapper unitUnderTest = new DatabindMapperFactory(nodeFactory)
            .getMapper(VslMimeTypes.PROTOBUF);

    /**
     * Test protobuf serialization of a simple node.
     *
     * @throws IOException
     *             Must not happen.
     */
    @Test
    public void testSimpleNode() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final VslNode demoNode;
        final VslNode deserialized;
        try {
            Map<String, String> restrictions = new LinkedHashMap<String, String>();
            restrictions.put("restriction1", "value1");
            restrictions.put("restriction2", "value2");
            demoNode = nodeFactory.createImmutableLeaf(Arrays.asList("/basic/string"), "Value",
                    new Timestamp(1444515934L), 15L, "r", restrictions);
            unitUnderTest.writeValue(output, demoNode);
            deserialized = unitUnderTest.readValue(new ByteArrayInputStream(output.toByteArray()),
                    VslNode.class);
        } finally {
            output.close();
        }
        assertThat(deserialized.getTypes(), contains(demoNode.getTypes().toArray()));
        assertThat(deserialized.getValue(), is(equalTo(demoNode.getValue())));
        assertThat(deserialized.getTimestamp(), is(equalTo(demoNode.getTimestamp())));
        assertThat(deserialized.getVersion(), is(equalTo(demoNode.getVersion())));
        assertThat(deserialized.getAccess(), is(equalTo(demoNode.getAccess())));
        assertThat(deserialized.getRestrictions(), is(equalTo(demoNode.getRestrictions())));
    }

    /**
     * Test protobuf serialization of a node with a child.
     *
     * @throws IOException
     *             Must not happen.
     */
    @Test
    public void testNodeWithChild() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final VslMutableNode demoNode;
        final VslNode deserialized;
        try {
            demoNode = nodeFactory.createMutableNode(Arrays.asList("/a/1", "/b/2"), "ParentValue");
            demoNode.putChild("relativeChildAddress", nodeFactory.createMutableNode("ChildValue"));
            unitUnderTest.writeValue(output, demoNode);
            deserialized = unitUnderTest.readValue(new ByteArrayInputStream(output.toByteArray()),
                    VslNode.class);
        } finally {
            output.close();
        }
        assertThat(deserialized.getTypes(), contains(demoNode.getTypes().toArray()));
        assertThat(deserialized.getValue(), is(equalTo(demoNode.getValue())));
        assertThat(deserialized.hasChild("relativeChildAddress"), is(true));
        assertThat(deserialized.getChild("relativeChildAddress").getValue(),
                is(equalTo("ChildValue")));
    }
}
