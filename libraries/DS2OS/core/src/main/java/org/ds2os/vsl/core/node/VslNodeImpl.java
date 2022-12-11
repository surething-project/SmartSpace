package org.ds2os.vsl.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A node of the VSL, as transferred by get and set operations.
 *
 * @author felix
 */
public class VslNodeImpl extends AbstractTreeComponent<VslNode, VslMutableNode>
        implements VslMutableNode {

    /**
     * The VSL type string describing the type of this node.
     */
    private final List<String> types;

    /**
     * The value of this node, if it has a value or null otherwise.
     */
    private String value;

    /**
     * The timestamp when the value of the node was set.
     */
    private Date timestamp;

    /**
     * The version of this node.
     */
    private long version;

    /**
     * The restrictions of this node.
     */
    private final Map<String, String> restrictions;

    /**
     * The access flags of this node.
     */
    private final String access;

    /**
     * Constructor of a new node cloning an existing node without its children.
     *
     * @param clone
     *            the node to clone or null will create an intermediary node.
     */
    protected VslNodeImpl(final VslNode clone) {
        this(clone == null ? null : clone.getTypes(), clone == null ? null : clone.getValue(),
                clone == null ? null : clone.getTimestamp(),
                clone == null ? -1 : clone.getVersion(), clone == null ? "" : clone.getAccess(),
                clone == null ? null : clone.getRestrictions());
    }

    /**
     * Constructor of a new node with all fields (usually used internally by the connector or KOR
     * for get operations). Also a Jackson {@link JsonCreator}.
     *
     * @param newTypes
     *            the VSL types describing the node's types.
     * @param newValue
     *            the value of this node.
     * @param newTimestamp
     *            The timestamp the nodes value was set.
     * @param newVersion
     *            The version of this node.
     * @param newAccess
     *            The access flags of this node (r, w or empty)
     * @param newRestrictions
     *            The restrictions of this node.
     */
    @JsonCreator
    public VslNodeImpl(@JsonProperty("types") final List<String> newTypes,
            @JsonProperty("value") final String newValue,
            @JsonProperty("timestamp") final Date newTimestamp,
            @JsonProperty("version") final Long newVersion,
            @JsonProperty("access") final String newAccess,
            @JsonProperty("restrictions") final Map<String, String> newRestrictions) {
        types = newTypes;
        value = newValue;
        timestamp = newTimestamp == null ? null : new Date(newTimestamp.getTime());
        version = newVersion == null ? -1L : newVersion;
        if ("r".equals(newAccess) || "w".equals(newAccess) || "-".equals(newAccess)) {
            access = newAccess;
        } else {
            access = "";
        }
        restrictions = newRestrictions == null ? Collections.<String, String>emptyMap()
                : newRestrictions;
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
    protected VslNodeImpl(final String relativeAddress, final VslMutableNode onlyChild) {
        super(relativeAddress, onlyChild);
        types = null;
        value = null;
        timestamp = null;
        version = -1;
        access = "";
        restrictions = Collections.emptyMap();
    }

    /**
     * Constructor of a new parent node with children (used for set operations which set multiple
     * children at once).
     *
     * @param addressToChildMap
     *            Map of the child addresses, relative to this node, to the child node objects (more
     *            children can be added later).
     * @throws IllegalArgumentException
     *             If the map represents an impossible VSL structure.
     */
    public VslNodeImpl(final Map<String, VslNodeImpl> addressToChildMap)
            throws IllegalArgumentException {
        // special treatment of empty relative address for self intialization (only valid here)
        this(addressToChildMap.get(""));

        loadChildMap(addressToChildMap);
    }

    /**
     * Load children from the map into this node (used for set operations which set multiple
     * children at once, not safe under normal conditions). Used e.g. for deserialization.
     *
     * @param addressToChildMap
     *            Map of the child addresses, relative to this node, to the child node objects (more
     *            children can be added later).
     * @throws IllegalArgumentException
     *             If the map represents an impossible VSL structure.
     */
    @JsonProperty("children")
    public final void loadChildMap(final Map<String, VslNodeImpl> addressToChildMap)
            throws IllegalArgumentException {
        for (final Map.Entry<String, VslNodeImpl> entry : addressToChildMap.entrySet()) {
            final String address = entry.getKey();
            final VslNodeImpl child = entry.getValue();
            if ("".equals(address) || child == null) {
                continue;
            }

            // put this child (creating intermediary nodes if necessary)
            final VslNodeImpl oldNode = (VslNodeImpl) putChild(address, child);

            // adding a whole map might scramble the order, i.e. "a/b" might be added before "a" is
            // added, yielding a condition where children of intermediary nodes must be moved.
            // This is not atomic but in a constructor acceptable (adding whole maps at once is not
            // supported as an operation after construction as it cannot be lock-free atomic).
            if (oldNode != null) {
                if (oldNode.getValue() != null) {
                    throw new IllegalArgumentException(
                            "The map effectively contains the same node twice: " + address
                                    + " first value \"" + oldNode.getValue() + "\" second value \""
                                    + child.getValue() + "\".");
                } else {
                    child.mergeChildren(oldNode);
                }
            }
        }
    }

    /**
     * Load children from a {@link ChildAt} list (used for set operations which set multiple
     * children at once, not safe under normal conditions). Used e.g. for deserialization.
     *
     * @param childList
     *            List of the ChildAt descriptors which contain address and child (more children can
     *            be added later).
     * @throws IllegalArgumentException
     *             If the map represents an impossible VSL structure.
     */
    @JsonIgnore
    public final void loadChildList(final List<ChildAt> childList) throws IllegalArgumentException {
        for (final ChildAt entry : childList) {
            final String address = entry.getAddress();
            final VslNodeImpl child = entry.getNode();
            if ("".equals(address) || child == null) {
                continue;
            }

            // put this child (creating intermediary nodes if necessary)
            final VslNodeImpl oldNode = (VslNodeImpl) putChild(address, child);

            // adding a whole map might scramble the order, i.e. "a/b" might be added before "a" is
            // added, yielding a condition where children of intermediary nodes must be moved.
            // This is not atomic but in a constructor acceptable (adding whole maps at once is not
            // supported as an operation after construction as it cannot be lock-free atomic).
            if (oldNode != null) {
                if (oldNode.getValue() != null) {
                    throw new IllegalArgumentException(
                            "The map effectively contains the same node twice: " + address
                                    + " first value \"" + oldNode.getValue() + "\" second value \""
                                    + child.getValue() + "\".");
                } else {
                    child.mergeChildren(oldNode);
                }
            }
        }
    }

    /**
     * Internal helper to merge the children of an old *intermediary* node into this object's own
     * children. This method must only be called under very special conditions as it is not atomic
     * and therefore it is protected and final.
     *
     * @param oldNode
     *            the old *intermediary* node (must not have a value) from which the children are
     *            merged into this object.
     * @throws IllegalArgumentException
     *             If children of the old node and this node collide.
     */
    protected final void mergeChildren(final VslMutableNode oldNode)
            throws IllegalArgumentException {
        for (final Map.Entry<String, VslNode> entry : oldNode.getDirectChildren()) {
            final String address = entry.getKey();
            // FIXME: hacked!
            final VslNodeImpl newChild = (VslNodeImpl) entry.getValue();
            final VslNodeImpl oldChild = (VslNodeImpl) putChild(address, newChild);

            // overwrite intermediary nodes with the real nodes, but two real nodes can't be merged.
            if (oldChild != null) {
                if (oldChild.getValue() == null) {
                    newChild.mergeChildren(oldChild);
                } else if (newChild.getValue() == null) {
                    oldChild.mergeChildren(newChild);
                    putChild(address, oldChild);
                } else {
                    throw new IllegalArgumentException(
                            "Children cannot be merged: " + address + " old value "
                                    + oldChild.getValue() + " new value " + newChild.getValue());
                }
            }
        }
    }

    @Override
    protected final VslMutableNode createIntermediaryNode(final String relativeChildAddress,
            final VslMutableNode onlyChild) {
        return new VslNodeImpl(relativeChildAddress, onlyChild);
    }

    @Override
    public final String getValue() {
        return value;
    }

    @Override
    public final void setValue(final String newValue) {
        value = newValue;
    }

    @Override
    @JsonIgnore
    public final List<String> getTypes() {
        if (types == null) {
            return null;
        } else {
            return Collections.unmodifiableList(types);
        }
    }

    /**
     * Get types for Jackson with minimalization.
     *
     * @return null or types array.
     */
    @JsonProperty("types")
    public final List<String> getTypesJackson() {
        if (types == null || types.isEmpty()) {
            return null;
        } else {
            return Collections.unmodifiableList(types);
        }
    }

    @Override
    public final Date getTimestamp() {
        if (timestamp == null) {
            return null;
        } else {
            return new Date(timestamp.getTime());
        }
    }

    @Override
    public final void setTimestamp(final Date timestamp) {
        if (timestamp == null) {
            this.timestamp = null;
        } else {
            this.timestamp = new Date(timestamp.getTime());
        }
    }

    @Override
    @JsonIgnore
    public final long getVersion() {
        return version;
    }

    @Override
    public final void setVersion(final long version) {
        this.version = version;
    }

    /**
     * Get the version value for Jackson with minimalization.
     *
     * @return null or the version.
     */
    @JsonProperty("version")
    public final Long getVersionJackson() {
        if (version < 0L) {
            return null;
        } else {
            return version;
        }
    }

    @Override
    @JsonIgnore
    public final String getAccess() {
        return access;
    }

    /**
     * Get the access value for Jackson with minimalization.
     *
     * @return null or the access.
     */
    @JsonProperty("access")
    public final String getAccessJackson() {
        if (access == null || access.isEmpty()) {
            return null;
        } else {
            return access;
        }
    }

    @Override
    public final Map<String, String> getRestrictions() {
        return restrictions;
    }

    /**
     * Get the access value for Jackson with minimalization.
     *
     * @return null or the restrictions.
     */
    @JsonProperty("restrictions")
    public final Map<String, String> getRestrictionsJackson() {
        if (restrictions == null || restrictions.isEmpty()) {
            return null;
        } else {
            return restrictions;
        }
    }

    /**
     * Special format of {@link #getDirectChildren()} for special purposes like protobuf
     * serialization.
     *
     * @return a list of {@link ChildAt} entries.
     */
    @JsonIgnore
    public final List<ChildAt> getDirectChildList() {
        final List<ChildAt> result = new ArrayList<ChildAt>();
        for (final Map.Entry<String, VslNode> entry : getDirectChildren()) {
            if (entry.getValue() instanceof VslNodeImpl) {
                result.add(new ChildAt((VslNodeImpl) entry.getValue(), entry.getKey()));
            } else {
                result.add(new ChildAt(new VslNodeImpl(entry.getValue()), entry.getKey()));
            }
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    /**
     * Child with address pair for special purposes.
     *
     * @author felix
     */
    public static final class ChildAt {

        /**
         * The child instance.
         */
        private final VslNodeImpl node;

        /**
         * The address of the child.
         */
        private final String address;

        /**
         * Constructor that also serves as a {@link JsonCreator}.
         *
         * @param node
         *            the child node instance.
         * @param address
         *            the address of the child.
         */
        @JsonCreator
        public ChildAt(@JsonProperty("node") final VslNodeImpl node,
                @JsonProperty("address") final String address) {
            this.node = node;
            this.address = address;
        }

        /**
         * Get the address of the child.
         *
         * @return the child's address.
         */
        public String getAddress() {
            return address;
        }

        /**
         * Get the child node.
         *
         * @return the node instance.
         */
        public VslNodeImpl getNode() {
            return node;
        }
    }
}
