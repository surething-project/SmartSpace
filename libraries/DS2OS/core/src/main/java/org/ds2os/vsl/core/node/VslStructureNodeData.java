package org.ds2os.vsl.core.node;

import java.util.List;

/**
 * Data fields of a node in the VSL.
 *
 * @author liebald
 */
public interface VslStructureNodeData {

    /**
     * Get the readerIds of this node.
     *
     * @return the readerIds assigned to this node.
     */
    List<String> getReaderIds();

    /**
     * Get the writerIds of this node.
     *
     * @return the writerIds assigned to this node.
     */
    List<String> getWriterIds();

    /**
     * Get the restrictions on this node.
     *
     * @return the restrictions assigned to this node.
     */
    String getRestrictions();

    /**
     * Get the cache Parameters of this node.
     *
     * @return the cache Parameters assigned to this node.
     */
    String getCacheParameters();

    /**
     * Get the types of this node.
     *
     * @return The types assigned to this node.
     */
    List<String> getTypes();
}
