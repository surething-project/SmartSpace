package org.ds2os.vsl.core.node;

/**
 * A mutable extension of {@link VslTreeComponent}.
 *
 * @author felix
 * @param <T>
 *            the specific type of tree component.
 * @param <U>
 *            the specific type of mutable tree component (U must extend T, though that's not
 *            specifiable with Java generics).
 */
public interface VslMutableTreeComponent<T extends VslTreeComponent<T>,
        U extends VslMutableTreeComponent<T, U>>
        extends VslTreeComponent<T> {

    /**
     * Associates the specified childNode with the specified relative address as a child node of
     * this node. If this node previously contained a child node at this address, the old child node
     * is replaced by the new child node. Children of the old node are not moved to the new node.
     *
     * @param relativeAddress
     *            the address, relative to this node, where the child is inserted.
     * @param childNode
     *            the new child node or null, in which case the old child node is removed and no new
     *            child node is inserted.
     * @return null if there was no node associated with this address or the old child node.
     */
    U putChild(String relativeAddress, U childNode);
}
