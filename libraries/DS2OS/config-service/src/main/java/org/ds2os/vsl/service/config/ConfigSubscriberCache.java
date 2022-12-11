package org.ds2os.vsl.service.config;

import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class subscribes to all registered config parameters. Their values will be cached in this
 * class and updated only if a notification is retrieved.
 *
 * @author liebald
 */
public class ConfigSubscriberCache implements VslSubscriber {

    /**
     * SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSubscriberCache.class);

    /**
     * This Map caches all config parameters that are registered for the
     * {@link ConfigSubscriberCache} . On notifications they will be updated. Key is the address of
     * the config parameter in the kor ("/localKA/system/config/kor/db/persist", value the current
     * parameter value.
     */
    private final Map<String, String> cachedConfigParameters;

    /**
     * This Map caches all boolean config parameters that are registered for the
     * {@link ConfigSubscriberCache} . On notifications they will be updated. Key is the address of
     * the config parameter in the kor ("/localKA/system/config/kor/db/persist", value the current
     * parameter value.
     */
    private final Map<String, Boolean> cachedBooleanConfigParameters;

    /**
     * This Map caches all integer config parameters that are registered for the
     * {@link ConfigSubscriberCache} . On notifications they will be updated. Key is the address of
     * the config parameter in the kor ("/localKA/system/config/kor/db/persist", value the current
     * parameter value.
     */
    private final Map<String, Integer> cachedIntegerConfigParameters;

    /**
     * The {@link VslConnector} for KOR access.
     */
    private final VslConnector connector;

    /**
     * Constructor.
     *
     * @param connector
     *            the injected {@link VslConnector}.
     */
    public ConfigSubscriberCache(final VslConnector connector) {
        this.connector = connector;
        cachedConfigParameters = new HashMap<String, String>();
        cachedBooleanConfigParameters = new HashMap<String, Boolean>();
        cachedIntegerConfigParameters = new HashMap<String, Integer>();
    }

    /**
     * Add an address to a textual value the subscribed ConfigParameters.
     *
     * @param configAddress
     *            The address to cache and update on notifications.
     */
    public final void subscribeToString(final String configAddress) {
        subscribeTo(configAddress);
        cachedConfigParameters.put(configAddress, "");
        notificationCallback(configAddress);
    }

    /**
     * Add an address to a Boolean value to the subscribed ConfigParameters.
     *
     * @param configAddress
     *            The address to cache and update on notifications.
     */
    public final void subscribeToBoolean(final String configAddress) {
        subscribeTo(configAddress);

        cachedBooleanConfigParameters.put(configAddress, null);
        notificationCallback(configAddress);
    }

    /**
     * Add an address to a Integer value to the subscribed ConfigParameters.
     *
     * @param configAddress
     *            The address to cache and update on notifications.
     */
    public final void subscribeToInteger(final String configAddress) {
        subscribeTo(configAddress);
        cachedIntegerConfigParameters.put(configAddress, null);
        notificationCallback(configAddress);
    }

    /**
     * Subscribes to a specific node in the VSL.
     *
     * @param configAddress
     *            The node to subscribe to
     */
    private void subscribeTo(final String configAddress) {
        try {
            connector.subscribe(configAddress, this);
        } catch (final VslException e) {
            LOGGER.debug("Error on adding subscription to {}: {}", configAddress, e.getMessage());
            return;
        }
    }

    /**
     * Returns the config Parameter for the specified address. (e.g.
     * "/localKA/system/config/kor/db/persist")
     *
     * @param configAddress
     *            The address of the config Parameter in the KOR.
     * @return The desired config Parameter as String.
     */
    public final String getConfigParameter(final String configAddress) {
        return cachedConfigParameters.get(configAddress);
    }

    /**
     * Returns the config Parameter for the specified address. (e.g.
     * "/localKA/system/config/kor/db/persist") as boolean.
     *
     * @param configAddress
     *            The address of the config Parameter in the KOR.
     * @return The desired config Parameter as boolean.
     */
    public final boolean getBooleanConfigParameter(final String configAddress) {
        return cachedBooleanConfigParameters.get(configAddress);
    }

    /**
     * Returns the config Parameter for the specified address. (e.g.
     * "/localKA/system/config/kor/db/persist") as int.
     *
     * @param configAddress
     *            The address of the config Parameter in the KOR.
     * @return The desired config Parameter as int.
     */
    public final int getIntegerConfigParameter(final String configAddress) {
        return cachedIntegerConfigParameters.get(configAddress);
    }

    @Override
    public final void notificationCallback(final String address) {

        try {
            if (cachedConfigParameters.containsKey(address)) {
                String value = null;
                value = connector.get(address).getValue();
                cachedConfigParameters.put(address, value);
            } else if (cachedBooleanConfigParameters.containsKey(address)) {
                Boolean value = null;
                value = parseBoolean(connector.get(address).getValue());
                cachedBooleanConfigParameters.put(address, value);
            } else if (cachedIntegerConfigParameters.containsKey(address)) {
                Integer value = null;
                value = Integer.parseInt(connector.get(address).getValue());
                cachedIntegerConfigParameters.put(address, value);
            }
        } catch (final VslException e) {
            LOGGER.debug("Error on subscriptionHandling: {}", e.getMessage());
            return;
        }
    }

    /**
     * Helper function to decide if a given String is true or false as boolean. 0 and false (+
     * False, faLse,...) are interpreted as false, everything else as true.
     *
     * @param bool
     *            The String to check.
     * @return True or false.
     */
    private Boolean parseBoolean(final String bool) {
        return !(bool != null && (bool.equals("0") || bool.toLowerCase().equals("false")));
    }
}
