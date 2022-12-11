package org.ds2os.vsl.core.node;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A node of the VSL, as used internally for operations that need structural information. Extends
 * the normal {@link VslNode} with additional data (readers, writers, restriction).
 *
 * @author liebald, felix
 */
public class VslStructureNodeImpl
        extends AbstractTreeComponent<VslStructureNode, VslMutableStructureNode>
        implements VslMutableStructureNode {

    /**
     * The List of IDs that have read access on this node.
     */
    private List<String> readerIds;

    /**
     * The List of IDs that have write access on this node.
     */
    private List<String> writerIds;

    /**
     * The restrictions of this node.
     */
    private String restrictions;

    /**
     * The cache parameters of this node.
     */
    private String cacheParameters;

    /**
     * The types of the Node.
     */
    private List<String> types;

    /**
     * Constructor of a new node cloning an existing node without its children.
     *
     * @param clone
     *            the node to clone or null will create an intermediary node.
     */
    protected VslStructureNodeImpl(final VslStructureNodeImpl clone) {
        this(clone == null ? null : clone.getReaderIds(),
                clone == null ? null : clone.getWriterIds(),
                clone == null ? "" : clone.getRestrictions(),
                clone == null ? null : clone.getTypes(),
                clone == null ? "" : clone.getCacheParameters());
    }

    /**
     * Constructor of an intermediary node - only used internally.
     *
     * @param relativeAddress
     *            the relative address of the child of this intermediary node.
     * @param onlyChild
     *            the only child of this node, which is the cause why this intermediary node is
     *            created.
     */
    protected VslStructureNodeImpl(final String relativeAddress,
            final VslMutableStructureNode onlyChild) {
        super(relativeAddress, onlyChild);
        readerIds = new LinkedList<String>();
        writerIds = new LinkedList<String>();
        types = new LinkedList<String>();
        restrictions = "";
        cacheParameters = "";
    }

    /**
     * Constructor for metaData only.
     *
     * @param newTypes
     *            The type(s) of the node.
     * @param newReaderIds
     *            The ReaderIds.
     * @param newWriterIds
     *            The writerIds.
     * @param newRestriction
     *            The restrictions.
     * @param newCacheParameters
     *            The cache parameters.
     * @throws IllegalArgumentException
     *             If any of the Lists is null (reader, writer or types).
     */
    public VslStructureNodeImpl(final List<String> newReaderIds, final List<String> newWriterIds,
            final String newRestriction, final List<String> newTypes,
            final String newCacheParameters) throws IllegalArgumentException {
        if (newReaderIds == null) {
            throw new IllegalArgumentException("readerIds can't be null");
        }
        if (newWriterIds == null) {
            throw new IllegalArgumentException("writerIds can't be null");
        }
        if (newTypes == null) {
            throw new IllegalArgumentException("types can't be null");
        }
        if (newRestriction == null) {
            restrictions = "";
        } else {
            restrictions = newRestriction;
        }
        if (newCacheParameters == null) {
            cacheParameters = "";
        } else {
            cacheParameters = newCacheParameters;
        }
        writerIds = newWriterIds;
        readerIds = newReaderIds;
        types = newTypes;
    }

    /**
     * Json creator for jackson with all fields.
     *
     * @param readerIds
     *            the reader ids.
     * @param writerIds
     *            the writer ids.
     * @param restrictions
     *            the restrictions.
     * @param types
     *            the types.
     * @param cacheParameters
     *            the cache parameters.
     * @param children
     *            the children map.
     */
    @JsonCreator
    protected VslStructureNodeImpl(@JsonProperty("readerIds") final List<String> readerIds,
            @JsonProperty("writerIds") final List<String> writerIds,
            @JsonProperty("restrictions") final String restrictions,
            @JsonProperty("types") final List<String> types,
            @JsonProperty("cacheParameters") final String cacheParameters,
            @JsonProperty("children") final Map<String, VslMutableStructureNode> children) {
        super(false, children);
        this.readerIds = readerIds;
        this.writerIds = writerIds;
        this.restrictions = restrictions;
        this.types = types;
        this.cacheParameters = cacheParameters;
    }

    @Override
    protected final VslMutableStructureNode createIntermediaryNode(
            final String relativeChildAddress, final VslMutableStructureNode onlyChild) {
        return new VslStructureNodeImpl(relativeChildAddress, onlyChild);
    }

    @Override
    public final List<String> getReaderIds() {
        return readerIds;
    }

    @Override
    public final List<String> getWriterIds() {
        return writerIds;
    }

    @Override
    public final String getRestrictions() {
        return restrictions;
    }

    @Override
    public final String getCacheParameters() {
        return cacheParameters;
    }

    @Override
    public final List<String> getTypes() {
        return types;
    }

    @Override
    public final void setReaderIds(final List<String> readerIds) {
        this.readerIds = new LinkedList<String>(readerIds);
    }

    @Override
    public final void setWriterIds(final List<String> writerIds) {
        this.writerIds = new LinkedList<String>(writerIds);
    }

    @Override
    public final void setRestrictions(final String restrictions) {
        this.restrictions = restrictions;
    }

    @Override
    public final void setCacheParameters(final String cacheParameters) {
        this.cacheParameters = cacheParameters;
    }

    @Override
    public final void setTypes(final List<String> types) {
        this.types = new LinkedList<String>(types);
    }

}
