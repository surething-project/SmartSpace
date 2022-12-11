package org.ds2os.vsl.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Content mapper with Json format using the Jackson databind API.
 *
 * @author felix
 */
public final class JsonMapper implements VslMapper {

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
    public JsonMapper(final Module nodeDeserializer) {
        mapper = new ObjectMapper();
        mapper.registerModule(nodeDeserializer);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public String getContentType() {
        return VslMimeTypes.JSON;
    }

    @Override
    public String getContentEncoding() {
        return "UTF-8";
    }

    @Override
    public <T> void writeValue(final OutputStream output, final T value) throws IOException {
        final Writer writer = new BufferedWriter(
                new OutputStreamWriter(output, getContentEncoding()));
        try {
            mapper.writeValue(writer, value);
        } finally {
            writer.close();
        }
    }

    @Override
    public <T> T readValue(final InputStream input, final Class<T> valueType) throws IOException {
        final Reader reader = new BufferedReader(
                new InputStreamReader(input, getContentEncoding()));
        try {
            return mapper.readValue(reader, valueType);
        } finally {
            reader.close();
        }
    }
}
