package org.ds2os.vsl.core.utils;

import java.util.Map;

/**
 * Interface to encapsulate parameters for requests.
 *
 * @author liebald
 */
public interface VslAddressParameters {

    /**
     * Enum that defines the options which data shall be returned by the database.
     *
     */
    public enum NodeInformationScope {
        /**
         * All available content is addressed.
         */
        COMPLETE,

        /**
         * Only the value of affected nodes is addressed.
         */
        VALUE,

        /**
         * Only Metadata of affected nodes is addressed.
         */
        METADATA
    }

    /**
     * Returns the scope the affected operation should affect (e.g. value only). Used by get
     * operations.
     *
     * @return Enum {@link NodeInformationScope} that describes the requested scope.
     */
    NodeInformationScope getNodeInformationScope();

    /**
     * Sets the scope the affected operation should affect (e.g. value only).Used by get operations.
     *
     * @param scope
     *            The requested scope.
     * @return the updated {@link VslAddressParameters} object.
     */
    VslAddressParameters withNodeInformationScope(NodeInformationScope scope);

    /**
     * Depth of the tree affected by a query. -1 means complete subtree, 0 means node only
     * (default), 1 means node + direct children, etc. Used by get, subscribe and unsubscribe
     * operations.
     *
     * @return The depth of the subtree that should be affected by a query.
     */
    int getDepth();

    /**
     * Sets the requested depth of the request.-1 means complete subtree, 0 means node only
     * (default), 1 means node + direct children, etc. Used by get operations.
     *
     * @param depth
     *            The requested depth.
     * @return the updated {@link VslAddressParameters} object.
     */
    VslAddressParameters withDepth(int depth);

    /**
     * Requested Version of a Node/subtree. -1 means latest version subtree, any other positive
     * value specifies the desired version. Used by get operations.
     *
     * @return The version of the subtree that is requested by a get query.
     */
    int getRequestedVersion();

    /**
     * Sets the requested Version of a Node/subtree. -1 means latest version subtree, any other
     * positive value specifies the desired version. Used by get operations.
     *
     * @param version
     *            The requested version.
     * @return the updated {@link VslAddressParameters} object.
     */
    VslAddressParameters withVersion(int version);

    /**
     * Set to false if no parameters were given, true otherwise.
     *
     * @return true if parameters were included in the request. False otherwise, therefore the
     *         defaults are used.
     */
    boolean areParamsGiven();

    /**
     * Returns all stored parameters as a {@link Map} of key value pairs. Default values are
     * Omitted.
     *
     * @return Unmodifiable map of all stored parameters.
     */
    Map<String, String> getParametersAsMap();
}
