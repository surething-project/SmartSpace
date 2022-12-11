package org.ds2os.vsl.core.impl;

import java.util.Map;
import java.util.Set;

import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.node.VslStructureNodeData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A KOR update, either an incremental or a full update. Implements the {@link VslKORUpdate}
 * Interface.
 *
 * @author liebald
 * @author felix
 */
public class KORUpdate implements VslKORUpdate {

    /**
     * The map of addresses to {@link VslStructureNodeData}s which were added/changed at these
     * addresses.
     */
    private final Map<String, VslStructureNode> addedNodes;

    /**
     * Get the KOR hash from which this updates originates. If this is the empty string, this is a
     * full update.
     */
    private final String hashFrom;

    /**
     * The KOR hash to which this update will update.
     */
    private final String hashTo;

    /**
     * The set of addresses at which knowledge was removed.
     */
    private final Set<String> removedNodes;

    /**
     * Name of the agent the update is from.
     */
    private final String agentName;

    /**
     * Constructor for a new KOR update.
     *
     * @param hashFrom
     *            Get the KOR hash from which this updates originates. If this is the empty string,
     *            this is a full update.
     * @param hashTo
     *            The KOR hash to which this update will update.
     * @param addedNodes
     *            The map of nodes which were added or changed.
     * @param removedNodes
     *            The set of addresses at which knowledge was removed.
     * @param agentName
     *            Name of the agent the update is from.
     */
    @JsonCreator
    public KORUpdate(@JsonProperty("hashFrom") final String hashFrom,
            @JsonProperty("hashTo") final String hashTo,
            @JsonProperty("addedNodes") final Map<String, VslStructureNode> addedNodes,
            @JsonProperty("removedNodes") final Set<String> removedNodes,
            @JsonProperty("agentName") final String agentName) {
        this.hashFrom = hashFrom;
        this.hashTo = hashTo;
        this.addedNodes = addedNodes;
        this.removedNodes = removedNodes;
        this.agentName = agentName;
    }

    @Override
    public final Map<String, VslStructureNode> getAddedNodes() {
        return addedNodes;
    }

    @Override
    public final String getHashFrom() {
        return hashFrom;
    }

    @Override
    public final String getHashTo() {
        return hashTo;
    }

    @Override
    public final Set<String> getRemovedNodes() {
        return removedNodes;
    }

    @Override
    public final String getAgentName() {
        return agentName;
    }
}
