package org.ds2os.vsl.service.config;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.config.VslConfigurationService;
import org.ds2os.vsl.core.config.VslInitialConfig;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of the config service.
 *
 * @author liebald
 */
public final class ConfigService extends AbstractVslModule implements VslConfigurationService {

    /**
     * Service manifest object. At the moment, created dummy instance.
     */
    private static final VslServiceManifest DUMMY_MANIFEST = new VslServiceManifest() {

        @Override
        public String getBinaryHash() {
            return "";
        }

        @Override
        public String getModelHash() {
            return "";
        }

        @Override
        public String getModelId() {
            return "/system/config";
        }
    };

    /**
     * SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

    /**
     * rootAddress of the local config in the system.
     */
    private final String configRoot;

    /**
     * singleton object of the internal config.
     */
    private final VslInitialConfig initialConfig;

    /**
     * The {@link VslConnector} used by the config service.
     */
    private final VslParametrizedConnector kor;

    /**
     * Cache for config Parameters. Uses subscriptions to make sure the values are up to date.
     */
    private ConfigSubscriberCache configCache;

    /**
     * Construct the config service.
     *
     * @param connector
     *            the {@link VslConnector} used by the config service.
     * @param initialConfig
     *            Initial configuration to jump start this service
     */
    public ConfigService(final VslParametrizedConnector connector,
            final VslInitialConfig initialConfig) {
        this.kor = connector;
        this.initialConfig = initialConfig;
        configRoot = "/" + getAgentName() + "/system/config";
    }

    @Override
    public void activate() {
        boolean configWasAlreadyLoaded = true;
        try {
            kor.get(configRoot);
        } catch (final NodeNotExistingException e) {
            configWasAlreadyLoaded = false;
            // if the get throws an exception this means the model isn't
            // registered yet and we need to overwrite config values with data
            // from the initial config after instantiating it.
        } catch (final VslException e) {
            LOGGER.error("unexpected Exception on retrieving the config subtree:", e);
        }

        try {
            // necessary anyways, if the model already is registered this will
            // re-register lists, otherwise instantiate the model.
            final String serviceName = kor.registerService(DUMMY_MANIFEST);
            LOGGER.info("Service Name : {} ", serviceName);
        } catch (final VslException e) {
            LOGGER.error("Error while registering the service:", e.getMessage());
        }
        if (!configWasAlreadyLoaded) {
            initConfig();
        }
        configCache = new ConfigSubscriberCache(kor);
        subscribeToConfigAddresses();
    }

    @Override
    public void shutdown() {
        // nothing to do here.
    }

    /**
     * In this function all nodes that should be cached and subscribed by the
     * {@link ConfigSubscriberCache} are registered.
     */
    private void subscribeToConfigAddresses() {
        configCache.subscribeToBoolean(configRoot + "/kor/archive/enabled");
        configCache.subscribeToInteger(configRoot + "/kor/archive/limit");
        configCache.subscribeToInteger(configRoot + "/korSync/updateCacheTimeout");
        configCache.subscribeToInteger(configRoot + "/kor/locking/lockExpirationTime");
        configCache.subscribeToInteger(configRoot + "/kor/locking/lockExpirationWarningTime");
        configCache.subscribeToInteger(configRoot + "/cache/defaultTTL");
        configCache.subscribeToBoolean(configRoot + "/cache/enabled");
        configCache.subscribeToInteger(configRoot + "/cache/capacity");
        configCache.subscribeToString(configRoot + "/cache/replacementPolicy");
        configCache.subscribeToInteger(configRoot + "/statistics/limitDatapoints");
        configCache.subscribeToString(configRoot + "/modelRepository/localPath");
        configCache.subscribeToString(configRoot + "/modelRepository/cmrUrl");
        configCache.subscribeToString(configRoot + "/ds2os/charset");
        configCache.subscribeToInteger(configRoot + "/alivePing/senderIntervall");
        configCache.subscribeToBoolean(configRoot + "/transport/allowLoopback");
        configCache.subscribeToInteger(configRoot + "/transport/callbackTimeout");
        configCache.subscribeToInteger(configRoot + "/transport/rest/port");
        configCache.subscribeToString(configRoot + "/transport/rest/contentTypePreference");
        configCache.subscribeToBoolean(configRoot + "/security/sphinx/enabled");
        configCache.subscribeToBoolean(configRoot + "/security/sphinx/blockOnAnomaly");

    }

