package org.ds2os.vsl.mapper.protobuf;

import java.util.List;
import java.util.Map;

import org.ds2os.vsl.core.node.VslNodeData;
import org.ds2os.vsl.core.node.VslNodeImpl;
import org.ds2os.vsl.core.node.VslNodeImpl.ChildAt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mixin for protobuf serialization of VSL nodes.
 *
 * @author felix
 */
public interface VslNodeProtobufMixin extends VslNodeData {

    /**
     * Used by VslNodeImpl.
     *
     * @return the child map for Jackson.
     */
    @JsonIgnore
    Map<String, ?> getChildMapForJackson();

    /**
     * Used by VslNodeImpl.
     *
     * @return the list of {@link ChildAt} entries for Jackson.
     */
    @JsonIgnore(false)
    @JsonProperty("children")
    List<ChildAt> getDirectChildList();

    /**
     * Used by VslNodeImpl.
     *
     * @param addressToChildMap
     *            the map of addresses to child nodes.
     */
    @JsonIgnore
    void loadChildMap(Map<String, VslNodeImpl> addressToChildMap);

    /**
     * Used by VslNodeImpl.
     *
     * @param childList
     *            the list of {@link ChildAt} entries.
     */
    @JsonIgnore(false)
    @JsonProperty("children")
    void loadChildList(List<ChildAt> childList);
}
