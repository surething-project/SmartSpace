package org.ds2os.vsl.kor.dataStructures;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * An internal node of the VSL, used inside the KOR to work with nodes (access checks, get,
 * set,...).
 *
 * @author liebald
 */
public class InternalNode extends MetaNode {

    /**
     * Timestamp of the nodes Values last update.
     */
    private Date timestamp;

    /**
     * Value of the node.
     */
    private String value;

    /**
     * Version of the node.
     */
    private long version;

    /**
     * Creates a new InternalVslNode with his type, value, readerIDs and writerIDs.
     *
     * @param newTypes
     *            new Type
     * @param newValue
     *            new Value
     * @param newReaderIDs
     *            new ReaderIDs
     * @param newWriterIDs
     *            new WriterIDs
     * @param newVersion
     *            new versionnumber
     * @param newTimestamp
     *            new Timestamp
     * @param newRestriction
     *            what restrictions does this nodes value have
     * @param newCache
     *            The cache parameters of this node (e.g. TTL).
     */
    public InternalNode(final List<String> newTypes, final String newValue,
            final List<String> newReaderIDs, final List<String> newWriterIDs, final long newVersion,
            final Date newTimestamp, final String newRestriction, final String newCache) {
        super(newTypes, newReaderIDs, newWriterIDs, newRestriction, newCache);
        setValue(newValue);
        if (newTimestamp != null) {
            setTimestamp(newTimestamp);
        } else {
            setTimestamp(null);
        }
        setVersion(newVersion);
    }

    /**
     * Returns the timestamp of the nodes values last change.
     *
     * @return timestamp
     */
    public final Timestamp getTimestamp() {
        if (timestamp == null) {
            return null;
        }
        return new Timestamp(timestamp.getTime());
    }

    /**
     * Returns the value of this node.
     *
     * @return value the value of this node.
     */
    public final String getValue() {
        return value;
    }

    /**
     * Get the versionnumber of this node.
     *
     * @return version of the node
     */
    public final long getVersion() {
        return version;
    }

    /**
     * Sets the timestamp.
     *
     * @param newTimestamp
     *            newTimestamp
     */
    public final void setTimestamp(final Date newTimestamp) {
        if (newTimestamp == null) {
            this.timestamp = null;
        } else {
            this.timestamp = new Timestamp(newTimestamp.getTime());
        }
    }

    /**
     * Set the new value of this node.
     *
     * @param newValue
     *            the new value of this node.
     */
    public final void setValue(final String newValue) {
        value = newValue;
        // setTimestamp(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Set the new version of this node.
     *
     * @param newVersion
     *            the new version number of this node.
     */
    public final void setVersion(final long newVersion) {
        this.version = newVersion;
    }

    @Override
    public final String toString() {
        final StringBuilder s = new StringBuilder();
        s.append("Value: ").append(getValue()).append(" Version: ").append(getVersion())
                .append(" types: ").append(getType()).append("\n").append(" readers: ")
                .append(getReaderIDs()).append(" writers: ").append(getWriterIDs())
                .append(" timestamp: ").append(getTimestamp()).append(" Restriction: ")
                .append(getRestriction());

        return s.toString();
    }

}