    /**
     * Reads the config subtree from the local kor, sets values that are not yet initialized by the
     * model but given in the initial config and writes the result back.
     */
    private void initConfig() {
        // read the full config tree from the KOR:
        VslNode configBefore;
        final VslMutableNode configAfter = kor.getNodeFactory().createMutableNode((String) null);
        try {
            configBefore = kor.get(configRoot, new AddressParameters().withDepth(-1));
        } catch (final VslException e) {
            LOGGER.error("unexpected Exception on retrieving the config subtree: {}",
                    e.getMessage());
            return;
        }
        // iterate through all nodes and try to read the default value from the
        // config.
        for (final Entry<String, VslNode> node : configBefore.getAllChildren()) {
            // default values of the config file have preference over the
            // values from the model.
            final String prop = node.getKey().replace("/", ".");

            // if we can't find it, set it to null (means it won't be set in
            // the kor)
            configAfter.putChild(node.getKey(),
                    kor.getNodeFactory().createMutableNode(initialConfig.getProperty(prop, null)));

        }
        // finally set the updated config.
        try {
            kor.set(configRoot, configAfter);
        } catch (final VslException e) {
            LOGGER.error("couldn't set the updated config from init file {}", configRoot, e);
        }
    }

    @Override
    public String getAgentName() {
        return initialConfig.getProperty("ka.agentName", "agent1");
    }

