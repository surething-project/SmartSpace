package org.ds2os.vsl.test.serialization;

/**
 * Benchmark the serialization and deserialization of data using the databind protobuf module.
 *
 * @author felix
 */
public class ProtobufSerializationBenchmark extends AbstractSerializationBenchmark {

    /**
     * Constructor that initializes superclass with the protobuf databind mapper.
     */
    public ProtobufSerializationBenchmark() {
        super(TestMappers.getInstance().getProtobufDatabindMapper());
    }
}
