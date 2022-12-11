package org.ds2os.vsl.test.serialization;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.node.VslNode;

/**
 * A {@link NodeSerializationTest} using the databind mapper module for CBOR.
 *
 * @author felix
 */
public class DatabindCborNodeTest extends NodeSerializationTest {

    /**
     * Constructor for the superclass.
     *
     * @param node
     *            the node to test.
     * @param name
     *            the name of this node.
     */
    public DatabindCborNodeTest(final VslNode node, final String name) {
        super(node, name);
    }

    @Override
    public final VslMapper getMapper() {
        return TestMappers.getInstance().getCborDatabindMapper();
    }
}
