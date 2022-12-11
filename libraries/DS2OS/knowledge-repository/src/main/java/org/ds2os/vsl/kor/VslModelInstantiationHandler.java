package org.ds2os.vsl.kor;

import org.ds2os.vsl.exception.InvalidModelException;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeAlreadyExistingException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.ParentNotExistingException;
import org.ds2os.vsl.exception.RequiredDataMissingException;

/**
 * interface that offers the necessary functions to add a model to a list.
 *
 * @author liebald
 *
 */
public interface VslModelInstantiationHandler {

    /**
     * @param fullAddress
     *            The address of the node to hang the serialized nodes at. The root of the new tree
     *            is a direct child of this address.
     * @param modelID
     *            The ID of the model to be added at parentAddress.
     * @param creatorID
     *            ID of the service creating the object. This service gets read and write access
     *            granted.
     * @param rootNodeName
     *            The name of the rootnode of the new model.
     * @throws ParentNotExistingException
     *             if the start node given by address does not exist.
     * @throws NoPermissionException
     *             If lockerID has no write permissions on the node.
     * @throws RequiredDataMissingException
     *             This exception is thrown when adding nodes from a serialized stream (e.g. XML)
     *             and some required properties are missing (e.g. the type="").
     * @throws ModelNotFoundException
     *             If the model specified by ID was not found.
     * @throws NodeNotExistingException
     *             If an address specifies a non-existing node.
     * @throws NodeAlreadyExistingException
     *             Thrown if there is already node at the address the new model should be added.
     * @throws InvalidModelException
     *             Thrown if the model specified by modelID could not be parsed properly.
     */
    void addSubtreeFromModelID(String fullAddress, String modelID, String creatorID,
            String rootNodeName) throws ParentNotExistingException, NoPermissionException,
            RequiredDataMissingException, ModelNotFoundException, NodeNotExistingException,
            NodeAlreadyExistingException, InvalidModelException;

    /**
     * returns the instance of the {@link VslNodeTree} the KOR is working with.
     *
     * @return The VslNodeTree used to new nodes models to the KOR.
     */
    VslNodeTree getVslNodeTree();

    /**
     * Removes a node and it's child from the KOR.
     *
     * @param address
     *            The address of the node
     * @throws NodeNotExistingException
     *             Thrown if the node doesn't exist.
     */
    void removeNode(String address) throws NodeNotExistingException;
}
