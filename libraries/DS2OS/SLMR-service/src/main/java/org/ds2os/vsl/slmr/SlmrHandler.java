package org.ds2os.vsl.slmr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.config.VslModelRepositoryConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for answering requests to the virtual Node.
 *
 * @author gaßmann
 * @author liebald
 */
public class SlmrHandler extends VirtualNodeAdapter implements VslVirtualNodeHandler {

    /**
     * You can use the logger for logs.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SlmrHandler.class);

    /**
     * Access to the model repository related configuration.
     */
    private final VslModelRepositoryConfig config;

    /**
     * The address of the virtualNode this handler is responsible for.
     */
    private final String vNodeAddress;

    /**
     * The {@link VslConnector} used for accessing the Vsl.
     */
    private final VslConnector con;

    /**
     * Constructor.
     *
     * @param vNodeAddress
     *            The address of the virtualNode this handler is responsible for.
     * @param config
     *            Access to the model repository related configuration.
     * @param con
     *            The {@link VslConnector} used for accessing the Vsl.
     */
    public SlmrHandler(final String vNodeAddress, final VslModelRepositoryConfig config,
            final VslConnector con) {
        this.config = config;
        this.vNodeAddress = vNodeAddress;
        this.con = con;
    }

    /**
     * Returns the model XML of the given modelID.
     *
     * @param modelID
     *            The id of the model that should be returned.
     * @return requested model as String in XML.
     * @throws ModelNotFoundException
     *             Thrown if the model doesn't exist.
     */
    private String getModel(final String modelID) throws ModelNotFoundException {

        String model = getFromLocal(modelID);
        if (model != null && !model.isEmpty()) {
            return model;
        }
        model = tryLoadFromCMR(modelID);
        saveLocally(modelID, model);
        return model;
    }

    /**
     * Saves the given model in the local model storage/cache.
     *
     * @param modelID
     *            ID of the model that should be stored.
     * @param content
     *            XML of the model that should be stored
     */
    private void saveLocally(final String modelID, final String content) {
        final File file = new File(config.getLocalModelFolderPath(), modelID + ".xml");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            LOGGER.debug("couldn't create necessary folders for path {} and file {}",
                    config.getLocalModelFolderPath(), modelID + ".xml");
            return;
        }
        final String path = file.getAbsolutePath();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path, config.getCharset());
            writer.write(content);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Returns the model for the given modelID from the local model cache as XML string.
     *
     * @param modelID
     *            ID of the requested model.
     * @return the requested model in xml format.
     */
    private String getFromLocal(final String modelID) {
        try {
            final File modelFile = new File(config.getLocalModelFolderPath(), modelID + ".xml");
            if (modelFile.exists()) {
                return readFile(config.getLocalModelFolderPath(), modelID + ".xml");
            } else {
                final URL resource = getClass().getResource("/models" + modelID + ".xml");
                if (resource != null) {
                    return readFileAsStringFromRessource(resource);
                }
                return "";
            }
        } catch (final IOException e) {
            return "";
        }
    }

    /**
     * Reads a file from the resource folder into a String.
     *
     * @param resource
     *            The resource to be read.
     * @return A String containing the content of the file.
     * @throws java.io.IOException
     *             If the file is not existing.
     */
    private String readFileAsStringFromRessource(final URL resource) throws IOException {
        final StringBuilder result = new StringBuilder("");
        // Get file from resources folder
        if (resource == null) {
            throw new IOException("couldn't load " + resource + "from ressource folder");
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
     * Tries to load the model with the given modelID from the CMR.
     *
     * @param modelID
     *            id of the model.
     * @return Model as string in XML format.
     * @throws ModelNotFoundException
     *             thrown if the model could not be retrieved from the CMR.
     */
    protected String tryLoadFromCMR(final String modelID) throws ModelNotFoundException {
        try {
            return getTextFromURL(config.getCMRurl() + "/" + modelID + ".xml");
        } catch (final Exception e) {
            LOGGER.error("couldn't load model {} from the CMR {}", modelID,
                    config.getCMRurl() + modelID + ".xml", e);
            throw new ModelNotFoundException(String.format(
                    "ModelID %s not found in CMR. "
                            + "Usage: get /agent/slmr/modelID (e.g. /agent/slmr/basic/text)",
                    modelID));
        }
    }

    /**
     * Parses the string returned from connecting to a given url.
     *
     * @param url
     *            The url to connect to (CMR).
     * @return The response from the connection as String.
     * @throws Exception
     *             thrown if something unexpected happens.
     */
    private String getTextFromURL(final String url) throws Exception {
        final URL website = new URL(url);
        final URLConnection connection = website.openConnection();
        final BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), config.getCharset()));

        final StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();

        return response.toString();
    }

    /**
     * Reads a file on the given path with the given name and returns its content.
     *
     * @param basePath
     *            Path of the file.
     * @param fileName
     *            Name of the file.
     * @return content of the file.
     * @throws IOException
     *             Thrown if the file could not be read.
     */
    private String readFile(final String basePath, final String fileName) throws IOException {
        final int pageSize = 4096;
        final StringBuilder builder = new StringBuilder(pageSize);
        final char[] page = new char[pageSize];
        final Reader file = new InputStreamReader(
                new FileInputStream(basePath + File.separator + fileName), "UTF-8");
        try {
            int read;
            do {
                read = file.read(page);
                if (read > 0) {
                    builder.append(String.copyValueOf(page, 0, read));
                }
            } while (read > 0);
        } finally {
            file.close();
        }
        return builder.toString();
    }

    @Override
    public final VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        final String modelID = address.substring(vNodeAddress.length());
        return con.getNodeFactory().createImmutableLeaf(getModel(modelID));
    }
}
