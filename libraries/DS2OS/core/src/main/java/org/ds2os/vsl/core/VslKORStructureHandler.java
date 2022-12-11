package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.exception.NodeNotExistingException;

/**
 * Interface for accessing Structure information of the KOR.
 *
 * @author liebald
 */
public interface VslKORStructureHandler {

    /**
     * Returns the structure information for the given node and his children.
     *
     * @param address
     *            The address of the root node for the request.
     * @return {@link VslStructureNode} containing the requested informations
     * @throws NodeNotExistingException
     *             Thrown if the existing node doesn't exist.
     */
    VslStructureNode getStructure(String address) throws NodeNotExistingException;

}
