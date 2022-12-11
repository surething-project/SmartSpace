package org.ds2os.vsl.kor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class provides methods to evaluate the restrictions that come with certain models.
 *
 * @author liebald
 */
public final class Restrictions {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Restrictions.class);

    /**
     * This method evaluates if a given value is matched by a restriction String.
     *
     * @param value
     *            The value to be evaluated.
     * @param restrictions
     *            The restrictions that should be checked. Format:
     *            restriction1='restriction',restriction2='restriction'
     * @return True if all restrictions are matched or empty, false otherwise
     */
    public static boolean evaluateNumberText(final String value, final String restrictions) {
        String valueToTest = value;
        if (restrictions == null || restrictions.equals("")) {
            return true;
        }
        if (value == null) {
            valueToTest = "";
        }

        Boolean result = true;

        // Split the restrictions (e.g. minimumValue, maximumValue)
        for (final String restriction : getRestrictionArray(restrictions)) {
            final String[] s = parseRestriction(restriction);
            if (s[0].equals("minimumValue")) {
                try {
                    if (Integer.parseInt(valueToTest) < Integer.parseInt(s[1])) {
                        result = false;
                    }
                } catch (final Exception e) {
                    result = false;
                }
            } else if (s[0].equals("maximumValue")) {
                try {
                    if (Integer.parseInt(valueToTest) > Integer.parseInt(s[1])) {
                        result = false;
                    }
                } catch (final Exception e) {
                    result = false;
                }
            } else if (s[0].equals("regularExpression") && !valueToTest.matches(s[1])) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Returns a List of all allowed types a VSL list can have as entries. If all types are allowed
     * or the typeRestriction couldn't be parsed, null is returned;
     *
     * @param restrictions
     *            The restrictions string to parse.
     * @return The List of allowed types. If all types are allowed, null is returned.
     */
    public static List<String> getAllowedListTypes(final String restrictions) {
        if (restrictions == null || restrictions.isEmpty()) {
            return null;
        }
        List<String> allowedTypes = null;
        for (final String restriction : getRestrictionArray(restrictions)) {
            final String[] s = parseRestriction(restriction);

            if (s[0].equals("allowedTypes")) {

                try {
                    allowedTypes = new ArrayList<String>(
                            Arrays.asList(StringUtils.split(s[1], ";,")));
                    // LOGGER.debug(allowedTypes.toString());
                } catch (final Exception e) {
                    LOGGER.error("Exception retrieving allowedTypes restrictions: {}",
                            e.getMessage());
                }
            }
        }
        return allowedTypes;
    }

    /**
     * Returns the maximum amount of entries that is allowed in a VSL list, extracted from the
     * provided restricitons String. If this value isn't specified or invalid, Interger.MAX_VALUE is
     * returned.
     *
     * @param restrictions
     *            The restrictions string to parse.
     * @return The maximum amount of entries a VSL list can have.
     */
    public static int getMaxListEntries(final String restrictions) {
        for (final String restriction : getRestrictionArray(restrictions)) {
            final String[] s = parseRestriction(restriction);
            if (s[0].equals("maximumEntries")) {
                try {
                    return Integer.parseInt(s[1]);
                } catch (final NumberFormatException e) {
                    return Integer.MAX_VALUE;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the minimum amount of entries that a VSL list must contain, extracted from the
     * provided restricitons String. If this value isn't specified or invalid, 0 is returned.
     *
     * @param restrictions
     *            The restrictions string to parse.
     * @return The minimum amount of entries a VSL list must have.
     */
    public static int getMinListEntries(final String restrictions) {
        for (final String restriction : getRestrictionArray(restrictions)) {
            final String[] s = parseRestriction(restriction);
            if (s[0].equals("minimumEntries")) {
                try {
                    return Integer.parseInt(s[1]);
                } catch (final NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Splits the restriction string in its single Elements (e.g. "restriction1='a',
     * restriction2='b'" will be returned as ["restriction1='a'", "restriction2='b'"].
     *
     * @param restrictions
     *            The restrictions String that should be split.
     * @return A String array containing the single constrictions.
     */
    private static String[] getRestrictionArray(final String restrictions) {
        final String res = restrictions;
        if (res == null || res.trim().isEmpty()) {
            return new String[0];
        }
        final String[] result = res.split(",(?=([^']*'[^']*')*[^']*$)", -1);
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }

    /**
     * Parses a single restriction and returns its key and value. E.g. "restriction1='a'" will be
     * parsed as ["restriction1","a"].
     *
     * @param restriction
     *            The restriction string that should be parsed.
     * @return A string array with two elements, s[0] is the restriction name, s[1] the restricion
     *         value
     */
    private static String[] parseRestriction(final String restriction) {
        final String[] result = restriction.split("=(?=([^']*'[^']*')*[^']*$)", -1);
        // remove enclosing ''
        if (result.length == 2) {
            result[1] = result[1].substring(1, result[1].length() - 1);
        }
        return result;
    }

    /**
     * Hide default constructor for utility class.
     */
    private Restrictions() {
    }

    /**
     * Returns the given Restrictions in String as list.
     *
     * @param restrictions
     *            The restrictions in list format.
     * @return The restrictions as list.
     */
    public static Map<String, String> splitRestrictions(final String restrictions) {
        final Map<String, String> result = new HashMap<String, String>();
        for (final String restriction : getRestrictionArray(restrictions)) {
            final String[] split = parseRestriction(restriction);
            result.put(split[0], split[1]);
        }
        return result;
    }

}
