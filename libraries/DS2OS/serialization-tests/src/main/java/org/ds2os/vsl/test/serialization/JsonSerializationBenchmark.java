package org.ds2os.vsl.test.serialization;

/**
 * Benchmark the serialization and deserialization of data using the databind JSON module.
 *
 * @author felix
 */
public class JsonSerializationBenchmark extends AbstractSerializationBenchmark {

    /**
     * Constructor that initializes superclass with the JSON databind mapper.
     */
    public JsonSerializationBenchmark() {
        super(TestMappers.getInstance().getJsonDatabindMapper());
    }
}
