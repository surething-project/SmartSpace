package org.ds2os.vsl.core;

import java.util.Map;
import java.util.Set;

import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.node.VslStructureNode;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * A KOR update, either an incremental or a full update.
 *
 * @author felix
 * @author liebald
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = KORUpdate.class)
public interface VslKORUpdate {

    /**
     * Get the KOR hash from which this updates originates. If this is the empty string, this is a
     * full update.
     *
     * @return the hash from which the update starts or an empty string.
     */
    String getHashFrom();

    /**
     * Get the KOR hash to which this update will update.
     *
     * @return the KOR hash which will be valid after applying the updates.
     */
    String getHashTo();

    /**
     * Get the map of addresses to nodes which are added at these addresses.
     *
     * @return the map from address strings to nodes.
     */
    Map<String, VslStructureNode> getAddedNodes();

    /**
     * Get the set of addresses at which knowledge was removed.
     *
     * @return the set of address string to delete.
     */
    Set<String> getRemovedNodes();

    /**
     * Get the name of the agent the update is from.
     *
     * @return Agentname (e.g. KA1)
     */
    String getAgentName();
}
