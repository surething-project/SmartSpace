package org.ds2os.vsl.kor.config;

import org.ds2os.vsl.core.config.VslInitialConfig;
import org.ds2os.vsl.core.config.VslKORConfig;

/**
 * Serves as intermediate config for the startup and is replaces with the config service after the
 * KOR is running.
 *
 * @author liebald
 */
public class InterMediateConfig implements VslKORConfig {

    /**
     * The inital Config which is used to retrieve the configuration.
     */
    VslInitialConfig initialConfig;

    /**
     * Constructor.
     *
     * @param initialConfig
     *            The inital Config which is used to retrieve the configuration.
     */
    public InterMediateConfig(final VslInitialConfig initialConfig) {
        this.initialConfig = initialConfig;
    }

    @Override
    public final int getArchiveNodeVersionLimit() {
        final int result = initialConfig.getIntProperty("kor.archive.limit", 10);
        if (result <= 0) {
            return 10;
        } else {
            return result;
        }
    }

    @Override
    public final Boolean isDatabasePersistent() {
        return initialConfig.getBooleanProperty("kor.db.persist", false);
    }

    @Override
    public final boolean isArchiveEnabled() {
        return initialConfig.getBooleanProperty("kor.archive", false);
    }

    @Override
    public final String getDatabasePath() {
        return initialConfig.getProperty("kor.db.location", "hsqldb/db");
    }

    @Override
    public final String getDatabaseUsername() {
        return initialConfig.getProperty("kor.db.username", "admin");
    }

    @Override
    public final String getDatabasePassword() {
        return initialConfig.getProperty("kor.db.password", "password");
    }

    @Override
    public final int getLockExpirationTime() {
        return initialConfig.getIntProperty("kor.locking.expirationTime", 30);
    }

    @Override
    public final int getLockExpirationWarningTime() {
        return initialConfig.getIntProperty("kor.locking.expirationWarningTime", 30);
    }

    @Override
    public final String getAgentName() {
        return initialConfig.getProperty("ka.agentName", "agent1");
    }

    @Override
    public final String getDatabaseMaxValueLength() {
        // necessary only before startup, so not stored in the KOR.
        return initialConfig.getProperty("kor.db.maxValueLength", "16M");
    }

    @Override
    public final String getDatabaseMemoryMode() {
        return initialConfig.getProperty("kor.db.memoryMode", "CACHED").toUpperCase();
    }

    @Override
    public String getDatabaseType() {
        return initialConfig.getProperty("kor.db.type", "hsqldb").toLowerCase();
    }

}
