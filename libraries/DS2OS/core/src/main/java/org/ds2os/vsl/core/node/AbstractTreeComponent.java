package org.ds2os.vsl.core.node;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Abstract generic implementation of a mutable VSL subtree.
 *
 * @author felix
 * @param <T>
 *            the specific type of child tree component.
 * @param <U>
 *            the specific type of mutable tree component (U must extend T, though that's not
 *            specifiable with Java generics).
 */
public abstract class AbstractTreeComponent<T extends VslTreeComponent<T>,
        U extends VslMutableTreeComponent<T, U>> implements VslMutableTreeComponent<T, U> {

    /**
     * Map of the children of this node mapping their relative addresses to their instance.
     */
    private final ConcurrentMap<String, U> children;

    /**
     * Special internal flag for intermediary nodes. They must be replaced with the real value node
     * once there is a real node inserted at the position of the intermediary node.
     */
    private final boolean intermediary;

    /**
     * Default constructor without children for non-intermediary nodes.
     */
    protected AbstractTreeComponent() {
        children = new ConcurrentHashMap<String, U>();
        intermediary = false;
    }

    /**
     * Constructor of an intermediary node - only used internally.
     *
     * @param relativeChildAddress
     *            the relative address of the child of this intermediary node.
     * @param onlyChild
     *            the only child of this node, which is the cause why this intermediary node is
     *            created.
     */
    protected AbstractTreeComponent(final String relativeChildAddress, final U onlyChild) {
        children = new ConcurrentHashMap<String, U>();
        intermediary = true;
        putChild(relativeChildAddress, onlyChild);
    }

    /**
     * Internal helper constructor which can be used to directly construct the fields of this class.
     *
     * @param isIntermediary
     *            whether this node shall be intermediary or not.
     * @param directChildren
     *            map of the direct children to use.
     */
    protected AbstractTreeComponent(final boolean isIntermediary,
            final Map<String, U> directChildren) {
        intermediary = isIntermediary;
        if (directChildren == null) {
            children = new ConcurrentHashMap<String, U>();
        } else {
            children = new ConcurrentHashMap<String, U>(directChildren);
        }
    }

    /**
     * Needed by {@link #putChild(String, VslMutableTreeComponent)}: creates an intermediary node,
     * which does not hold any data, but does have a child (which might be another intermediary node
     * with a child).
     *
     * @param relativeChildAddress
     *            the relative address of the child of this intermediary node.
     * @param onlyChild
     *            the only child of this node, which is the cause why this intermediary node is
     *            created.
     * @return the newly created intermediary node with the onlyChild added at relativeChildAddress.
     */
    protected abstract U createIntermediaryNode(String relativeChildAddress, U onlyChild);

    @SuppressWarnings("unchecked")
    @Override
    public final T getChild(final String relativeAddress) {
        final int firstSlash = relativeAddress.indexOf('/');
        if (firstSlash == relativeAddress.length() - 1) {
            return null;
        }
        if (firstSlash != -1) {
            final VslTreeComponent<T> directChild = children
                    .get(relativeAddress.substring(0, firstSlash));
            if (directChild == null) {
                return null;
            } else {
                return directChild.getChild(relativeAddress.substring(firstSlash + 1));
            }
        }
        // we suppressed warnings but this conversion is safe as U extends T is also required
        return (T) children.get(relativeAddress);
    }

    @Override
    public final U putChild(final String relativeAddress, final U childNode) {
        final int firstSlash = relativeAddress.indexOf('/');
        if (firstSlash == relativeAddress.length() - 1) {
            return null;
        }
        if (firstSlash != -1) {
            final String prefix = relativeAddress.substring(0, firstSlash);
            final String suffix = relativeAddress.substring(firstSlash + 1);
            final U directChild;
            if (childNode == null) {
                directChild = children.get(prefix);
            } else {
                // atomic and proper insertion requires preallocating an intermediary node
                final U intermediaryNode = createIntermediaryNode(suffix, childNode);
                directChild = children.putIfAbsent(prefix, intermediaryNode);
            }
            if (directChild == null) {
                return null;
            } else {
                final U result = directChild.putChild(suffix, childNode);
                // TODO: safe removing of stale intermediary nodes
                // if (directChild.isIntermediary() && directChild.isLeaf()) {
                // // race condition!
                // children.remove(prefix);
                // }
                return result;
            }
        }
        if (childNode == null) {
            return children.remove(relativeAddress);
        } else {
            return children.put(relativeAddress, childNode);
        }
    }

    @Override
    public final boolean hasChild(final String relativeAddress) {
        // it's legitimate to use getChild != null as we do not permit null nodes
        return getChild(relativeAddress) != null;
    }

    @Override
    public final boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public final boolean isIntermediary() {
        return intermediary;
    }

    /**
     * Special method for Jackson serialization to get all children as map for convenience.
     *
     * @return the child map or null if the node does not have children.
     */
    @JsonProperty("children")
    protected final Map<String, U> getChildMapForJackson() {
        if (children.isEmpty()) {
            return null;
        } else {
            return children;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Iterable<Map.Entry<String, T>> getDirectChildren() {
        // we suppressed warnings but this conversion is safe as U extends T is also required
        @SuppressWarnings("rawtypes")
        final Iterable entries = Collections.unmodifiableMap(children).entrySet();
        return entries;
    }

    /**
     * Recursive helper method for getAllChildren.
     *
     * @param relativeAddress
     *            the relative address.
     * @param currentChild
     *            the child which will be iterated in this step.
     * @param output
     *            the output map shared across the whole recursion.
     */
    protected final void getAllChildrenRecursive(final String relativeAddress,
            final VslTreeComponent<T> currentChild, final Map<String, T> output) {
        for (final Map.Entry<String, T> child : currentChild.getDirectChildren()) {
            output.put(relativeAddress + child.getKey(), child.getValue());
            if (!child.getValue().isLeaf()) {
                getAllChildrenRecursive(relativeAddress + child.getKey() + "/", child.getValue(),
                        output);
            }
        }
    }

    @Override
    public final Iterable<Map.Entry<String, T>> getAllChildren() {
        // TODO: this should be done with a custom iterator to prevent copying the whole structure
        final Map<String, T> tempMap = new TreeMap<String, T>();
        getAllChildrenRecursive("", this, tempMap);
        return Collections.unmodifiableMap(tempMap).entrySet();
    }
}
