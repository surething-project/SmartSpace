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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.exception.InvalidModelException;
import org.ds2os.vsl.exception.ModelIDExistsAlreadyException;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.RequiredDataMissingException;
import org.ds2os.vsl.exception.TypeMissingException;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of a VslModelCache. It retrieves models from a directory on disk.
 * Conceptually the models should be retrieved from a site-local or global server.
 *
 * @author pahl
 * @author liebald
 */
public final class ModelCache implements VslModelCache {

    /**
     * The singleton instance.
     */
    private static ModelCache instance = null;

    /**
     * The connector used to communicate with the slmr.
     */
    private VslConnector con = null;
    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelCache.class);

    /**
     * The folder where the local models are loaded from.
     */
    private static String modelsFolder;

    /**
     * Indicates whether to try to request models from the SLMR when local requests dont find them.
     */
    private boolean trySlmr = true;

    /**
     * The address of the central model repository.
     */
    private String slmrAddress;

    /**
     * Returns an instance of the ModelCache class.
     *
     * @return The singleton instance of this class.
     */
    public static synchronized ModelCache getInstance() {
        if (instance == null) {
            instance = new ModelCache();
            LOGGER.trace("ModelRepository instance created.");
        }
        return instance;
    }

    /**
     * Returns the models folder.
     *
     * @return the path of the models folder
     */
    public static String getModelsFolder() {
        return modelsFolder;
    }

    /**
     * Instantiate the local model repo.
     */
    private ModelCache() {
        // Set basedir from config
        // TODO: set Modelcache according to config
        String basedir = "."; // DssosGlobals.getProperty("ds2os.home");
        if (basedir == null || basedir.isEmpty()) {
            basedir = ".";
        }
        modelsFolder = basedir + File.separator + "models";
        LOGGER.debug("The ModelCache is situated in folder {}.", modelsFolder);
        // here the ModelRepository server may be connected or the existence of
        // the folder may be checked.
        trySlmr = true;
    }

    @Override
    public LinkedHashMap<String, InternalNode> getCompleteModelNodes(final String modelID,
            final String serviceID) throws ModelNotFoundException, InvalidModelException {
        final LinkedHashMap<String, InternalNode> result = getCompleteModelNodes(modelID, serviceID,
                new LinkedHashMap<String, InternalNode>());

        // We want to make sure that Listelements that are predefined by the model are in the same
        // order they were specified. Therefore create the content of the .../list/elements
        // subnodes properly
        final HashMap<String, List<String>> listElements = new HashMap<String, List<String>>();
        // iterate through all model nodes and check if they are lists.
        for (final Entry<String, InternalNode> node : result.entrySet()) {
            // LOGGER.debug("Found node {}", node.getKey());
            String parentNode;
            if (node.getKey().contains("/")) {
                parentNode = node.getKey().substring(0, node.getKey().lastIndexOf("/"));
            } else {
                parentNode = "";
            }

            if (node.getValue().getType().contains("/basic/list")) {
                listElements.put(node.getKey(), new LinkedList<String>());
            } else if (node.getKey().contains("/") && listElements.containsKey(parentNode)) {
                final String element = node.getKey().substring(parentNode.length() + 1);
                if (!(element.equals("add") || element.equals("del")
                        || element.equals("elements"))) {
                    final List<String> elements = listElements.get(parentNode);
                    elements.add(element);
                    listElements.put(parentNode, elements);
                }
            }
        }
        for (final Entry<String, List<String>> listRoot : listElements.entrySet()) {
            result.get(listRoot.getKey() + "/elements").setValue(
                    StringUtils.join(listRoot.getValue(), VslNodeDatabase.LIST_SEPARATOR));

        }
        final List<String> types = result.get(serviceID).getType();

        // add the id of the model itself to its types:
        if (!types.contains(modelID)) {
            final List<String> newTypes = new LinkedList<String>();
            newTypes.add(modelID);
            newTypes.addAll(types);
            result.get(serviceID).setType(newTypes);
        }
        return result;
    }

    /**
     * Used to recursively build the complete model, including nodes added from a models types.
     *
     * @param modelID
     *            modelID to add to the complete model (either the initial id or an type for
     *            recursion)
     * @param serviceID
     *            address of the current model relatively to the initial model
     * @param nodes
     *            TreeMap of nodes where we want to add new nodes or change old ones according to
     *            their types.
     * @return the complete Model for the given modelID(=type)
     * @throws ModelNotFoundException
     *             Thrown if the model specified by modelID could not be retrieved.
     * @throws InvalidModelException
     *             Thrown if the model specified by modelID could not be parsed properly.
     */
    private LinkedHashMap<String, InternalNode> getCompleteModelNodes(final String modelID,
            final String serviceID, final LinkedHashMap<String, InternalNode> nodes)
            throws ModelNotFoundException, InvalidModelException {
        final String serializedSubtree = getModel(modelID);
        ByteArrayInputStream input = null;
        try {
            input = new ByteArrayInputStream(serializedSubtree.getBytes("UTF-8"));
        } catch (final UnsupportedEncodingException e1) {
            LOGGER.error("{} encoding is not supported by your system. Cannot load data!", "UTF-8");
            e1.printStackTrace();
        }

        String overrideFirstNodeName = serviceID;

        final XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLStreamReader parser;

        try {
            parser = factory.createXMLStreamReader(input);
            String currentAddress = "";
            while (parser.hasNext()) {
                String nodeName = null;
                List<String> types = null;
                List<String> readers = null;
                List<String> writers = null;
                String restriction = null;
                String cacheParameters = null;
                String value = null;

                switch (parser.next()) {

                case XMLStreamConstants.START_ELEMENT:
                    nodeName = parser.getLocalName();

                    if (overrideFirstNodeName != null) {
                        nodeName = overrideFirstNodeName;

                    }

                    if (!currentAddress.endsWith("/") && currentAddress.length() > 0) {
                        currentAddress += "/";
                    }
                    currentAddress += nodeName;

                    // Get attributes:
                    for (int i = parser.getAttributeCount() - 1; i >= 0; --i) {
                        final String attrName = parser.getAttributeLocalName(i);
                        if (attrName.equals("type")) {
                            types = new LinkedList<String>();
                            for (final String nextType : parser.getAttributeValue(i).split(",")) {
                                if (!types.contains(nextType.trim())) {
                                    types.add(nextType.trim());
                                }
                            }
                        } else if (attrName.equals("reader")) {

                            readers = new LinkedList<String>();
                            for (final String nextReader : parser.getAttributeValue(i).split(",")) {
                                readers.add(nextReader.trim());
                            }
                        } else if (attrName.equals("writer")) {
                            writers = new LinkedList<String>();
                            for (final String nextWriter : parser.getAttributeValue(i).split(",")) {
                                writers.add(nextWriter.trim());
                            }
                        } else if (attrName.equals("restriction")) {
                            restriction = parser.getAttributeValue(i);
                        } else if (attrName.equals("cacheParameters")) {
                            cacheParameters = parser.getAttributeValue(i);
                        }

                    }
                    // All parameters are collected. (value comes after the START_ELEMENT)
                    // now the recursion happens, check all types from right to left.
                    if (types != null) {

                        for (int type = types.size() - 1; type >= 0; type--) {
                            getCompleteModelNodes(types.get(type), currentAddress, nodes);
                        }
                    }
                    // logger.debug("{}, {}", types, restriction);

                    if (nodes.containsKey(currentAddress)) {
                        if (writers != null) {
                            nodes.get(currentAddress).setWriterIDs(writers);
                        }
                        if (readers != null) {
                            nodes.get(currentAddress).setReaderIDs(readers);
                        }
                        if (types != null) {
                            for (final String type : nodes.get(currentAddress).getType()) {
                                if (!types.contains(type)) {
                                    types.add(type);
                                }
                            }
                            nodes.get(currentAddress).setType(types);
                        }
                        if (restriction != null) {
                            nodes.get(currentAddress).setRestriction(restriction);
                        }
                        if (cacheParameters != null) {
                            nodes.get(currentAddress).setCacheParameters(cacheParameters);
                        }
                    } else {
                        nodes.put(currentAddress, new InternalNode(types, null, readers, writers, 0,
                                null, restriction, cacheParameters));
                    }

                    overrideFirstNodeName = null;

                    break; // /START_ELEMENT

                case XMLStreamConstants.CHARACTERS:
                    if (!parser.isWhiteSpace()) {
                        value = parser.getText();
                        // if we have the current node already stored in our
                        // list, set its value
                        if (nodes.containsKey(currentAddress)) {
                            if (value != null) {
                                nodes.get(currentAddress).setValue(value.trim());
                            } else {
                                nodes.get(currentAddress).setValue("");
                            }
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    nodeName = parser.getLocalName();
                    // LOGGER.trace("At {} trying to remove last element from {}",
                    // nodeName, currentAddress);
                    if (currentAddress.lastIndexOf("/") > 0) {
                        // Remove the last node from the current address:
                        currentAddress = currentAddress.substring(0,
                                currentAddress.lastIndexOf("/"));
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    parser.close();
                    break;
                default:
                    break;
                }
            }
        } catch (final XMLStreamException e) {
            LOGGER.debug("Exception parsing model XML: {}", e.getMessage());
            throw new InvalidModelException("Exception parsing model XML: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(input);
        }
        return nodes;

    }

    @Override
    public String getModel(final String modelId) throws ModelNotFoundException {
        String fileContent = null;
        final String modelID = AddressParser.makeWellFormedAddress(modelId);
        // LOGGER.trace("Request to retrieve model {}.", modelID);
        try {
            // try the resource folder first
            fileContent = readFileAsStringFromRessource("/models" + modelID + ".xml");
            return fileContent.trim();
        } catch (final IOException e) {
            try {
                // if the model isn't included with the ka, try the local cache folder
                fileContent = readFileAsString(modelsFolder + modelID + ".xml");
                return fileContent.trim();
            } catch (final IOException e1) {
                // finally try the SLMR
                if (trySlmr) {
                    LOGGER.debug("Model {} not found in local model repository, " + "trying SLMR.",
                            modelID);
                    try {
                        if (con == null) {
                            throw new ModelNotFoundException("Model not cached locally and no "
                                    + "connector available to contact the SLMR.");
                        }

                        synchronized (con) {
                            if (slmrAddress == null) {
                                final List<String> repoNodes = Arrays
                                        .asList(con.get("/search/type/system/slmr").getValue()
                                                .toString().split("//"));
                                if (repoNodes != null && repoNodes.size() > 0) {
                                    slmrAddress = repoNodes.get(0);
                                }
                                // TODO: test if chosen slmr is working, not just pick the first
                            }

                            if (slmrAddress != null) {
                                final String slmrModelAddress = slmrAddress + modelID;
                                LOGGER.debug("Model not found locally, trying slmr at {}",
                                        slmrModelAddress);
                                final String model = con.get(slmrModelAddress).getValue();
                                if (model != null && !model.isEmpty()) {
                                    // 'cache' the model in the local repo
                                    try {
                                        setModel(modelID, model);
                                    } catch (final Exception e2) {
                                        LOGGER.error(
                                                "Cannot cache model with id {} from SLMR, as the id"
                                                        + " already exists in the local cache: {}",
                                                modelID, e2.getMessage());
                                    }
                                    return model;
                                }
                                throw new ModelNotFoundException(
                                        "Couldn't find the model " + modelID);
                            } else {
                                LOGGER.error("SLMRs location could not be determined.");
                            }
                        }
                    } catch (final Exception e2) {
                        throw new ModelNotFoundException(
                                "The model with the ID " + modelID + " could not be found");
                    }
                }
                // if we arrive here without finding a model
                throw new ModelNotFoundException(
                        "The model with the ID " + modelID + " could not be found in folder "
                                + modelsFolder + " or in the model repository");
            }
        }
    }

    @Override
    public String getModel(final String modelID, final String nodeName)
            throws ModelNotFoundException {
        return replaceOutermostTagnameWith(getModel(modelID), nodeName);
    }

    /**
     * Reads a file into a String.
     *
     * @param filePath
     *            The file to be read.
     * @return A String containing the content of the file.
     * @throws java.io.IOException
     *             If the file is not existing.
     */
    private String readFileAsString(final String filePath) throws java.io.IOException {
        final byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) {
                IOUtils.closeQuietly(f);
            }
        }
        return new String(buffer, "UTF-8");
    }

    /**
     * Reads a file from the resource folder into a String.
     *
     * @param resourcePath
     *            The resource to be read.
     * @return A String containing the content of the file.
     * @throws java.io.IOException
     *             If the file is not existing.
     */
    private String readFileAsStringFromRessource(final String resourcePath)
            throws java.io.IOException {
        final StringBuilder result = new StringBuilder("");
        // Get file from resources folder
        final URL resource = getClass().getResource(resourcePath);
        // LOGGER.debug("path: {}, res: {}", resourcePath, resource);
        if (resource == null) {
            throw new IOException("couldn't load " + resourcePath + "from ressource folder");
        }

        final Reader reader = new InputStreamReader(resource.openStream(), "UTF-8");

        // File file = new File(resource.getFile());
        final Scanner scanner = new Scanner(reader);
        while (scanner.hasNextLine()) {
            final String line = scanner.nextLine();
            result.append(line).append("\n");
        }
        scanner.close();
        reader.close();
        return result.toString();
    }

    /**
     * Removes the persisted model from the repository by deleting the physical representation. If
     * no model with the specified ID can be found nothing will be done.
     *
     * @param modelId
     *            the model id to delete.
     */
    public void removeModel(final String modelId) {
        final File f = new File(modelsFolder + "/" + modelId + ".xml");
        if (f.exists() && !f.delete()) {
            LOGGER.info("Could not delete model at {}", f.getAbsoluteFile());
        }
    }

    /**
     * Replaces the outermost tag name of modelXML with nodeName.
     *
     * @param modelXML
     *            The XML text to do the replacement inside.
     * @param nodeName
     *            The new tag name for the outermost tag.
     * @return Returns the modelXML text with the replaced outermost tags.
     */
    @Override
    public String replaceOutermostTagnameWith(final String modelXML, final String nodeName) {
        final String trimmedModelXML = modelXML.trim();
        if (trimmedModelXML.isEmpty()) {
            LOGGER.warn("The model XML for node {} is empty?!", nodeName);
            return null;
        }
        String openTag = trimmedModelXML;
        // TODO What if empty? -2 error
        try {
            final int firstSpace = trimmedModelXML.indexOf(" ");
            final int firstBracket = trimmedModelXML.indexOf(">");
            openTag = trimmedModelXML.substring(1,
                    ((firstSpace == -1) || firstBracket < firstSpace ? firstBracket : firstSpace));
        } catch (final IndexOutOfBoundsException e) {
            LOGGER.warn(
                    "Outermost tag (\"{}\") has no properties and especially "
                            + "no type and can thus not be renamed to {}. No renaming is done.",
                    openTag, nodeName);
            return null;
        }

        // LOGGER.trace("Replaced outermost Tag {} with {}.", openTag, nodeName);
        return "<" + nodeName + trimmedModelXML.substring(openTag.length() + 1,
                trimmedModelXML.length() - openTag.length() - 1) + nodeName + ">";
    }

    @Override
    public void setModel(final String modelID, final String modelXML)
            throws ModelIDExistsAlreadyException, TypeMissingException, IOException,
            RequiredDataMissingException {
        String model = modelID;
        if (model == null || model.isEmpty()) {
            LOGGER.warn("Request to save a model with empty or null ID won't be served.");
            return;
        }
        if (!model.startsWith("/")) {
            model = "/" + model;
        }
        final String typePropertyID = " type=\"";
        if (modelXML == null || modelXML.isEmpty()) {
            throw new RequiredDataMissingException(
                    "Cannot store model " + modelID + " because the model definition was empty!");
        }
        final int typeStartsAt = modelXML.indexOf(typePropertyID) + typePropertyID.length();

        if (typeStartsAt == -1) {
            throw new TypeMissingException("No type given.");
        }
        final int typeEndsAt = modelXML.indexOf("\"", typeStartsAt);
        // logger.debug("Request to save a model with the ID {}: \n{}.", model, modelXML);

        String typeString = "";
        try {
            typeString = modelXML.substring(typeStartsAt, typeEndsAt);
        } catch (final IndexOutOfBoundsException e) {
            LOGGER.warn("Could not determine type of the model {}", modelID);
        }
        String xmlToStore = "";
        // derived types contain multiple types... myType, derivedFromType1, derivedFromType2, ...
        if (typeString.contains(",")) {
            final String[] models = typeString.split(",");
            // Remove unnecessary white spaces:
            final StringBuilder xmlRepresentation = new StringBuilder();
            xmlRepresentation.append(modelXML.substring(0, typeStartsAt));
            boolean moreThanOne = false;
            for (final String nextModel : models) {
                if (moreThanOne) {
                    xmlRepresentation.append(",");
                }
                final String nextModelID = nextModel.trim();
                if (!nextModelID.equals(model)) {
                    xmlRepresentation.append(nextModelID);
                    moreThanOne = true;
                    // TODO: infinite loops might still work with another model in between.
                } else {
                    LOGGER.warn(
                            "The XML definition for model {} contains itself in the "
                                    + "types ({}) (=endless loop). Removed. XML: {}",
                            model, typeString, modelXML);
                }

            }
            xmlRepresentation.append(modelXML.substring(typeEndsAt));
            xmlToStore = xmlRepresentation.toString();
        } else {
            xmlToStore = modelXML;
        }
        final String nodeName = model.substring(model.lastIndexOf("/") + 1);
        // logger.debug("Trying to save model under ID {}.", model);

        final String modelFileNameWithPath = modelsFolder + model + ".xml";
        final File xmlFile = new File(modelFileNameWithPath);
        if (xmlFile.exists()) {
            LOGGER.warn("A model with ID {} already exists locally ({})", model,
                    modelFileNameWithPath);
            throw new ModelIDExistsAlreadyException("A model with ID " + model
                    + " already exists locally (" + modelFileNameWithPath + ").");
        }

        // Create paths:
        final int pathLength = model.lastIndexOf("/");
        if (pathLength != -1) {
            final String path = modelsFolder + "/" + model.substring(0, pathLength);
            final File fPath = new File(path);
            if (!fPath.exists() && fPath.mkdirs()) {
                LOGGER.trace("Created directories {}.", path);
            } else if (!fPath.exists()) {
                LOGGER.warn("Path {} for model {} could not be created.", path, model);
            }
        }
        // Create file:
        // logger.trace("Trying to create file {}", xmlFile.getAbsolutePath());
        if (!xmlFile.createNewFile()) {
            LOGGER.warn("File {} for model {} could not be created.", modelFileNameWithPath, model);
        }

        // Replace opening and closing tag:
        final String fileContent = replaceOutermostTagnameWith(xmlToStore, nodeName);

        if (fileContent == null) {
            LOGGER.warn("Could not prepare model to store locally, aborting.");
            return;
        }
        FileOutputStream fstream = null;
        try {
            fstream = new FileOutputStream(modelFileNameWithPath);
            final byte[] content = fileContent.getBytes("UTF-8");
            fstream.write(content, 0, content.length);
            fstream.flush();
            fstream.close();
            // LOGGER.debug("Stored new model {} at {}: \n{}", model, modelFileNameWithPath,
            // fileContent);
        } catch (final Exception e) {
            LOGGER.error("There was a problem opening the stream for the newly created file {}: {}",
                    modelFileNameWithPath, e.getMessage());
        } finally {
            if (fstream != null) {
                IOUtils.closeQuietly(fstream);
            }
        }
    }

    @Override
    public void setConnector(final VslConnector connector) {
        this.con = connector;
    }
}
