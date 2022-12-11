package org.ds2os.vsl.json;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.transport.PostOperation.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test mapping of the Vsl content to Json with the {@link SimpleJsonMapper} .
 * <p>
 * FIMXE: does not work with SimpleJsonMapper!
 * </p>
 *
 * @author felix
 * @author liebald
 */
public final class JsonVslMapperTest {

    /**
     * SLF4J logger for Json output to DEBUG level.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsonVslMapperTest.class);

    /**
     * Simple Json demo string.
     */
    private static final String DEMO_JSON_SIMPLE = "{\"type\":\"/basic/number\","
            + "\"value\":\"Example\",\"version\":5,\"timestamp\":1444515934}";

    /**
     * Json demo string with children.
     */
    private static final String DEMO_JSON_WITH_CHILDREN = "{\"type\":\"/basic/composition,"
            + "/basic/text\",\"children\":{\"a\":" + DEMO_JSON_SIMPLE + ",\"b\":" + DEMO_JSON_SIMPLE
            + "}}";

    /**
     * Json demo string for {@link PostOperation}.
     */
    private static final String DEMO_JSON_OPERATION = "{\"operation\":\"LOCK_SUBTREE\","
            + "\"callbackId\":\"cf255f45-c442-4af8-95f7-1c054ad0093a\"}";

    /**
     * Vsl node factory.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * The unit under test.
     */
    private final SimpleJsonMapper unitUnderTest = new SimpleJsonMapper();

