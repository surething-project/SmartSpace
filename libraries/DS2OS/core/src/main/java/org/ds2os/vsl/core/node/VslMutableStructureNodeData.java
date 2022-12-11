package org.ds2os.vsl.core.node;

import java.util.List;

/**
 * Data fields of a node in the VSL.
 *
 * @author liebald
 */
public interface VslMutableStructureNodeData {

    /**
     * Set the readerIds of this node.
     *
     * @param readerIds
     *            the readerIds assigned to this node.
     */
    void setReaderIds(List<String> readerIds);

    /**
     * Set the writerIds of this node.
     *
     * @param writerIds
     *            the writerIds assigned to this node.
     */
    void setWriterIds(List<String> writerIds);

    /**
     * Set the restrictions on this node.
     *
     * @param restrictions
     *            the restrictions assigned to this node.
     */
    void setRestrictions(String restrictions);

    /**
     * Set the cache Parameters of this node.
     *
     * @param cacheParameters
     *            the cache Parameters assigned to this node.
     */
    void setCacheParameters(String cacheParameters);

    /**
     * Set the types of this node.
     *
     * @param types
     *            The types assigned to this node.
     */
    void setTypes(List<String> types);
}
