package org.ds2os.vsl.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ds2os.vsl.core.VslMapper;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Map Json objects without using the Jackson databinder (very embedded applications).
 *
 * @author felix
 */
public class SimpleJsonMapper implements VslMapper {

    /**
     * Factory used to create Json generators and parsers.
     */
    private final JsonFactory factory;

    /**
     * Map of the registered {@link JsonTypeMapper} instances.
     */
    private final ConcurrentMap<Class<?>, JsonTypeMapper<?>> mapperMap;

    /**
     * Default constructor.
     */
    public SimpleJsonMapper() {
        mapperMap = new ConcurrentHashMap<Class<?>, JsonTypeMapper<?>>();
        factory = new JsonFactory();
        factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        factory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    /**
     * Add a new {@link JsonTypeMapper} to this mapper, so the type is supported for Json mapping.
     *
     * @param <T>
     *            the type.
     * @param type
     *            the type to add.
     * @param mapper
     *            the JsonMapper instance.
     */
    public final <T> void addMapper(final Class<T> type, final JsonTypeMapper<T> mapper) {
        mapperMap.put(type, mapper);
    }

    /**
     * Get the mapper which is used for the specified type. Logic is quite shaky and might fail...
     *
     * @param <T>
     *            the type.
     * @param clazz
     *            the class to inspect.
     * @return the JsonTypeMapper.
     * @throws IOException
     *             If no suitable JsonTypeMapper can be found.
     */
    @SuppressWarnings("unchecked")
    public final <T> JsonTypeMapper<T> getMapperFromClass(final Class<T> clazz) throws IOException {
        JsonTypeMapper<?> mapper = mapperMap.get(clazz);
        if (mapper == null) {
            for (final Class<?> iface : clazz.getInterfaces()) {
                if (mapperMap.containsKey(iface)) {
                    mapper = mapperMap.get(iface);
                    break;
                }
            }
            // FIXME: to dangerous...
            // if (mapper == null) {
            // final Class<?> superclass = type.getSuperclass();
            // if (superclass != null) {
            // mapper = getMapper(superclass);
            // }
            // }
        }
        if (mapper == null) {
            throw new IOException("No Json mapper registered for type " + clazz.getName());
        }
        return (JsonTypeMapper<T>) mapper;
    }

    /**
     * Get the mapper which is used for the instance's type. Logic is quite shaky and might fail...
     *
     * @param <T>
     *            the type.
     * @param value
     *            the value instance to inspect.
     * @return the JsonTypeMapper.
     * @throws IOException
     *             If no suitable JsonTypeMapper can be found.
     */
    @SuppressWarnings("unchecked")
    public final <T> JsonTypeMapper<T> getMapperFromInstance(final T value) throws IOException {
        return getMapperFromClass((Class<T>) value.getClass());
    }

    @Override
    public final String getContentType() {
        return "application/json";
    }

    @Override
    public final String getContentEncoding() {
        return "UTF-8";
    }

    @Override
    public final <T> void writeValue(final OutputStream output, final T value) throws IOException {
        final JsonTypeMapper<T> mapper = getMapperFromInstance(value);
        final JsonGenerator jsonGenerator = factory.createGenerator(output, JsonEncoding.UTF8);
        try {
            mapper.writeValue(value, jsonGenerator);
        } finally {
            jsonGenerator.close();
        }
    }

    /**
     * Writes a value to an {@link Writer}. Usually the writer is closed by this operation.
     *
     * @param <T>
     *            the type of the value.
     * @param writer
     *            the writer.
     * @param value
     *            the value to serialize.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public final <T> void writeValue(final Writer writer, final T value) throws IOException {
        final JsonTypeMapper<T> mapper = getMapperFromInstance(value);
        final JsonGenerator jsonGenerator = factory.createGenerator(writer);
        try {
            mapper.writeValue(value, jsonGenerator);
        } finally {
            jsonGenerator.close();
        }
    }

    @Override
    public final <T> T readValue(final InputStream input, final Class<T> valueType)
            throws IOException {
        final JsonTypeMapper<T> mapper = getMapperFromClass(valueType);
        final JsonParser jsonParser = factory.createParser(input);
        try {
            return mapper.readValue(jsonParser);
        } finally {
            jsonParser.close();
        }
    }

    /**
     * Read a value from a {@link Reader}. Usually the reader is closed by this operation.
     *
     * @param <T>
     *            the type of the value.
     * @param reader
     *            the reader to read input from.
     * @param valueType
     *            the class to instantiate for the value.
     * @return the value.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public final <T> T readValue(final Reader reader, final Class<T> valueType) throws IOException {
        final JsonTypeMapper<T> mapper = getMapperFromClass(valueType);
        final JsonParser jsonParser = factory.createParser(reader);
        try {
            return mapper.readValue(jsonParser);
        } finally {
            jsonParser.close();
        }
    }
}