    /**
     * Test simple writing of a node without children. Only some Json validation is done.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testWriteNodeSimple() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final VslNode demoNode = nodeFactory.createImmutableLeaf(Arrays.asList("/basic/string"),
                    "Value", new Timestamp(1444515934L), 15L, null,
                    Collections.<String, String>emptyMap());
            unitUnderTest.writeValue(output, demoNode);
            final String json = output.getBuffer().toString();
            LOG.debug("Simple Json: {}", json);
            assertThat(json, containsString("\"type\":\"/basic/string\""));
            assertThat(json, containsString("\"value\":\"Value\""));
            assertThat(json, containsString("\"version\":15"));
            assertThat(json, containsString("\"timestamp\":1444515934"));
            assertThat(json, not(containsString("\"children\":")));
        } finally {
            output.close();
        }
    }

    /**
     * Test writing of a node with a child. Only some Json validation is done.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testWriteNodeWithChild() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final VslMutableNode demoNode = nodeFactory.createMutableNode("ParentValue");
            demoNode.putChild("relativeChildAddress", nodeFactory.createMutableNode("ChildValue"));
            unitUnderTest.writeValue(output, demoNode);
            final String json = output.getBuffer().toString();
            LOG.debug("Json with child: {}", json);
            assertThat(json, containsString("\"value\":\"ParentValue\""));
            assertThat(json, containsString("\"children\":{\"relativeChildAddress\":"));
            assertThat(json, containsString("\"relativeChildAddress\":{\"value\":\"ChildValue\""));
        } finally {
            output.close();
        }
    }

    /**
     * Test the readNode functionality with a simple example.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testReadNodeSimple() throws IOException {
        final Reader input = new StringReader(DEMO_JSON_SIMPLE);
        try {
            final VslNode node = unitUnderTest.readValue(input, VslNode.class);
            assertThat(node.getTypes(), contains("/basic/number"));
            assertThat(node.getValue(), is(equalTo("Example")));
            assertThat(node.getVersion(), is(equalTo(5L)));
            assertThat(node.getTimestamp().getTime(), is(equalTo(1444515934L)));
            assertThat(node.isLeaf(), is(true));
        } finally {
            input.close();
        }
    }

    /**
     * Test the readNode functionality with children.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testReadNodeWithChildren() throws IOException {
        final Reader input = new StringReader(DEMO_JSON_WITH_CHILDREN);
        try {
            final VslNode node = unitUnderTest.readValue(input, VslNode.class);
            assertThat(node.getTypes().size(), is(equalTo(2)));
            assertThat(node.getTypes(), contains("/basic/composition", "/basic/text"));
            assertThat(node.getValue(), is(nullValue()));
            assertThat(node.getChild("a"), is(not(nullValue())));
            assertThat(node.getChild("b"), is(not(nullValue())));
        } finally {
            input.close();
        }
    }

    /**
     * Test idempotency with a complex structure.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testIdempotency() throws IOException {
        final Map<String, VslNode> exampleData = new HashMap<String, VslNode>();
        exampleData.put("a", nodeFactory.createImmutableLeaf(Arrays.asList("aType"), "aValue"));
        exampleData.put("a/b/c", nodeFactory.createImmutableLeaf("cValue"));
        exampleData.put("d", nodeFactory.createImmutableLeaf(Arrays.asList("dType"), "dValue"));
        final VslNode originalNode = nodeFactory.createImmutableNode(exampleData.entrySet());

        // write Json
        final StringWriter output = new StringWriter();
        final String json;
        try {
            unitUnderTest.writeValue(output, originalNode);
            json = output.getBuffer().toString();
            LOG.debug("Comlex Json of idempotency test:\n{}", json);
        } finally {
            output.close();
        }

        // read again
        final Reader input = new StringReader(json);
        final VslNode readNode;
        try {
            readNode = unitUnderTest.readValue(input, VslNode.class);
        } finally {
            input.close();
        }

        // check idempotency
        assertThat(readNode.getValue(), is(nullValue()));
        assertThat(readNode.hasChild("a"), is(true));
        assertThat(readNode.hasChild("a/b"), is(true));
        assertThat(readNode.hasChild("a/b/c"), is(true));
        assertThat(readNode.hasChild("d"), is(true));
        assertThat(readNode.getChild("a").getTypes(), contains("aType"));
        assertThat(readNode.getChild("a").getValue(), is(equalTo("aValue")));
        assertThat(readNode.getChild("a/b").getValue(), is(nullValue()));
        assertNull(readNode.getChild("a/b/c").getTypes());
        assertThat(readNode.getChild("a/b/c").getValue(), is(equalTo("cValue")));
        assertThat(readNode.getChild("d").getTypes(), contains("dType"));
        assertThat(readNode.getChild("d").getValue(), is(equalTo("dValue")));
    }

    /**
     * Test writing of a post operation. Only some Json validation is done.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testWriteOperation() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final UUID randomUUID = UUID.randomUUID();
            final PostOperation demoOperation = new PostOperation(OperationType.SUBSCRIBE,
                    randomUUID);
            unitUnderTest.writeValue(output, demoOperation);
            final String json = output.getBuffer().toString();
            LOG.debug("Operation Json: {}", json);
            assertThat(json, containsString("\"operation\":\"SUBSCRIBE\""));
            assertThat(json, containsString("\"callbackId\":\"" + randomUUID.toString() + "\""));
        } finally {
            output.close();
        }
    }

    /**
     * Test the readOperation functionality.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testReadOperation() throws IOException {
        final Reader input = new StringReader(DEMO_JSON_OPERATION);
        try {
            final PostOperation operation = unitUnderTest.readValue(input, PostOperation.class);
            assertThat(operation.getOperation(), is(equalTo(OperationType.LOCK_SUBTREE)));
            assertThat(operation.getCallbackId(),
                    is(equalTo(UUID.fromString("cf255f45-c442-4af8-95f7-1c054ad0093a"))));
        } finally {
            input.close();
        }
    }

    /**
     * Test the databinder for VslIdentity.
     *
     * @throws IOException
     *             Must not happen.
     */
    // @Test
    public void testIdentity() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final VslIdentity demoIdentity = new ServiceIdentity("myClient",
                    Arrays.asList("accessID1", "accessID2"));
            unitUnderTest.writeValue(output, demoIdentity);
            final String json = output.getBuffer().toString();
            LOG.debug("Identity Json: {}", json);
            assertThat(json, containsString("\"clientId\":\"myClient\""));
            final VslIdentity deserializedIdentity = unitUnderTest.readValue(new StringReader(json),
                    ServiceIdentity.class);
            assertThat(deserializedIdentity.getAccessIDs().size(),
                    is(equalTo(demoIdentity.getAccessIDs().size())));
        } finally {
            output.close();
        }
    }

    // TODO: Test with service manifest etc.
}
