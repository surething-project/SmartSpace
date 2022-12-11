package org.ds2os.vsl.core.utils;

import java.util.Collections;
import java.util.Set;

import org.ds2os.vsl.core.config.VslRestConfig;

/**
 * Static configuration instances for usages without the config service.
 *
 * @author felix
 */
public final class StaticConfig {

    /**
     * Default {@link VslRestConfig}, for example for the service connectors.
     */
    public static final VslRestConfig DEFAULT_REST_CONFIG = new VslRestConfig() {
        @Override
        public Set<String> getUsableInterfaces() {
            return Collections.singleton("*");
        }

        @Override
        public boolean isLoopbackAllowed() {
            return false;
        }

        @Override
        public int getCallbackTimeout() {
            return 60;
        }

        @Override
        public int getPort() {
            return 8080;
        }

        @Override
        public String getContentTypePreference() {
            return "application/cbor,application/json";
        }
    };

    /**
     * Utility class - no instantiation.
     */
    private StaticConfig() {
    }
}
