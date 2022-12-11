package org.ds2os.vsl.core.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides configuration for the KOR. Allows querying for values stored in the local config file.
 *
 * @author liebald
 */
public final class InitialConfigFromFile implements VslInitialConfig {

    /**
     * Get the logger instance for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(InitialConfigFromFile.class);

    /**
     * The backing plain text config file that holds all values.
     */
    private Properties config;

    /**
     * The name of the config file.
     */
    private String configFileName = "config.txt";

    /**
     * Singleton object of this class.
     */
    private static InitialConfigFromFile initialConfig;

    /**
     * Utility classes won't get instantiated, remove the visible constructor.
     */
    private InitialConfigFromFile() {

    }

    /**
     * Returns the Initial configuration from a file as object.
     *
     * @return InitialConfigFromFile
     */
    public static InitialConfigFromFile getInstance() {
        synchronized (InitialConfigFromFile.class) {
            if (initialConfig == null) {
                initialConfig = new InitialConfigFromFile();
                initialConfig.loadProperties();
            }
        }
        return initialConfig;
    }

    /**
     * Loads the config file as Properties object.
     */
    private void loadProperties() {
        if (config == null) {
            config = new Properties();
            InputStream input = null;
            try {
                input = new FileInputStream(configFileName);
                config.load(input);

            } catch (final IOException ex) {
                logger.error("Error loading the config file: {}", ex.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (final IOException e) {
                        logger.debug("Error closing the inputStream for the config file: {}",
                                e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * changes the file used to load the configuration to the given one.
     *
     * @param newConfigFile
     *            address of the new configfile (e.g. test.txt instead of config.txt)
     */
    public void changeConfigFile(final String newConfigFile) {
        if (newConfigFile != null) {
            final Properties tmp = config;
            config = null;
            configFileName = newConfigFile;
            loadProperties();
            if (config == null) {
                config = tmp;
            }
        }
    }

    /**
     * Changes the value of a property internally (not reflected to KOR or the config file).
     * Changing the config file via {@link InitialConfigFromFile#changeConfigFile(String)} or
     * restarting the KA will reset this change. Usable for testing.
     *
     * @param key
     *            The key of the config's entry.
     * @param value
     *            the new value of the key
     */
    public void setProperty(final String key, final String value) {
        if (config == null) {
            loadProperties();
        }
        if (config != null) {
            config.setProperty(key, value);
        }
    }

    /**
     * Tries to read the value of <code>key</code> from the config file. Returns null if the config
     * file could not be found or if the key does not exist.
     *
     * @param key
     *            The key of the config's entry.
     * @return The value associated with the key or null.
     */
    private String getProperty(final String key) {
        if (config == null) {
            loadProperties();
        }
        return config == null ? null : config.getProperty(key);
    }

    /**
     * Retrieves the value from the configuration that is associated with the specified key.
     *
     * @param key
     *            The name of the configuration key (key = ...).
     * @param defaultValue
     *            The default value returned.
     * @return Returns the value to the given key or defaultValue if not found.
     */
    @Override
    public String getProperty(final String key, final String defaultValue) {
        final String result = getProperty(key);

        return result == null ? defaultValue : result;
    }

    /**
     * Tries to read the value associated with <code>key</code> and parse it as Integer.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The integer associated with the key, or <code>defaultValue</code> if the key cannot
     *         be found or cannot be parsed as Integer.
     */
    @Override
    public int getIntProperty(final String key, final int defaultValue) {
        Integer result;
        try {
            result = Integer.parseInt(getProperty(key));
        } catch (final NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Tries to read the value associated with <code>key</code> and parse it as long.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The long associated with the key, or <code>defaultValue</code> if the key cannot be
     *         found or cannot be parsed as long.
     */
    @Override
    public long getLongProperty(final String key, final long defaultValue) {
        long result;
        try {
            result = Long.parseLong(getProperty(key));
        } catch (final NumberFormatException e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * Tries to read the value associated with <code>key</code> and parse it as Boolean. If the key
     * cannot be found, this will return <code>defaultValue</code>. If there is a value associated
     * with <code>key</code> the result of {@link Boolean#parseBoolean(String)} will be returned.
     *
     * @param key
     *            The key of the config entry to fetch.
     * @param defaultValue
     *            The default value returned.
     * @return The parsed boolean value or default value if the entry cannot be found.
     */
    @Override
    public boolean getBooleanProperty(final String key, final boolean defaultValue) {
        final String value = getProperty(key);
        return value == null ? defaultValue : value.equals("1") || value.equals("true");

    }

}
