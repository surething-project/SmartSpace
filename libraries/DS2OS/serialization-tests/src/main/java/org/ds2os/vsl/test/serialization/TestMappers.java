package org.ds2os.vsl.test.serialization;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.mapper.DatabindMapperFactory;

/**
 * Global static singleton instances of the used mappers - to avoid multi-instantiation of the
 * mappers during the tests.
 *
 * @author felix
 */
public final class TestMappers {

    /**
     * The singleton instance.
     */
    private static final TestMappers INST = new TestMappers();

    /**
     * {@link VslMapper} instance for JSON mapping using Jackson databind.
     */
    private final VslMapper jsonDatabindMapper;

    /**
     * {@link VslMapper} instance for XML mapping using Jackson databind.
     */
    private final VslMapper xmlDatabindMapper;

    /**
     * {@link VslMapper} instance for CBOR mapping using Jackson databind.
     */
    private final VslMapper cborDatabindMapper;

    /**
     * {@link VslMapper} instance for protobuf mapping using Jackson databind.
     */
    private final VslMapper protobufDatabindMapper;

    /**
     * Singleton.
     */
    private TestMappers() {
        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final VslMapperFactory databindFactory = new DatabindMapperFactory(nodeFactory);
        jsonDatabindMapper = databindFactory.getMapper(VslMimeTypes.JSON);
        xmlDatabindMapper = databindFactory.getMapper(VslMimeTypes.XML);
        cborDatabindMapper = databindFactory.getMapper(VslMimeTypes.CBOR);
        protobufDatabindMapper = databindFactory.getMapper(VslMimeTypes.PROTOBUF);
    }

    /**
     * Get the {@link VslMapper} instance for JSON mapping using Jackson databind.
     *
     * @return the mapper instance.
     */
    public VslMapper getJsonDatabindMapper() {
        return jsonDatabindMapper;
    }

    /**
     * Get the {@link VslMapper} instance for XML mapping using Jackson databind.
     *
     * @return the mapper instance.
     */
    public VslMapper getXmlDatabindMapper() {
        return xmlDatabindMapper;
    }

    /**
     * Get the {@link VslMapper} instance for CBOR mapping using Jackson databind.
     *
     * @return the mapper instance.
     */
    public VslMapper getCborDatabindMapper() {
        return cborDatabindMapper;
    }

    /**
     * Get the {@link VslMapper} instance for protobuf mapping using Jackson databind.
     *
     * @return the mapper instance.
     */
    public VslMapper getProtobufDatabindMapper() {
        return protobufDatabindMapper;
    }

    /**
     * Get the singleton instance.
     *
     * @return {@link TestMappers} singleton.
     */
    public static TestMappers getInstance() {
        return INST;
    }
}
