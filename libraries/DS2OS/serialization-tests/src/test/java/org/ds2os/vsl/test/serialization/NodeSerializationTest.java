package org.ds2os.vsl.test.serialization;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.test.TestNodes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Base class for serialization tests that test with {@link VslNode} structures.
 *
 * @author felix
 */
@RunWith(Parameterized.class)
public abstract class NodeSerializationTest {

    /**
     * The {@link VslNode} to test.
     */
    private final VslNode node;

    /**
     * The name of the test node.
     */
    private final String name;

    /**
     * Constructor for parametrized runner.
     *
     * @param node
     *            the {@link VslNode} used for the test.
     * @param name
     *            the name of the test node.
     */
    public NodeSerializationTest(final VslNode node, final String name) {
        this.node = node;
        this.name = name;
    }

    /**
     * Abstract method to get the {@link VslMapper} under test. Subclasses implement this to test a
     * specific mapper implementation.
     *
     * @return the mapper instance.
     */
    public abstract VslMapper getMapper();

    /**
     * Get the node of the test.
     *
     * @return the node.
     */
    public VslNode getNode() {
        return node;
    }

    /**
     * Test to serialize the node using the mapper from {@link #getMapper()} to a file.
     *
     * @throws IOException
     *             Should not happen, but might be caused by filesystem I/O errors.
     */
    @Test
    public void testSerialization() throws IOException {
        final VslMapper mapper = getMapper();
        final File outputFile = new File("target/serialized/" + name + "."
                + mapper.getContentType().substring(mapper.getContentType().lastIndexOf("/") + 1));
        outputFile.getParentFile().mkdirs();
        final OutputStream output = new FileOutputStream(outputFile);
        try {
            mapper.writeValue(output, node);
        } finally {
            output.close();
        }
        assertThat(outputFile.length(), is(greaterThan(0L)));
    }

    /**
     * Test to deserialize the given data after serialization.
     *
     * @throws IOException
     *             Should not happen.
     */
    @Test
    public void testDeserialization() throws IOException {
        final VslMapper mapper = getMapper();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            mapper.writeValue(output, node);
        } finally {
            output.close();
        }
        final InputStream input = new ByteArrayInputStream(output.toByteArray());
        try {
            final VslNode deserialized = mapper.readValue(input, VslNode.class);
            assertThat(deserialized, is(not(nullValue())));
        } finally {
            input.close();
        }
    }

    /**
     * Data set for the parametrized runner.
     *
     * @return the data set.
     */
    @Parameters(name = "{index} - {1}")
    public static Collection<Object[]> data() {
        final List<Object[]> result = new ArrayList<Object[]>(9);
        result.add(new Object[]{TestNodes.SIMPLE_DATA_NODE, "simple data node"});
        result.add(new Object[]{TestNodes.BIG_DATA_NODE, "big data node"});
        result.add(new Object[]{TestNodes.SIMPLE_NODE, "simple node"});
        result.add(new Object[]{TestNodes.METADATA_NODE, "metadata node"});
        result.add(new Object[]{TestNodes.BIG_NODE, "big node"});
        result.add(new Object[]{TestNodes.SIMPLE_DATA_STRUCTURE, "simple data structure"});
        result.add(new Object[]{TestNodes.BIG_DATA_STRUCTURE, "big data structure"});
        result.add(new Object[]{TestNodes.SIMPLE_STRUCTURE, "simple structure"});
        result.add(new Object[]{TestNodes.METADATA_STRUCTURE, "metadata structure"});
        result.add(new Object[]{TestNodes.BIG_STRUCTURE, "big structure"});
        return result;
    }
}
