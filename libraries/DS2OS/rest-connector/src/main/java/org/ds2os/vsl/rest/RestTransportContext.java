package org.ds2os.vsl.rest;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.config.VslRestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic helper functions of a RESTful transport.
 *
 * @author felix
 */
public class RestTransportContext {

    /**
     * Timeout for idle connections until they are closed (also affects WebSockets, but their alive
     * pings are sent with a rate half of this timeout).
     */
    public static final long IDLE_TIMEOUT = 300000L;

    /**
     * The WebSocket protocol string for the VSL callbacks (version 1).
     */
    public static final String WEBSOCKET_PROTOCOL_V1 = "v1.vsl.ds2os.org";

    /**
     * The maximum WebSocket buffer size (per message). Increased a lot for bigger virtual nodes.
     * <p>
     * FIXME: implement "drop zone" for large requests and reduce it again.
     * </p>
     */
    public static final int WEBSOCKET_MAX_MESSAGE_SIZE = 16 * 1024 * 1024;

    /**
     * The SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RestTransportContext.class);

    /**
     * The configuration for generic REST properties.
     */
    private final VslRestConfig config;

    /**
     * The {@link VslMapperFactory} to get mappers for different MIME types.
     */
    private final VslMapperFactory mapperFactory;

    /**
     * The key store.
     */
    private final KeyStore keystore;

    /**
     * The password of private keys inside the keystore.
     */
    private final String keyPassword;

    /**
     * Cache the calculated preference for content types.
     */
    private String[] contentTypePreference;

    /**
     * Lock for thread-safe operations on the content type preference.
     */
    private final Object contentTypeLock = new Object();

    /**
     * Construct the REST transport helper.
     *
     * @param config
     *            the {@link VslRestConfig} with generic configuration.
     * @param keystore
     *            the key store.
     * @param keyPassword
     *            password of private keys inside the keystore.
     * @param mapperFactory
     *            the {@link VslMapperFactory} for creation of the content mappers.
     */
    public RestTransportContext(final VslRestConfig config, final KeyStore keystore,
            final String keyPassword, final VslMapperFactory mapperFactory) {
        this.config = config;
        this.keystore = keystore;
        this.keyPassword = keyPassword;
        this.mapperFactory = mapperFactory;
    }

    /**
     * Get the {@link VslMapperFactory}.
     *
     * @return the mapper factory.
     */
    public final VslMapperFactory getMapperFactory() {
        return mapperFactory;
    }

    /**
     * Get the {@link KeyStore}.
     *
     * @return the keystore.
     */
    public final KeyStore getKeystore() {
        return keystore;
    }

    /**
     * Get the password of private keys inside the keystore.
     *
     * @return the password.
     */
    public final String getKeyPassword() {
        return keyPassword;
    }

    /**
     * Get the {@link VslRestConfig}.
     *
     * @return the REST configuration.
     */
    public final VslRestConfig getConfig() {
        return config;
    }

    /**
     * Get the content type preference. It is guaranteed that at least one default content type
     * is set, therefore, the returned array is never empty.
     *
     * @return the content type preference as an array starting with the most preferred content
     *         type.
     */
    public final String[] getContentTypePreference() {
        synchronized (contentTypeLock) {
            if (contentTypePreference == null) {
                final List<String> preference = new ArrayList<String>();
                final String[] configuredPreference = config.getContentTypePreference().split(",");
                for (final String configured : configuredPreference) {
                    final String contentType = configured.trim();
                    if (!preference.contains(contentType)
                            && mapperFactory.getSupportedMimeTypes().contains(contentType)) {
                        preference.add(contentType);
                    }
                }
                if (preference.isEmpty()) {
                    LOG.warn("No valid content type given in preference.{}",
                            " Adding all supported content types.");
                    preference.addAll(mapperFactory.getSupportedMimeTypes());
                }
                contentTypePreference = preference.toArray(new String[preference.size()]);
                LOG.debug("Determined content type preference: {}",
                        Arrays.deepToString(contentTypePreference));
            }
            return contentTypePreference;
        }
    }

    /**
     * Get a mapper based on the specified accepted content types. The own preference is used as a
     * fallback, but it does not take precedence over the accept order.
     *
     * @param accept
     *            the accepted content types.
     * @return the mapper to use.
     */
    public final VslMapper getMapper(final String... accept) {
        if (accept != null) {
            for (final String acceptVal : accept) {
                if (acceptVal != null && !"".equals(acceptVal)) {
                    final VslMapper mapper;
                    if ("*".equals(acceptVal) || "*/*".equals(acceptVal)) {
                        mapper = mapperFactory.getMapper(getContentTypePreference()[0]);
                    } else {
                        mapper = mapperFactory.getMapper(acceptVal.trim().toLowerCase(Locale.ROOT));
                    }
                    if (mapper != null) {
                        return mapper;
                    }
                }
            }
            if (accept.length > 0) {
                LOG.error(
                        "None of the accepted content type is available in the mapper factory: {}",
                        Arrays.deepToString(accept));
                return null;
            }
        }
        return mapperFactory.getMapper(getContentTypePreference()[0]);
    }
}
