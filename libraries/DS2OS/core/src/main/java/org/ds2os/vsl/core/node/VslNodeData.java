package org.ds2os.vsl.core.node;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data fields of a node in the VSL.
 *
 * @author felix
 */
public interface VslNodeData {

    /**
     * Get the value of this node or null if there is none.
     *
     * @return null or the String of the value.
     */
    String getValue();

    /**
     * Get the type of this node.
     *
     * @return the list of type strings.
     */
    List<String> getTypes();

    /**
     * Get the timestamp when this nodes value was set.
     *
     * @return the timestamp.
     */
    Date getTimestamp();

    /**
     * Get the version of this node.
     *
     * @return the version.
     */
    long getVersion();

    /**
     * Returns the access flags of this node.
     *
     * @return r, w or empty String (=rw).
     */
    String getAccess();

    /**
     * Returns the restrictions of this node as map. Each map entry is one restriction, split into
     * key(what restriction) and value (the restrictions value).
     *
     * @return The restrictions of this node.
     */
    Map<String, String> getRestrictions();
}
