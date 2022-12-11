package org.ds2os.vsl.core.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes the {@link ConfigDescription} annotations.
 *
 * @author liebald
 */
public final class DescriptionProcessor {

    /**
     * The logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionProcessor.class);

    /**
     * Hidden constructor for utility class.
     */
    private DescriptionProcessor() {
    }

    /**
     * Method to generate a printable text Documentation out of the {@link ConfigDescription}
     * annotations from the given object. Each Description starts in a new line.
     *
     * @param cl
     *            The class to parse for {@link ConfigDescription} annotations.
     * @return The Descriptions of the given object as printable String.
     */
    public static String getDocumentation(final Class<?> cl) {
        final StringBuilder res = new StringBuilder();
        res.append("Available configuration options "
                + "for the configuration of an Knowledge Agent:\n");
        final Map<String, String> annotations = new TreeMap<String, String>();
        parseInterfaces(annotations, cl);

        // Since we used a Treemap, the entries are ordered by their keys.
        for (final Entry<String, String> desc : annotations.entrySet()) {
            res.append(desc.getValue());
        }

        return res.toString();
    }

    /**
     * Helper function to recursively parse all Interfaces that are implemented by the currently
     * checked Interface and add their annotations to the resulting map. This is necessary since
     * just implementing an interface doesn't carry over the annotations of this interface.
     *
     * @param annotations
     *            Map of already parsed interfaces to which the currently parsed Interfaces
     *            annotations should be added.
     * @param cl
     *            The class to parse for {@link ConfigDescription} annotations.
     */
    private static void parseInterfaces(final Map<String, String> annotations, final Class<?> cl) {
        try {
            for (final Method m : cl.getDeclaredMethods()) {
                try {
                    for (final Annotation a : m.getAnnotations()) {
                        if (a.annotationType() == ConfigDescription.class) {
                            final ConfigDescription desc = (ConfigDescription) a;
                            final String tmp = String.format(
                                    "%n%s%n  description: %s%n  default: %s%n  restriction: %s%n",
                                    desc.id(), desc.description(), desc.defaultValue(),
                                    desc.restrictions());
                            annotations.put(desc.id(), tmp);
                        }
                    }

                } catch (final Exception e) {
                    LOGGER.error("Exception on creating the Documentation for a single method:", e);
                }
            }
        } catch (final Exception e) {
            LOGGER.error("Exception on creating the Documentation:", e);
        }
        // recursively parse all implemented interfaces of the currently parsed interface.
        for (final Class<?> cl2 : cl.getInterfaces()) {
            parseInterfaces(annotations, cl2);
        }

    }

}
