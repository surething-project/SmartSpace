package org.ds2os.vsl.mapper.cbor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

/**
 * Content mapper with CBOR format using the Jackson databind API.
 *
 * @author felix
 */
public final class CborMapper implements VslMapper {

    /**
     * The used object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Constructor with injected module for VSL node deserialization.
     *
     * @param nodeDeserializer
     *            the VSL node deserializer module to use.
     */
    public CborMapper(final Module nodeDeserializer) {
        mapper = new ObjectMapper(new CBORFactory());
        mapper.registerModule(nodeDeserializer);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getContentType() {
        return VslMimeTypes.CBOR;
    }

    @Override
    public String getContentEncoding() {
        return "";
    }

    @Override
    public <T> void writeValue(final OutputStream output, final T value) throws IOException {
        mapper.writeValue(output, value);
    }

    @Override
    public <T> T readValue(final InputStream input, final Class<T> valueType) throws IOException {
        return mapper.readValue(input, valueType);
    }
}
