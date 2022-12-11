package org.ds2os.benchmark.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark the serialization and deserialization of data.
 *
 * @author felix
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5)
@Warmup(iterations = 2)
@Threads(4)
@State(Scope.Benchmark)
public class JsonSerializationBenchmark {

    /**
     * The node factory to use internally.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * The mapper to use in the tests.
     */
    private final VslMapper mapper = new DatabindMapperFactory(nodeFactory)
            .getMapper(VslMimeTypes.JSON);

    /**
     * Serialize a reasonably small VSL leaf node.
     *
     * @throws IOException
     *             If an I/O exception occurs.
     */
    @Benchmark
    public final void benchmarkLeafVslNode() throws IOException {
        final VslNode node = nodeFactory.createImmutableLeaf(Arrays.asList("type1", "type2"),
                "reasoably small value for a leaf", new Date(), 2L, "rw",
                Collections.singletonMap("r1", "v1"));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        mapper.writeValue(output, node);
        mapper.readValue(new ByteArrayInputStream(output.toByteArray()), VslNode.class);
    }
}
