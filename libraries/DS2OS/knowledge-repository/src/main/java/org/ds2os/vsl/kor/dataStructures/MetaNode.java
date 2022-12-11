package org.ds2os.vsl.kor.dataStructures;

import java.util.ArrayList;
import java.util.List;

import org.ds2os.vsl.core.VslIdentity;

/**
 * MetaNodes are used internal to store metadata retrieved from the database in order to process
 * them (e.g. reader, writer, types)
 *
 * @author liebald
 */
public class MetaNode {

    /**
     * List of all IDs which can read (get) the Nodes value.
     */
    private List<String> readerIDs;

    /**
     * The constraints on this nodes value (e.g. a regular expression or a numerical range).
     */
    private String restriction;

    /**
     * The cache parameters of this node (e.g. TTL).
     */
    private String cacheParameters;

    /**
     * Types of the node.
     */
    private List<String> types;

    /**
     * List of all IDs which can write (set) the Nodes value.
     */
    private List<String> writerIDs;

    /**
     * Creates a new MetaNode based on the given information about the type, readers and writers.
     * Null values are internally replaced by empty lists.
     *
     * @param newTypes
     *            the type information of the node
     * @param newReaderIDs
     *            who can read a node
     * @param newWriterIDs
     *            who can write a node
     * @param newRestriction
     *            what restrictions does this nodes value have
     * @param newCache
     *            The cache parameters of this node (e.g. TTL).
     */
    public MetaNode(final List<String> newTypes, final List<String> newReaderIDs,
            final List<String> newWriterIDs, final String newRestriction, final String newCache) {
        if (newReaderIDs == null) {
            setReaderIDs(new ArrayList<String>());
        } else {
            setReaderIDs(new ArrayList<String>(newReaderIDs));
        }

        if (newWriterIDs == null) {
            setWriterIDs(new ArrayList<String>());
        } else {
            setWriterIDs(new ArrayList<String>(newWriterIDs));
        }

        if (newTypes == null) {
            setType(new ArrayList<String>());
        } else {
            setType(new ArrayList<String>(newTypes));
        }
        setRestriction(newRestriction);
        setCacheParameters(newCache);
    }

    /**
     * Adds a readerID to the existing readerIDs of this node.
     *
     * @param readerID
     *            readerID
     */
    public final void addReaderID(final String readerID) {
        this.readerIDs.add(readerID);
    }

    /**
     * Adds a writerID to the existing writerIDs of this node.
     *
     * @param writerID
     *            writerID
     */
    public final void addWriterID(final String writerID) {
        this.writerIDs.add(writerID);
    }

    /**
     * Returns the readerIDs of this node.
     *
     * @return readerIDs
     */
    public final List<String> getReaderIDs() {
        return readerIDs;
    }

    /**
     * Returns the restriction of this nodes value.
     *
     * @return the restriction
     */
    public final String getRestriction() {
        return restriction;
    }

    /**
     * Returns the Cache parameters of this nodes.
     *
     * @return the Cache parameters
     */
    public final String getCacheParameters() {
        return cacheParameters;
    }

    /**
     * Returns the types of this node.
     *
     * @return all types of the node
     */
    public final List<String> getType() {
        return types;
    }

    /**
     * Returns the writerIDs of this node.
     *
     * @return writerIDs
     */
    public final List<String> getWriterIDs() {
        return writerIDs;
    }

    /**
     * Checks if any of the given identities ID matches one of the nodes readerIDs.
     *
     * @param identity
     *            A single identity as a String
     * @return result
     */
    public final boolean isReadableBy(final String identity) {
        if (readerIDs.contains(identity) || readerIDs.contains("*")) {
            return true;
        }
        return false;
    }

    /**
     * Checks if any of the given identities IDs matches one of the nodes readerIDs.
     *
     * @param identity
     *            A list of identities as VslIdentity
     * @return result
     */
    public final boolean isReadableBy(final VslIdentity identity) {
        if (readerIDs.contains(identity.getClientId())) {
            return true;
        }
        return matchItem(identity, readerIDs);
    }

    /**
     * Checks if any of the given identities ID matches one of the nodes readerIDs.
     *
     * @param identity
     *            A single identity as a String
     * @return result
     */
    public final boolean isWritableBy(final String identity) {
        if (writerIDs.contains("*") || writerIDs.contains(identity)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if any of the given identities IDs matches one of the nodes writerIDs.
     *
     * @param identity
     *            A list of identities as VslIdentity
     * @return result
     */
    public final boolean isWritableBy(final VslIdentity identity) {
        if (writerIDs.contains(identity.getClientId())) {
            return true;
        }
        return matchItem(identity, writerIDs);
    }

    /**
     * Checks if at least one item (ID) in the given VslIdentity and the List (reader/writer) match.
     *
     * @param identity
     *            identity
     * @param target
     *            List to compare the identity Collection with
     * @return result
     */
    private boolean matchItem(final VslIdentity identity, final List<String> target) {
        if (target.contains("*")) {
            return true;
        }
        for (final String s : identity.getAccessIDs()) {
            if (target.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the readerIDs of this node.
     *
     * @param newReaderIDs
     *            new readerIDs
     */
    public final void setReaderIDs(final List<String> newReaderIDs) {
        this.readerIDs = newReaderIDs;
    }

    /**
     * Set the restriction of this nodes value.
     *
     * @param newRestriction
     *            the restrictions as string
     */
    public final void setRestriction(final String newRestriction) {
        restriction = newRestriction;
    }

    /**
     * Set the Cache parameters of this nodes value.
     *
     * @param newCacheParameters
     *            the cache parameters as string
     */
    public final void setCacheParameters(final String newCacheParameters) {
        cacheParameters = newCacheParameters;
    }

    /**
     * Set the new types of this node.
     *
     * @param newType
     *            the new Type of this node.
     */
    public final void setType(final List<String> newType) {
        types = newType;
    }

    /**
     * Sets the writerIDs of this node.
     *
     * @param newWriterIDs
     *            new writerIDs
     */
    public final void setWriterIDs(final List<String> newWriterIDs) {
        this.writerIDs = newWriterIDs;
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append(" types: ").append(getType()).append(" readers: ").append(getReaderIDs())
                .append(" writers: ").append(getWriterIDs()).append(" Restriction: ")
                .append(getRestriction());

        return s.toString();
    }

}
