package org.ds2os.vsl.core.node;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Factory for {@link VslNode} and derived variants as {@link VslMutableNode}.
 *
 * @author felix
 */
public interface VslNodeFactory {

    /**
     * Create an immutable leaf without a type (useful only with set operations).
     *
     * @param value
     *            the value of the node.
     * @return the new {@link VslNode}.
     */
    VslNode createImmutableLeaf(String value);

    /**
     * Create an immutable leaf with type and value.
     *
     * @param types
     *            the type of the node.
     * @param value
     *            the value of the node.
     * @return the new {@link VslNode}.
     */
    VslNode createImmutableLeaf(List<String> types, String value);

    /**
     * Create an immutable leaf with all fields from {@link VslNodeData}.
     *
     * @param types
     *            the type of the node.
     * @param value
     *            the value of the node.
     * @param timestamp
     *            the timestamp of the last change of the node.
     * @param version
     *            the version number of the node.
     * @param access
     *            The access flag of the node.
     * @param restrictions
     *            The restrictions of the node.
     * @return the new {@link VslNode}.
     */
    VslNode createImmutableLeaf(List<String> types, String value, Date timestamp, long version,
            String access, Map<String, String> restrictions);

    /**
     * Create an immutable node with children from an iteration of map entries using relative
     * addresses to child node at that address. The childs can even have childs again.
     *
     * @param map
     *            the map entry iterable from which the immutable node is created.
     * @return the new {@link VslNode}.
     */
    VslNode createImmutableNode(Iterable<Map.Entry<String, VslNode>> map);

    /**
     * Create a mutable node without a type (useful only with set operations). Use the setters to
     * set the value or add children. The type cannot be changed (added in this case).
     *
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableNode();

    /**
     * Create a mutable node without a type (useful only with set operations) and initial value. Use
     * the setters to add children. The type cannot be changed (added in this case).
     *
     * @param value
     *            the value of the node.
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableNode(String value);

    /**
     * Create a mutable node with the specified type. Use the setters to set the value or add
     * children. The type cannot be changed.
     *
     * @param types
     *            the types of the node.
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableNode(List<String> types);

    /**
     * Create a mutable node with the specified type and type. Use the setters to add children. The
     * type cannot be changed.
     *
     * @param types
     *            the types of the node.
     * @param value
     *            the value of the node.
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableNode(List<String> types, String value);

    /**
     * Same as {@link #createImmutableNode(Iterable)}, but the whole returned node structure is
     * mutable.
     *
     * @param map
     *            the map entry iterable from which the mutable node is created. If the children in
     *            this map are immutable, mutable clones are created.
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableNode(Iterable<Map.Entry<String, VslNode>> map);

    /**
     * Clone an immutable node with a mutable node structure. Everything (including children) will
     * be mutable in the result.
     *
     * @param node
     *            the node to clone, which may be immutable or mutable.
     * @return the new {@link VslMutableNode}.
     */
    VslMutableNode createMutableClone(VslNode node);

    /**
     * Get the {@link Class} of the type which should be used for Jackson deserialization.
     *
     * @param <T>
     *            the type.
     * @return the class type.
     */
    <T extends VslNode> Class<T> getDeserializationType();
}
