package org.ds2os.vsl.json;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.json.mapper.JsonAlivePingMapper;
import org.ds2os.vsl.json.mapper.JsonHandshakeMapper;
import org.ds2os.vsl.json.mapper.JsonKAInfoMapper;
import org.ds2os.vsl.json.mapper.JsonPostOperationMapper;
import org.ds2os.vsl.json.mapper.JsonServiceManifestMapper;
import org.ds2os.vsl.json.mapper.JsonTransportConnectorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link VslMapperFactory} using simple implementations for each format, if available.
 *
 * @author felix
 */
public class SimpleMapperFactory implements VslMapperFactory {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SimpleMapperFactory.class);

    /**
     * The JSON mapper (still incomplete).
     */
    private final VslMapper jsonMapper;

    /**
     * Default constructor. FIXME: Simple Json is NOT READY FOR USE YET.
     */
    public SimpleMapperFactory() {
        jsonMapper = createJsonMapper();
    }

    /**
     * Create the JSON mapper using {@link SimpleJsonMapper}.
     *
     * @return the initialized mapper.
     */
    protected final VslMapper createJsonMapper() {
        final SimpleJsonMapper result = new SimpleJsonMapper();
        try {
            result.addMapper(VslTransportConnector.class, new JsonTransportConnectorMapper());
            result.addMapper(VslAlivePing.class, new JsonAlivePingMapper(
                    result.getMapperFromClass(VslTransportConnector.class)));
            result.addMapper(VslKAInfo.class,
                    new JsonKAInfoMapper(result.getMapperFromClass(VslTransportConnector.class)));
            result.addMapper(VslHandshakeData.class,
                    new JsonHandshakeMapper(result.getMapperFromClass(VslKAInfo.class)));
            result.addMapper(PostOperation.class, new JsonPostOperationMapper());
            result.addMapper(VslServiceManifest.class, new JsonServiceManifestMapper());
            // TODO: always continue this list!
        } catch (final IOException e) {
            LOG.error("Cannot initialize SimpleJsonMapper.", e);
        }
        return result;
    }

    @Override
    public final Collection<String> getSupportedMimeTypes() {
        return Collections.singleton(VslMimeTypes.JSON);
    }

    @Override
    public final VslMapper getMapper(final String mimeType) {
        final String normalizedMimeType = VslMimeTypes.getNormalizedMimeType(mimeType);
        if (VslMimeTypes.JSON.equals(normalizedMimeType)) {
            return jsonMapper;
        } else {
            return null;
        }
    }
}
