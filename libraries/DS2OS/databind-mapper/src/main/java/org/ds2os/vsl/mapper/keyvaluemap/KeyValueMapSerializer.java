package org.ds2os.vsl.mapper.keyvaluemap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for maps that uses explicit key and value fields.
 *
 * @author felix
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 */
public final class KeyValueMapSerializer<K, V> extends StdSerializer<Map<K, V>> {

    /**
     * Jackson deserializer must be serializable.
     */
    private static final long serialVersionUID = 3056048229432630377L;

    /**
     * Constructor of the key value map serializer.
     */
    public KeyValueMapSerializer() {
        super(Map.class, false);
    }

    @Override
    public void serialize(final Map<K, V> value, final JsonGenerator gen,
            final SerializerProvider provider) throws IOException {
        final ObjectMapper mapper = (ObjectMapper) gen.getCodec();
        mapper.writerFor(new TypeReference<Set<Map.Entry<K, V>>>() {
        }).writeValue(gen, value.entrySet());
    }
}
