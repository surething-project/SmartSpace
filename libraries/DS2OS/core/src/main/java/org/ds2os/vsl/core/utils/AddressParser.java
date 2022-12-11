package org.ds2os.vsl.core.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for generic KOR problems.
 *
 * @author liebald
 */
public final class AddressParser {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressParser.class);

    /**
     * Make constructor unusable for utility class.
     */
    private AddressParser() {
    }

    /**
     * Returns all parent addresses for the given address.
     *
     * @param address
     *            address the parents are wanted for.
     * @return List of all parent addresses, empty list if no parents exist. First element is the
     *         direct parent address, last element the root Node.
     */
    public static List<String> getAllParentsOfAddress(final String address) {
        return getAllParentsOfAddress(address, 1);
    }

    /**
     * Returns all parent addresses for the given address.
     *
     * @param address
     *            address the parents are wanted for.
     * @param level
     *            The depth of parents that are wanted (e.g. 2 means only parents with at least 2
     *            "/" are returned)
     * @return List of all parent addresses, empty list if no parents exist for the wanted level.
     *         First element is the direct parent address, last element the parent with the given
     *         level..
     */
    public static List<String> getAllParentsOfAddress(final String address, final int level) {
        String add = address;
        final List<String> result = new LinkedList<String>();
        while (StringUtils.countMatches(add, "/") > level) {
            add = getParentAddress(add);
            result.add(add);
        }
        if (level == 1 && !address.equals("/")) {
            result.add("/");
        }
        return result;
    }

    /**
     * Takes a uri (abc?param1=value1&amp;param2=value2) or the query part of an uri
     * (?param1=value1&amp;param2=value2), parses the given parameters and returns them as a
     * {@link Map}. If the query part of the uri was empty, an empty map is returned.
     *
     * @param uri
     *            The uri to parse. If only the query part is given, the ? can be ommitted.
     * @return The parameters as {@link Map}.
     */
    public static Map<String, String> getParametersFromURIQuery(final String uri) {
        // TODO: remove escaped whitespaces
        // TODO: what if only a key is given, no value?

        final Map<String, String> parameters = new HashMap<String, String>();
        if (uri.isEmpty() || uri.equals("?")) {
            return parameters;
        }
        // separate the uri path from the query if necessary:
        final String[] uriComponents = uri.split("\\?");
        final String query = uriComponents[uriComponents.length - 1];
        // split the query into the single parameters, separated by & or ;:
        for (final String param : query.split("[&;]")) {
            if (param.isEmpty()) {
                continue;
            }
            final String[] parsedParam = param.split("=");
            if (parsedParam.length > 1) {
                parameters.put(parsedParam[0], parsedParam[1]);
            }
        }
        return parameters;
    }

    /**
     * Takes a map of parameters and returns them as URI query element. For example
     * {(param1=value1),(param2=value2)} is returned as "?param1=value1&amp;param2=value2". An empty
     * map returns an empty string as result.
     *
     * @param parameters
     *            Map of all parameters that should be parsed as URI query in String representation.
     * @return The map in its URI query representation.
     */
    public static String getParametersAsURIQuery(final Map<String, String> parameters) {
        // TODO: escape whitespaces
        // TODO: what if only a key is given, no value?
        if (parameters.isEmpty()) {
            return "";
        }
        final StringBuilder query = new StringBuilder();
        query.append("?");
        for (final Iterator<Entry<String, String>> iterator = parameters.entrySet()
                .iterator(); iterator.hasNext();) {
            final Entry<String, String> entry = iterator.next();
            query.append(entry.getKey()).append("=").append(entry.getValue());
            if (iterator.hasNext()) {
                query.append("&");
            }
        }
        return query.toString();
    }

    /**
     * Returns the direct parent of an address.
     *
     * @param address
     *            The address for which a parent is searched.
     * @return The parent of the address.
     */
    public static String getParentAddress(final String address) {
        String parent = address;
        if (address.length() > 1) {
            final int index = address.lastIndexOf('/');
            if (index == 0) {
                parent = "/";
            } else if (index != -1) {
                parent = address.substring(0, index);
            }
        }
        return parent;
    }

    /**
     * Makes an address look like /abcde/fghij/klm. Leading / is added. Trailing / is removed. In
     * addition, parameters are removed from the address (/a/b[2] becomes /a/b)
     *
     * @param address
     *            The address to be well formed.
     * @return The well formed address.
     */
    public static String makeWellFormedAddress(final String address) {
        if (address == null || address.isEmpty()) {
            LOGGER.trace("Well forming NULL");
            return "/";
        }

        String wellFormedAddress = address;

        // Remove parameters from address
        if (wellFormedAddress.contains("?")) {
            wellFormedAddress = address.substring(0, address.indexOf("?"));
        }
        if (wellFormedAddress.equals("/")) {
            return wellFormedAddress;
        }
        if (wellFormedAddress.endsWith("/")) {
            wellFormedAddress = wellFormedAddress.substring(0, wellFormedAddress.length() - 1);
        }
        if (!wellFormedAddress.startsWith("/")) {
            wellFormedAddress = "/" + wellFormedAddress;
        }
        return wellFormedAddress;
    }

    /**
     * Tests if a given string is an integer.
     *
     * @param value
     *            The String to test.
     * @return True if the string is an integer base 10, false if not.
     */
    public static boolean isInteger(final String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (i == 0 && value.charAt(i) == '-') {
                if (value.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(value.charAt(i), 10) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the suffix of the given prefix. Function can be useful when implementing virtual
     * nodes. (see e.g. TickleService). Excludes the leading /.
     *
     * @param addressPrefix
     *            The prefix.
     * @param fullAddress
     *            The full address.
     * @return The suffix that comes after the addressPrefix in fullAddress.
     */
    public static String getSuffix(final String addressPrefix, final String fullAddress) {
        String suffix = "";
        if (addressPrefix.length() < fullAddress.length()) {
            suffix = fullAddress.substring(addressPrefix.length() + 1);
        }
        return suffix;
    }

}
