package org.ds2os.vsl.core.node;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A tree component of the VSL tree. A single component is also referred to as node. These nodes can
 * have children which can again have children or not.
 *
 * @author felix
 * @param <T>
 *            the specific type of tree component.
 */
public interface VslTreeComponent<T extends VslTreeComponent<T>> {

    /**
     * Get the node at the specified address, which is relative to this node.
     *
     * @param relativeAddress
     *            the relative address.
     * @return null if there is no node at this address.
     */
    T getChild(String relativeAddress);

    /**
     * Checks if this node has a child at the specified address, which is relative to this node.
     *
     * @param relativeAddress
     *            the relative address.
     * @return true iff there is a node at this address.
     */
    boolean hasChild(String relativeAddress);

    /**
     * Check if this node is a leaf node, i.e. without children.
     *
     * @return true iff the node is a leaf node
     */
    @JsonIgnore
    boolean isLeaf();

    /**
     * VSL trees can contain nodes which are intermediary, i.e. they only exist because they have
     * children and are needed to complete the path to the children (example: there is a node /a and
     * a node /a/b/c: b is not really existing but needed to hold c).
     *
     * @return true iff the node is intermediary.
     */
    @JsonIgnore
    boolean isIntermediary();

    /**
     * Get all direct child nodes as map entries of relative addresses (child names) to the node
     * instance.
     *
     * @return Iterable of map entries which map relative address to the node at that address.
     */
    @JsonIgnore
    Iterable<Map.Entry<String, T>> getDirectChildren();

    /**
     * Get all child nodes including child nodes of the childs as map entries of relative addresses
     * (child names) to the node instance. The instances will still contain the children which are
     * also iterated by this iterable.
     *
     * @return Iterable of map entries which map relative address to the node at that address.
     */
    @JsonIgnore
    Iterable<Map.Entry<String, T>> getAllChildren();
}
