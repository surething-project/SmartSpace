package org.ds2os.vsl.mapper.xml;

import java.util.List;
import java.util.Map;

import org.ds2os.vsl.core.node.VslNodeData;
import org.ds2os.vsl.core.node.VslNodeImpl;
import org.ds2os.vsl.core.node.VslNodeImpl.ChildAt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Jackson mixin for XML serialization of VSL nodes.
 *
 * @author felix
 */
@JacksonXmlRootElement(localName = "node")
public interface VslNodeXmlMixin extends VslNodeData {

    /**
     * Used by VslNodeImpl.
     *
     * @return the minimalized types list.
     */
    @JacksonXmlElementWrapper(useWrapping = true, localName = "types")
    @JacksonXmlProperty(localName = "type")
    List<String> getTypesJackson();

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
    @JacksonXmlElementWrapper(useWrapping = true, localName = "children")
    @JacksonXmlProperty(localName = "child")
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
    @JacksonXmlElementWrapper(useWrapping = true, localName = "children")
    @JacksonXmlProperty(localName = "child")
    void loadChildList(List<ChildAt> childList);
}
