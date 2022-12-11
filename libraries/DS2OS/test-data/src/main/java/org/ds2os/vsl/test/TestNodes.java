package org.ds2os.vsl.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example {@link VslNode} structures for testing.
 *
 * @author felix
 */
public final class TestNodes {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestNodes.class);

    /**
     * Factory for the VSl nodes.
     */
    public static final VslNodeFactory NODE_FACTORY = new VslNodeFactoryImpl();

    /**
     * Simple data value.
     */
    public static final String SIMPLE_VALUE = "1234";

    /**
     * Big data value.
     */
    public static final String BIG_VALUE = loadBigValue();

    /**
     * Simple type.
     */
    public static final List<String> SIMPLE_TYPE = Arrays.asList("/basic/number");

    /**
     * Example date.
     */
    public static final Date EXAMPLE_DATE = Date.valueOf("2012-12-23");

    /**
     * Example version.
     */
    public static final long EXAMPLE_VERSION = 123456789L;

    /**
     * Example access.
     */
    public static final String EXAMPLE_ACCESS = "w";

    /**
     * Simple restriction example.
     */
    public static final Map<String, String> SIMPLE_RESTRICTIONS = Collections.singletonMap("regex",
            "[0-9]+");

    /**
     * Comlex type.
     */
    public static final List<String> COMPLEX_TYPE = Arrays.asList(
            "/com/fancycompany/subdivision/fancyproduct/submodel",
            "/feature/extendedfeature/ofdevice", "/device/typeofdevice", "/category/type",
            "/basic/composed");

    /**
     * Complex restriction example.
     */
    public static final Map<String, String> COMPLEX_RESTRICTIONS;
    static {
        final Map<String, String> fill = new HashMap<String, String>();
        fill.put("restriction1", "value1");
        fill.put("restriction2", "1234");
        fill.put("restriction3", "foobar");
        fill.put("restriction4", "<5 && >10");
        fill.put("restriction5", "restricted");
        fill.put("restriction6", "false");
        COMPLEX_RESTRICTIONS = fill;
    }

    /**
     * Very simple data node without metadata or children, just containing {@link #SIMPLE_VALUE}.
     */
    public static final VslNode SIMPLE_DATA_NODE = NODE_FACTORY.createImmutableLeaf(SIMPLE_VALUE);

    /**
     * Big data node without metadata or children, just containing {@link #BIG_VALUE}.
     */
    public static final VslNode BIG_DATA_NODE = NODE_FACTORY.createImmutableLeaf(BIG_VALUE);

    /**
     * Node with simple metadata, but without children or value.
     */
    public static final VslNode SIMPLE_METADATA_NODE = NODE_FACTORY.createImmutableLeaf(SIMPLE_TYPE,
            null, EXAMPLE_DATE, EXAMPLE_VERSION, EXAMPLE_ACCESS, SIMPLE_RESTRICTIONS);

    /**
     * Simple node without children, that contains simple metadata and {@link #SIMPLE_VALUE}.
     */
    public static final VslNode SIMPLE_NODE = NODE_FACTORY.createImmutableLeaf(SIMPLE_TYPE,
            SIMPLE_VALUE, EXAMPLE_DATE, EXAMPLE_VERSION, EXAMPLE_ACCESS, SIMPLE_RESTRICTIONS);

    /**
     * Node with complex metadata, but without children or value.
     */
    public static final VslNode METADATA_NODE = NODE_FACTORY.createImmutableLeaf(COMPLEX_TYPE, null,
            EXAMPLE_DATE, EXAMPLE_VERSION, EXAMPLE_ACCESS, COMPLEX_RESTRICTIONS);

    /**
     * Big node with complex metadata and {@link #BIG_VALUE}, but without children.
     */
    public static final VslNode BIG_NODE = NODE_FACTORY.createImmutableLeaf(COMPLEX_TYPE, BIG_VALUE,
            EXAMPLE_DATE, EXAMPLE_VERSION, EXAMPLE_ACCESS, COMPLEX_RESTRICTIONS);

    /**
     * {@link #SIMPLE_DATA_NODE} with a child structure of other {@link #SIMPLE_DATA_NODE}.
     */
    public static final VslNode SIMPLE_DATA_STRUCTURE = addChildTree(SIMPLE_DATA_NODE,
            SIMPLE_DATA_NODE);

    /**
     * {@link #BIG_DATA_NODE} with a child structure of other {@link #BIG_DATA_NODE}.
     */
    public static final VslNode BIG_DATA_STRUCTURE = addChildTree(BIG_DATA_NODE, BIG_DATA_NODE);

    /**
     * {@link #SIMPLE_METADATA_NODE} with a child structure of other {@link #SIMPLE_METADATA_NODE}.
     */
    public static final VslNode SIMPLE_METADATA_STRUCTURE = addChildTree(SIMPLE_METADATA_NODE,
            SIMPLE_METADATA_NODE);

    /**
     * {@link #SIMPLE_NODE} with a child structure of other {@link #SIMPLE_NODE}.
     */
    public static final VslNode SIMPLE_STRUCTURE = addChildTree(SIMPLE_NODE, SIMPLE_NODE);

    /**
     * {@link #METADATA_NODE} with a child structure of other {@link #METADATA_NODE}.
     */
    public static final VslNode METADATA_STRUCTURE = addChildTree(METADATA_NODE, METADATA_NODE);

    /**
     * {@link #BIG_NODE} with a child structure of other {@link #BIG_NODE}.
     */
    public static final VslNode BIG_STRUCTURE = addChildTree(BIG_NODE, BIG_NODE);

    /**
     * Utility class, no instantiation.
     */
    private TestNodes() {
        // utility class
    }

    /**
     * Load the big value from a resource.
     *
     * @return the big value.
     */
    private static String loadBigValue() {
        String result = "";
        InputStream resource = TestNodes.class.getResourceAsStream("1mb.txt");
        if (resource == null) {
            resource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("/org/ds2os/vsl/test/1mb.txt");
        }
        if (resource == null) {
            LOG.error("Cannot find 1mb.txt resource");
        } else {
            try {
                try {
                    final byte[] mb = new byte[1024 * 1024];
                    int read = resource.read(mb);
                    while (read < mb.length) {
                        final int readInc = resource.read(mb, read, mb.length - read);
                        if (readInc > 0) {
                            read += readInc;
                        } else {
                            break;
                        }
                    }
                    if (read < mb.length) {
                        LOG.warn("Did not read a full MB from resource 1mb.txt."
                                + " Actual number of bytes read: {}", read);
                    }
                    result = Charset.forName("UTF-8").decode(ByteBuffer.wrap(mb, 0, read))
                            .toString();
                } finally {
                    resource.close();
                }
            } catch (final IOException e) {
                LOG.error("Cannot read 1mb.txt resource", e);
            }
        }
        return result;
    }

    /**
     * add a tree of children to the given node, with depth=3.
     *
     * @param node
     *            the node to which all the children are added.
     * @param children
     *            the node to add in each of the subtree positions.
     * @return the map.
     */
    private static VslNode addChildTree(final VslNode node, final VslNode children) {
        final VslMutableNode tmp = NODE_FACTORY.createMutableClone(node);
        tmp.putChild("child1", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child2", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child3", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child2/child2.1", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child2/child2.1/child2.1.1", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child2/child2.1/child2.1.2", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child3/child3.1", NODE_FACTORY.createMutableClone(node));
        tmp.putChild("child4/child4.1/child4.1.1", NODE_FACTORY.createMutableClone(node));
        return tmp;
    }
}
