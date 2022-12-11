package org.ds2os.vsl.core.node;

/**
 * VSL node with mutable data and children.
 *
 * @author felix
 */
public interface VslMutableNode
        extends VslNode, VslMutableNodeData, VslMutableTreeComponent<VslNode, VslMutableNode> {
}
