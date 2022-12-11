package org.ds2os.vsl.test.serialization;

/**
 * Benchmark the serialization and deserialization of data using the databind CBOR module.
 *
 * @author felix
 */
public class CborSerializationBenchmark extends AbstractSerializationBenchmark {

    /**
     * Constructor that initializes superclass with the CBOR databind mapper.
     */
    public CborSerializationBenchmark() {
        super(TestMappers.getInstance().getCborDatabindMapper());
    }
}
