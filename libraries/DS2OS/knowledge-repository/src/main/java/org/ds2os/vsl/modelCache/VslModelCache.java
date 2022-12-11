/*
 * Copyright 2012-2013 Marc-Oliver Pahl, Deniz Ugurlu
 *
 * DS2OS is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version. DS2OS is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public License along
 * with DS2OS. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ds2os.vsl.modelCache;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.exception.InvalidModelException;
import org.ds2os.vsl.exception.ModelIDExistsAlreadyException;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.RequiredDataMissingException;
import org.ds2os.vsl.exception.TypeMissingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;

/**
 * This class represents the model repository. It is responsible for fetching models from the
 * repository. For the moment the repository is local. The concept is to have one or multiple area
 * or global repositories where vendors place their models.
 *
 * @author pahl, liebald
 */
public interface VslModelCache {
    /**
     * Returns a {@link LinkedHashMap} including all nodes of the corresponding model.
     *
     * @param modelID
     *            The ID specifying the model to be retrieved.
     * @param serviceID
     *            ID of the service instantiating the model. The outer most tag gets replaced with
     *            this name. Used for instantiating models under a new name.
     * @return A TreeMap including all relative addresses and the corresponding nodes of the model
     *         as internalNode objects.
     * @throws ModelNotFoundException
     *             Thrown if the model specified by modelID could not be retrieved.
     * @throws InvalidModelException
     *             Thrown if the model specified by modelID could not be parsed properly.
     */
    LinkedHashMap<String, InternalNode> getCompleteModelNodes(String modelID, String serviceID)
            throws ModelNotFoundException, InvalidModelException;

    /**
     * Returns the XML representation of the model specified by modeID.
     *
     * @param modelID
     *            The ID specifying the model to be retrieved.
     * @return The XML representation of the model specified by modelID.
     * @throws ModelNotFoundException
     *             Thrown if the model specified by modelID could not be retrieved.
     */
    String getModel(String modelID) throws ModelNotFoundException;

    /**
     * Returns the XML representation of the model specified by modeID.
     *
     * @param modelID
     *            The ID specifying the model to be retrieved.
     * @param nodeName
     *            New name of the node. The outer most tag gets replaced with this name. Used for
     *            instantiating models under a new name.
     * @return The XML representation of the model specified by modelID.
     * @throws ModelNotFoundException
     *             Thrown if the model specified by modelID could not be retrieved.
     */
    String getModel(String modelID, String nodeName) throws ModelNotFoundException;

    /**
     * Replaces the outermost tag name of modelXML with nodeName.
     *
     * @param modelXML
     *            The XML text to do the replacement inside.
     * @param nodeName
     *            The new tag name for the outermost tag.
     * @return Returns the modelXML text with the replaced outermost tags.
     */
    String replaceOutermostTagnameWith(String modelXML, String nodeName);

    /**
     * Stores the model specified by modeXML in under the ID modelID.
     *
     * @param modelID
     *            The ID the model should be stored at.
     * @param modelXML
     *            The XML of the model. The type property of the enclosing tag is the type to be
     *            saved.
     * @throws ModelIDExistsAlreadyException
     *             Thrown when there is already a model stored under the given modelID (=type
     *             enclosing tag).
     * @throws IOException
     *             Thrown if the model can not be persisted.
     * @throws TypeMissingException
     *             Thrown if there is no type given in the model.
     * @throws RequiredDataMissingException
     *             If the model definition is empty.
     */
    void setModel(String modelID, String modelXML) throws ModelIDExistsAlreadyException,
            IOException, TypeMissingException, RequiredDataMissingException;

    /**
     * Sets the {@link VslConnector} that can be used by the modelcache in order to contact the
     * slmr.
     *
     * @param connector
     *            The {@link VslConnector} to communicate with other KAs.
     */
    void setConnector(VslConnector connector);
}
