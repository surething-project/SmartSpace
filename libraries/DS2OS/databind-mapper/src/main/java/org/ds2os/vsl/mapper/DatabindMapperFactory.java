package org.ds2os.vsl.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeData;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * {@link VslMapperFactory} using Jackson databind implementations for each format.
 *
 * @author felix
 */
public class DatabindMapperFactory implements VslMapperFactory {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DatabindMapperFactory.class);

    /**
     * Jackson databind module for the node deserialization.
     */
    private final Module vslNodeDeserializer;

    /**
     * Map of the available mappers by MIME type.
     */
    private final Map<String, Class<? extends VslMapper>> availableMappers;

    /**
     * Map of the instantiated mappers by MIME type.
     */
    private final Map<String, VslMapper> instantiatedMappers;

    /**
     * Constructor with injected {@link VslNodeFactory} which uses the default mappers, which are
     * available on the classpath.
     *
     * @param nodeFactory
     *            the node factory which will be used by all created mappers.
     */
    public DatabindMapperFactory(final VslNodeFactory nodeFactory) {
        this(nodeFactory, getDefaultMappers());
    }

    /**
     * Constructor with injected {@link VslNodeFactory} and the mapper classes.
     *
     * @param nodeFactory
     *            the node factory which will be used by all created mappers.
     * @param mappers
     *            a map of the MIME types to the class of the suitable {@link VslMapper}.
     */
    public DatabindMapperFactory(final VslNodeFactory nodeFactory,
            final Map<String, Class<? extends VslMapper>> mappers) {
        vslNodeDeserializer = createNodeDeserializer(nodeFactory);
        availableMappers = new HashMap<String, Class<? extends VslMapper>>(mappers);
        instantiatedMappers = new HashMap<String, VslMapper>();
    }

    /**
     * Helper to create the VSL node deserializer Jackson module.
     *
     * @param nodeFactory
     *            the {@link VslNodeFactory} to use.
     * @return the initialized Jackson module.
     */
    protected final Module createNodeDeserializer(final VslNodeFactory nodeFactory) {
        final SimpleModule module = new SimpleModule("VslNodeDeserializerModule",
                Version.unknownVersion());
        module.addAbstractTypeMapping(VslNode.class, nodeFactory.getDeserializationType());
        module.addAbstractTypeMapping(VslNodeData.class, nodeFactory.getDeserializationType());
        return module;
    }

    /**
     * Helper to instantiate a mapper if it is not yet instantiated. Requires that the mapper is
     * available in {@link #availableMappers} and that it is not yet instantiated, synchronizing on
     * {@link #instantiatedMappers}.
     *
     * @param mimeType
     *            the MIME type of the mapper in {@link #availableMappers}.
     * @return the new instance.
     */
    protected final VslMapper instantiateMapper(final String mimeType) {
        final Class<? extends VslMapper> clazz = availableMappers.get(mimeType);
        try {
            final Constructor<? extends VslMapper> constructor = clazz.getConstructor(Module.class);
            final VslMapper newInstance = constructor.newInstance(vslNodeDeserializer);
            instantiatedMappers.put(mimeType, newInstance);
            return newInstance;
        } catch (final SecurityException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("SecurityException from JVM during cration of VslMapper for MIME type "
                        + mimeType + " using the class " + clazz.getCanonicalName(), e);
            }
        } catch (final NoSuchMethodException e) {
            LOG.error("Class {} has no constructor with one argument of type Module.", clazz);
        } catch (final IllegalArgumentException e) {
            LOG.error("IllegalArgumentException on invocation of the constructor of the VslMapper.",
                    e);
        } catch (final InstantiationException e) {
            LOG.error("Class {} could not be instantiated, maybe because it is not instantiable.",
                    clazz);
        } catch (final IllegalAccessException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("IllegalAccessException from JVM during instantiation of the class "
                        + clazz.getCanonicalName(), e);
            }
        } catch (final InvocationTargetException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Constructor of " + clazz.getCanonicalName() + " threw an exception.",
                        e.getCause());
            }
        }
        return null;
    }

    @Override
    public final Collection<String> getSupportedMimeTypes() {
        return Collections.unmodifiableCollection(availableMappers.keySet());
    }

    @Override
    public final VslMapper getMapper(final String mimeType) {
        final String normalizedMimeType = VslMimeTypes.getNormalizedMimeType(mimeType);
        if (!availableMappers.containsKey(normalizedMimeType)) {
            return null;
        }
        VslMapper result;
        synchronized (instantiatedMappers) {
            result = instantiatedMappers.get(normalizedMimeType);
            if (result == null) {
                result = instantiateMapper(normalizedMimeType);
            }
        }
        return result;
    }

    /**
     * Helper method to load a class by its name into the map of available mappers by MIME type. If
     * the class is not available on the classpath or the loading fails, this method will not throw
     * anything and nothing is added to the output map (but it will be logged as warning or info).
     *
     * @param mimeType
     *            the MIME type to add this class for.
     * @param className
     *            the class name which is searched on the class path.
     * @param outputMap
     *            the output map where the class is added upon successful loading.
     */
    @SuppressWarnings("unchecked")
    protected static final void loadMapperClass(final String mimeType, final String className,
            final Map<String, Class<? extends VslMapper>> outputMap) {
        try {
            final Class<?> mapper = Class.forName(className);
            if (VslMapper.class.isAssignableFrom(mapper)) {
                outputMap.put(mimeType, (Class<? extends VslMapper>) mapper);
            } else {
                LOG.warn("Mapper class {} is expected to implement VslMapper interface,"
                        + " but does not. This mapper will be ignored.", className);
            }
        } catch (ClassNotFoundException e) {
            LOG.info("Did not find mapper for {} on classpath ({}).", mimeType, className);
        }
    }

    /**
     * Helper method to get the map of default mapper classes by MIME type. Searches the classpath
     * by string for the mappers which are not included in this Maven module, ignoring errors if
     * other mappers are not found. The {@link JsonMapper} from this module will always be
     * available.
     *
     * @return a map of the MIME types to the class of the mapper.
     */
    protected static final Map<String, Class<? extends VslMapper>> getDefaultMappers() {
        final Map<String, Class<? extends VslMapper>> mappers;
        mappers = new HashMap<String, Class<? extends VslMapper>>();

        // always add JSON mapper from this Maven module
        mappers.put(VslMimeTypes.JSON, JsonMapper.class);

        // try to load the other known mappers from their modules, if these modules are available on
        // the classpath. Missing modules will be logged by ignored.
        loadMapperClass(VslMimeTypes.XML, "org.ds2os.vsl.mapper.xml.XmlMapper", mappers);
        loadMapperClass(VslMimeTypes.CBOR, "org.ds2os.vsl.mapper.cbor.CborMapper", mappers);
        loadMapperClass(VslMimeTypes.PROTOBUF, "org.ds2os.vsl.mapper.protobuf.ProtobufMapper",
                mappers);

        return mappers;
    }
}
