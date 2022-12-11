package org.ds2os.vsl.core.config;

/**
 * Access the initial configuration for the local KA (e.g. via a file)
 *
 * @author liebald
 */
public interface VslInitialConfig {

    /**
     * Retrieves the value from the configuration that is associated with the specified key. If the
     * key doesn't exist, the default value is returned.
     *
     * @param key
     *            The name of the configuration key (key = ...).
     * @param defaultValue
     *            The default value returned.
     * @return Returns the value to the given key or defaultValue if not found.
     */
    String getProperty(String key, String defaultValue);

    /**
     * Retrieves the value from the configuration that is associated with the specified key and
     * parse it as Integer. If this fails or the key isn't found, the defaultValue is returned.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The integer associated with the key, or defaultValue if the key cannot be found or
     *         cannot be parsed as Integer.
     */
    int getIntProperty(String key, int defaultValue);

    /**
     * Retrieves the value from the configuration that is associated with the specified key and
     * parse it as lang. If this fails or the key isn't found, the defaultValue is returned.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The long associated with the key, or defaultValue if the key cannot be found or
     *         cannot be parsed as long.
     */
    long getLongProperty(String key, long defaultValue);

    /**
     * Retrieves the value from the configuration that is associated with the specified key and
     * parse it as Boolean. If this fails or the key isn't found, the defaultValue is returned.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The parsed boolean value or default value if the entry cannot be found.
     */
    boolean getBooleanProperty(String key, boolean defaultValue);
}
