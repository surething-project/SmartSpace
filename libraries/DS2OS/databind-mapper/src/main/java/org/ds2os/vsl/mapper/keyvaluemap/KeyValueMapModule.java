package org.ds2os.vsl.mapper.keyvaluemap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that serializes maps with explicit key and value fields.
 *
 * @author felix
 */
public class KeyValueMapModule extends SimpleModule {

    /**
     * Jackson modules must be serializable.
     */
    private static final long serialVersionUID = -3575574282440794901L;

    /**
     * Constructor with proper module initialization.
     */
    public KeyValueMapModule() {
        super("KeyValueMapModule", Version.unknownVersion(),
                Collections.<Class<?>, JsonDeserializer<?>>singletonMap(Map.class,
                        new KeyValueMapDeserializer<Object, Object>()),
                Arrays.<JsonSerializer<?>>asList(new KeyValueMapSerializer<Object, Object>(),
                        new KeyValueMapEntrySerializer<Object, Object>()));
    }
}
