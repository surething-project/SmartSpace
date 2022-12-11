package org.ds2os.vsl.core.node;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link VslNodeFactory}.
 * <p>
 * TODO: Many things should be more efficient...
 * </p>
 *
 * @author felix
 */
public class VslNodeFactoryImpl implements VslNodeFactory {
    @Override
    public final VslNode createImmutableLeaf(final String value) {
        return createImmutableLeaf(null, value);
    }

    @Override
    public final VslNode createImmutableLeaf(final List<String> types, final String value) {
        return createImmutableLeaf(types, value, null, -1L, null, null);
    }

    @Override
    public final VslNode createImmutableLeaf(final List<String> types, final String value,
            final Date timestamp, final long version, final String access,
            final Map<String, String> restrictions) {
        // TODO: create efficient leaf implementation
        return new VslNodeImpl(types, value, timestamp, version, access, restrictions);
    }

    @Override
    public final VslNode createImmutableNode(final Iterable<Map.Entry<String, VslNode>> map) {
        // FIXME: this is not immutable
        return createMutableNode(map);
    }

    @Override
    public final VslMutableNode createMutableNode() {
        return createMutableNode(null, null);
    }

    @Override
    public final VslMutableNode createMutableNode(final String value) {
        return createMutableNode(null, value);
    }

    @Override
    public final VslMutableNode createMutableNode(final List<String> types) {
        return createMutableNode(types, null);
    }

    @Override
    public final VslMutableNode createMutableNode(final List<String> types, final String value) {
        return new VslNodeImpl(types, value, null, -1L, null, null);
    }

    @Override
    public final VslMutableNode createMutableNode(final Iterable<Map.Entry<String, VslNode>> map) {
        final Map<String, VslNodeImpl> temp = new HashMap<String, VslNodeImpl>();
        for (final Map.Entry<String, VslNode> entry : map) {
            temp.put(entry.getKey(), (VslNodeImpl) createMutableClone(entry.getValue()));
        }
        return new VslNodeImpl(temp);
    }

    @Override
    public final VslMutableNode createMutableClone(final VslNode node) {
        final VslMutableNode mutableNode = new VslNodeImpl(node);
        for (final Map.Entry<String, VslNode> child : node.getAllChildren()) {
            mutableNode.putChild(child.getKey(), new VslNodeImpl(child.getValue()));
        }
        return mutableNode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T extends VslNode> Class<T> getDeserializationType() {
        return (Class<T>) VslNodeImpl.class;
    }
}
