package org.ds2os.vsl.mapper.keyvaluemap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializer for maps that uses explicit key and value fields.
 *
 * @author felix
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 */
public final class KeyValueMapDeserializer<K, V> extends StdDeserializer<Map<K, V>> {

    /**
     * Jackson deserializer must be serializable.
     */
    private static final long serialVersionUID = 3505956913915339781L;

    /**
     * Constructor of the map deserializer.
     */
    public KeyValueMapDeserializer() {
        super(Map.class);
    }

    @Override
    public Map<K, V> deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
        final ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        final List<KeyValueMapEntry<K, V>> entries = mapper.readValue(jp,
                new TypeReference<List<KeyValueMapEntry<K, V>>>() {
                });
        final Map<K, V> result = new LinkedHashMap<K, V>(entries.size());
        for (final KeyValueMapEntry<K, V> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
