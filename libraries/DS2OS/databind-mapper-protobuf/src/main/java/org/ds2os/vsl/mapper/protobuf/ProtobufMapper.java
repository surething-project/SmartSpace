package org.ds2os.vsl.mapper.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeData;
import org.ds2os.vsl.core.node.VslNodeImpl;
import org.ds2os.vsl.mapper.keyvaluemap.KeyValueMapModule;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;

/**
 * Content mapper with Google protocol buffers format using the Jackson databind API.
 *
 * @author felix
 */
public final class ProtobufMapper implements VslMapper {

    /**
     * The used object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * The {@link ProtobufSchemaCache} of this mapper.
     */
    private final ProtobufSchemaCache schemaCache;

    /**
     * Constructor with injected module for VSL node deserialization.
     *
     * @param nodeDeserializer
     *            the VSL node deserializer module to use.
     */
    public ProtobufMapper(final Module nodeDeserializer) {
        mapper = new ObjectMapper(new ProtobufFactory());
        mapper.registerModule(nodeDeserializer);
        mapper.registerModule(new KeyValueMapModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // add the VslNodeProtobufMixin
        mapper.addMixIn(VslNode.class, VslNodeProtobufMixin.class);
        mapper.addMixIn(VslNodeData.class, VslNodeProtobufMixin.class);
        mapper.addMixIn(VslNodeImpl.class, VslNodeProtobufMixin.class);

        schemaCache = new ProtobufSchemaCache();
    }

    /**
     * Load schema for a supported VSL type.
     *
     * @param valueType
     *            the type.
     * @return the {@link ProtobufSchema}.
     * @throws IOException
     *             If an exception occurs.
     */
    public ProtobufSchema schemaFor(final Class<?> valueType) throws IOException {
        final ProtobufSchema schema;
        if (VslNodeData.class.isAssignableFrom(valueType)) {
            schema = schemaCache.loadSchema("VslNode.proto");
        } else {
            throw new IOException(
                    "No proto schema available for type " + valueType.getSimpleName());
        }
        return schema;
    }

    @Override
    public String getContentType() {
        return VslMimeTypes.PROTOBUF;
    }

    @Override
    public String getContentEncoding() {
        return "";
    }

    @Override
    public <T> void writeValue(final OutputStream output, final T value) throws IOException {
        mapper.writerFor(value.getClass()).with(schemaFor(value.getClass())).writeValue(output,
                value);
    }

    @Override
    public <T> T readValue(final InputStream input, final Class<T> valueType) throws IOException {
        return mapper.readerFor(valueType).with(schemaFor(valueType)).readValue(input);
    }
}
