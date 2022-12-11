package org.ds2os.vsl.core.node;

import java.util.Date;

/**
 * Mutable variant of {@link VslNodeData}.
 *
 * @author felix
 */
public interface VslMutableNodeData extends VslNodeData {

    /**
     * Set the new value of this node or null to not set any value.
     *
     * @param newValue
     *            the new value of this node.
     */
    void setValue(String newValue);

    /**
     * Set the timestamp of this node.
     *
     * @param timestamp
     *            The new timestamp.
     */
    void setTimestamp(Date timestamp);

    /**
     * Set the version of this node.
     *
     * @param version
     *            The new version.
     */
    void setVersion(long version);
}
