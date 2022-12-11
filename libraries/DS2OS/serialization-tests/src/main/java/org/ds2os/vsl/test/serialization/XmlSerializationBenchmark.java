package org.ds2os.vsl.test.serialization;

/**
 * Benchmark the serialization and deserialization of data using the databind XML module.
 *
 * @author felix
 */
public class XmlSerializationBenchmark extends AbstractSerializationBenchmark {

    /**
     * Constructor that initializes superclass with the XML databind mapper.
     */
    public XmlSerializationBenchmark() {
        super(TestMappers.getInstance().getXmlDatabindMapper());
    }
}
