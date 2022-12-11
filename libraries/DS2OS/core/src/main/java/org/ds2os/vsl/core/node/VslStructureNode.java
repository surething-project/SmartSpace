package org.ds2os.vsl.core.node;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * VSL structure node with data and children.
 *
 * @author liebald
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = VslStructureNodeImpl.class)
public interface VslStructureNode extends VslStructureNodeData, VslTreeComponent<VslStructureNode> {
}
