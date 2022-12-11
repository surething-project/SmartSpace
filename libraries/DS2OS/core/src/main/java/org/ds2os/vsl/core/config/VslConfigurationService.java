package org.ds2os.vsl.core.config;

/**
 * This interface bundles all sub-interfaces which are needed by the different Modules. Each module
 * which need to access configuration data creates an interface with all necessary methods and adds
 * it to this one. Implementations of this interface must then implement all methods needed.
 *
 * The necessary interface is then injected in the according module via constructor, allowing the
 * module to access to the necessary config data.
 *
 * An Implementation of this could check several sources for the required configuration data, e.g.
 * the KOR and an initial config file if the KOR doesn't provide the data.
 *
 * @author liebald
 */
public interface VslConfigurationService extends VslAgentName, VslKORConfig,
        VslMulticastTransportConfig, VslKORSyncConfig, VslCacheConfig, VslStatisticsConfig,
        VslModelRepositoryConfig, VslAgentRegistryConfig, VslCharset, VslAlivePingConfig,
        VslTransportConfig, VslRestConfig, VslAnomalyDetectionConfig {
}
