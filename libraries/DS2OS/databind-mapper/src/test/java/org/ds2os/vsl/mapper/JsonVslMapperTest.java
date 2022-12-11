package org.ds2os.vsl.mapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.StringContains.containsString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.transport.PostOperation.OperationType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test mapping of the Vsl content to JSON with the {@link JsonMapper}.
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
    private static final String DEMO_JSON_SIMPLE = "{\"types\":[\"/basic/number\"],"
            + "\"value\":\"Example\",\"version\":5,\"timestamp\":1444515934,"
            + "\"access\":\"r\",\"restrictions\":{\"a\":\"a1\",\"b\":\"b1\"}}";
    /**
     * Json demo string with children.
     */
    private static final String DEMO_JSON_WITH_CHILDREN = "{\"types\":[\"/basic/composition\","
            + "\"/basic/text\"],\"children\":{\"a\":" + DEMO_JSON_SIMPLE + ",\"b\":"
            + DEMO_JSON_SIMPLE + "}}";

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
    private final VslMapper unitUnderTest = new DatabindMapperFactory(nodeFactory)
            .getMapper(VslMimeTypes.JSON);

    /**
     * Test simple writing of a node without children. Only some Json validation is done.
     *
     * @throws IOException
     *             Must not happen.
     */
    @Test
    public void testWriteNodeSimple() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            Map<String, String> restrictions = new LinkedHashMap<String, String>();
            restrictions.put("restriction1", "value1");
            restrictions.put("restriction2", "value2");
            final VslNode demoNode = nodeFactory.createImmutableLeaf(Arrays.asList("/basic/string"),
                    "Value", new Timestamp(1444515934L), 15L, "r", restrictions);
            unitUnderTest.writeValue(output, demoNode);
            final String json = Charset.forName(unitUnderTest.getContentEncoding())
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString();
            LOG.debug("Simple Json: {}", json);
            assertThat(json, containsString("\"types\":[\"/basic/string\"]"));
            assertThat(json, containsString("\"value\":\"Value\""));
            assertThat(json, containsString("\"version\":15"));
            assertThat(json, containsString("\"timestamp\":1444515934"));
            assertThat(json, containsString(
                    "\"restrictions\":{\"restriction1\":\"value1\",\"restriction2\":\"value2\"}"));
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
    @Test
    public void testWriteNodeWithChild() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            final VslMutableNode demoNode = nodeFactory
                    .createMutableNode(Arrays.asList("/a/1", "/b/2"), "ParentValue");
            demoNode.putChild("relativeChildAddress", nodeFactory.createMutableNode("ChildValue"));
            unitUnderTest.writeValue(output, demoNode);
            final String json = Charset.forName(unitUnderTest.getContentEncoding())
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString();
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
    @Test
    public void testReadNodeSimple() throws IOException {
        final InputStream input = new ByteArrayInputStream(
                DEMO_JSON_SIMPLE.getBytes(unitUnderTest.getContentEncoding()));
        LOG.debug(DEMO_JSON_SIMPLE);
        try {
            final VslNode node = unitUnderTest.readValue(input, VslNode.class);
            assertThat(node.getTypes(), contains("/basic/number"));
            assertThat(node.getValue(), is(equalTo("Example")));
            assertThat(node.getVersion(), is(equalTo(5L)));
            assertThat(node.getTimestamp().getTime(), is(equalTo(1444515934L)));
            assertThat(node.getAccess(), is(equalTo("r")));

            assertThat(node.getRestrictions().size(), is(equalTo(2)));
            assertThat(node.getRestrictions().toString(), containsString("a=a1"));
            assertThat(node.getRestrictions().toString(), containsString("b=b1"));
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
    @Test
    public void testReadNodeWithChildren() throws IOException {
        final InputStream input = new ByteArrayInputStream(
                DEMO_JSON_WITH_CHILDREN.getBytes(unitUnderTest.getContentEncoding()));
        LOG.debug(DEMO_JSON_WITH_CHILDREN);
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
    @Test
    public void testIdempotency() throws IOException {
        final Map<String, VslNode> exampleData = new HashMap<String, VslNode>();
        exampleData.put("a", nodeFactory.createImmutableLeaf(Arrays.asList("aType"), "aValue"));
        exampleData.put("a/b/c", nodeFactory.createImmutableLeaf("cValue"));
        exampleData.put("d", nodeFactory.createImmutableLeaf(Arrays.asList("dType"), "dValue"));
        final VslNode originalNode = nodeFactory.createImmutableNode(exampleData.entrySet());

        // write Json
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final String json;
        try {
            unitUnderTest.writeValue(output, originalNode);
            json = Charset.forName(unitUnderTest.getContentEncoding())
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString();
            LOG.debug("Comlex Json of idempotency test:\n{}", json);
        } finally {
            output.close();
        }

        // read again
        final InputStream input = new ByteArrayInputStream(
                json.getBytes(unitUnderTest.getContentEncoding()));
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
        assertThat(readNode.getChild("a/b/c").getTypes(), is(nullValue()));
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
    @Test
    public void testWriteOperation() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            final UUID randomUUID = UUID.randomUUID();
            final PostOperation demoOperation = new PostOperation(OperationType.SUBSCRIBE,
                    randomUUID);
            unitUnderTest.writeValue(output, demoOperation);
            final String json = Charset.forName(unitUnderTest.getContentEncoding())
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString();
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
    @Test
    public void testReadOperation() throws IOException {
        final InputStream input = new ByteArrayInputStream(
                DEMO_JSON_OPERATION.getBytes(unitUnderTest.getContentEncoding()));
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
    @Test
    public void testIdentity() throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            final VslIdentity demoIdentity = new ServiceIdentity("myClient",
                    Arrays.asList("accessID1", "accessID2"));
            unitUnderTest.writeValue(output, demoIdentity);
            final String json = Charset.forName(unitUnderTest.getContentEncoding())
                    .decode(ByteBuffer.wrap(output.toByteArray())).toString();
            LOG.debug("Identity Json: {}", json);
            assertThat(json, containsString("\"clientId\":\"myClient\""));
            final InputStream input = new ByteArrayInputStream(
                    json.getBytes(unitUnderTest.getContentEncoding()));
            final VslIdentity deserializedIdentity = unitUnderTest.readValue(input,
                    ServiceIdentity.class);
            assertThat(deserializedIdentity.getAccessIDs().size(),
                    is(equalTo(demoIdentity.getAccessIDs().size())));
        } finally {
            output.close();
        }
    }
}
