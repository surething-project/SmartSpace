package org.ds2os.vsl.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Mapper for a Json object (used by databinder free mapping).
 *
 * @param <T>
 *            the type which is mapped by this mapper.
 * @author felix
 */
public interface JsonTypeMapper<T> {

    /**
     * Writes a value to the writer.
     *
     * @param value
     *            the value to serialize.
     * @param jsonGenerator
     *            the Json generator used to write Json.
     * @throws IOException
     *             If an I/O error occurs.
     */
    void writeValue(T value, JsonGenerator jsonGenerator) throws IOException;

    /**
     * Read a value from the reader.
     *
     * @param jsonParser
     *            the Json parser used to read Json.
     * @return the value.
     * @throws IOException
     *             If an I/O error occurs.
     */
    T readValue(JsonParser jsonParser) throws IOException;
}