    @Override
    public Boolean isDatabasePersistent() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getBooleanProperty("kor.db.persist", false);
    }

    @Override
    public String getDatabaseMaxValueLength() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.maxValueLength", "16M");
    }

    @Override
    public boolean isArchiveEnabled() {
        try {
            // try to get the value from the KOR first.
            return configCache.getBooleanConfigParameter(configRoot + "/kor/archive/enabled");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve Archive parameter: {}, trying the originalConfig now.",
                    e.getMessage());
            // fallback: try the initial Config, if this doesn't work use the
            // default value of false.
            return initialConfig.getBooleanProperty("kor.archive.enabled", false);
        }
    }

    @Override
    public int getArchiveNodeVersionLimit() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/kor/archive/limit");
        } catch (final Exception e) {
            LOGGER.trace("couldn't read archivedNodeVersionLimit parameter: {},"
                    + " trying the originalConfig now.", e.getMessage());
            return initialConfig.getIntProperty("kor.archive.limit", 10);
        }
    }

    @Override
    public String getDatabasePath() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.location", "hsqldb/db");
    }

    @Override
    public String getDatabaseMemoryMode() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.memoryMode", "CACHED").toUpperCase();
    }

    @Override
    public String getDatabaseUsername() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.username", "admin");
    }

    @Override
    public String getDatabasePassword() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.password", "password");
    }

    @Override
    public int getLockExpirationTime() {
        try {
            // try to get the value from the KOR first.
            return configCache
                    .getIntegerConfigParameter(configRoot + "/kor/locking/lockExpirationTime");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve LockExpirationTime: {}, trying the originalConfig now.",
                    e.getMessage());
            // fallback: try the initial Config, if this doesn't work use the
            // default value of 30.
            return initialConfig.getIntProperty("kor.locking.expirationTime", 30);
        }
    }

    @Override
    public int getLockExpirationWarningTime() {
        try {
            return configCache.getIntegerConfigParameter(
                    configRoot + "/kor/locking/lockExpirationWarningTime");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve LockExpirationWarningTimeLeft: {},"
                    + " trying the originalConfig now.", e.getMessage());
            return initialConfig.getIntProperty("kor.locking.expirationWarningTime", 5);
        }
    }

    @Override
    public String getTLSString() {
        try {
            return kor.get(configRoot + "/multicastTransport/tlsString").getValue();
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve tlsString: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getProperty("multicastTransport.tlsString",
                    "TLS_PSK_WITH_AES_256_CBC_SHA384");
        }
    }

    @Override
    public int getMaxSenders() {
        try {
            return Integer
                    .parseInt(kor.get(configRoot + "/multicastTransport/maxSenders").getValue());
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve maxSenders: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("multicastTransport.maxSenders", 100);
        }
    }

    @Override
    public int getMaxAuthorizedBufferSize() {
        try {
            return Integer.parseInt(
                    kor.get(configRoot + "/multicastTransport/maxAuthorizedBufferSize").getValue());
        } catch (final Exception e) {
            LOGGER.trace(
                    "couldn't retrieve maxAuthorizedBufferSize: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("multicastTransport.maxAuthorizedBufferSize",
                    50 * 1000 * 1000);
        }
    }

    @Override
    public int getMaxUnauthorizedBufferSize() {
        try {
            return Integer.parseInt(kor
                    .get(configRoot + "/multicastTransport/maxUnauthorizedBufferSize").getValue());
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve maxUnauthorizedBufferSize: {},"
                    + " trying the originalConfig now.", e.getMessage());
            return initialConfig.getIntProperty("multicastTransport.maxUnauthorizedBufferSize",
                    10 * 1000);
        }
    }

    @Override
    public long getBufferStaleInterval() {
        try {
            return Long.parseLong(
                    kor.get(configRoot + "/multicastTransport/bufferStaleInterval").getValue());
        } catch (final Exception e) {
            LOGGER.trace(
                    "couldn't retrieve bufferStaleInterval: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getLongProperty("multicastTransport.bufferStaleInterval",
                    10 * 1000);
        }
    }

    @Override
    public long getMaxKORUpdateCacheTime() {
        try {
            return configCache
                    .getIntegerConfigParameter(configRoot + "/korSync/updateCacheTimeout");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve korSync updateCacheTimeout: {},"
                    + " trying the originalConfig now.", e.getMessage());
            return initialConfig.getLongProperty("korSync.updateCacheTimeout", 60000);
        }
    }

    @Override
    public long getAgentRegistryCleanerInterval() {
        try {
            return Long.parseLong(
                    kor.get(configRoot + "/korSync/agentRegistryCleanerInterval").getValue());
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve korSync agentRegistryCleanerInterval: "
                    + "{}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getLongProperty("korSync.agentRegistryCleanerInterval", 30000);
        }
    }

    @Override
    public long getAgentRegistryStalenessTime() {
        try {
            return Long.parseLong(
                    kor.get(configRoot + "/korSync/agentRegistryStalenessTime").getValue());
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve korSync agentRegistryStalenessTime: "
                    + "{}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getLongProperty("korSync.agentRegistryStalenessTime", 60000);
        }
    }

    @Override
    public boolean isCacheEnabled() {
        try {
            return configCache.getBooleanConfigParameter(configRoot + "/cache/enabled");
        } catch (final Exception e) {
            LOGGER.trace(
                    "couldn't retrieve cache enabled parameter: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getBooleanProperty("cache.enabled", false);
        }
    }

    @Override
    public int getDefaultTTL() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/cache/defaultTTL");
        } catch (final Exception e) {
            LOGGER.trace(
                    "couldn't read cache defaultTTL parameter: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("cache.defaultTTL", 60);
        }
    }

    @Override
    public int getCacheCapacity() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/cache/capacity");
        } catch (final Exception e) {
            LOGGER.trace(
                    "couldn't read cache capacity parameter: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("cache.capacity", 1000000);
        }
    }

    @Override
    public String getReplacementPolicy() {
        try {
            return configCache.getConfigParameter(configRoot + "/cache/replacementPolicy");
        } catch (final Exception e) {
            LOGGER.trace("couldn't read cache replacementPolicy parameter:"
                    + " {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getProperty("cache.replacementPolicy", "rr");
        }
    }

    @Override
    public int getStatisticsLimitDatapoints() {
        try {
            return configCache
                    .getIntegerConfigParameter(configRoot + "/statistics/limitDatapoints");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read limit for StatisticDatapoints: {},"
                    + " trying the originalConfig now.", e.getMessage());
            return initialConfig.getIntProperty("statistics.limitDatapoints", 500);
        }
    }

    @Override
    public String getLocalModelFolderPath() {
        try {
            return configCache.getConfigParameter(configRoot + "/modelRepository/localPath");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read path of the local model folder from KOR:"
                    + " {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getProperty("modelRepository.localPath", "models");
        }
    }

    @Override
    public String getCMRurl() {
        try {
            return configCache.getConfigParameter(configRoot + "/modelRepository/cmrUrl");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read url of the CMR from KOR: {}"
                    + ", trying the originalConfig now.", e.getMessage());
            return initialConfig.getProperty("modelRepository.cmrUrl", "");
        }
    }

    @Override
    public Boolean isSLMR() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getBooleanProperty("modelRepository.isSLMR", false);
    }

    @Override
    public String getCharset() {
        try {
            return configCache.getConfigParameter(configRoot + "/ds2os/charset");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read charset for Stringoperations:"
                    + " {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getProperty("ds2os.charset", "UTF-8");
        }
    }

    @Override
    public int getAlivePingIntervall() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/alivePing/senderIntervall");
        } catch (final Exception e) {
            LOGGER.trace(
                    "Couldn't read alivePing intervall: {}," + " trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("alivePing.senderIntervall", 2);
        }
    }

    @Override
    public Set<String> getUsableInterfaces() {
        // necessary only before startup, so not stored in the KOR.
        final String interfacesFromConfig = initialConfig.getProperty("transport.interfaces", "*");
        final Set<String> interfaces = new HashSet<String>();
        for (final String iface : interfacesFromConfig.split("[,;]")) {
            interfaces.add(iface.toLowerCase(Locale.ROOT).trim());
        }
        if (interfaces.isEmpty()) {
            interfaces.add("*");
        }
        return interfaces;
    }

    @Override
    public boolean isLoopbackAllowed() {
        try {
            return configCache.getBooleanConfigParameter(configRoot + "/transport/allowLoopback");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read allow loopback: {}, trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getBooleanProperty("transport.allowLoopback", false);
        }
    }

    @Override
    public int getCallbackTimeout() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/transport/callbackTimeout");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read callback timeout: {}," + " trying the originalConfig now.",
                    e.getMessage());
            return initialConfig.getIntProperty("transport.callbackTimeout", 5);
        }
    }

    @Override
    public int getPort() {
        try {
            return configCache.getIntegerConfigParameter(configRoot + "/transport/rest/port");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read contentTypePreference for REST:"
                    + " {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getIntProperty("transport.rest.port", 8080);
        }
    }

    @Override
    public String getContentTypePreference() {
        try {
            return configCache
                    .getConfigParameter(configRoot + "/transport/rest/contentTypePreference");
        } catch (final Exception e) {
            LOGGER.trace("Couldn't read contentTypePreference for REST:"
                    + " {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getProperty("transport.rest.contentTypePreference",
                    "application/cbor,application/json");
        }
    }

    @Override
    public String getDatabaseType() {
        return initialConfig.getProperty("kor.db.type", "hsqldb").toLowerCase();
    }

    @Override
    public boolean isAnomalyDetectionEnabled() {
        try {
            return configCache.getBooleanConfigParameter(configRoot + "/security/sphinx/enabled");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve anomaly detection enabled"
                    + " parameter: {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getBooleanProperty("security.sphinx.enabled", false);
        }
    }

    @Override
    public boolean isBlockOnAnomaly() {
        try {
            return configCache
                    .getBooleanConfigParameter(configRoot + "/security/sphinx/blockOnAnomaly");
        } catch (final Exception e) {
            LOGGER.trace("couldn't retrieve block on anomaly"
                    + " parameter: {}, trying the originalConfig now.", e.getMessage());
            return initialConfig.getBooleanProperty("security.sphinx.blockOnAnomaly", false);
        }
    }
}
