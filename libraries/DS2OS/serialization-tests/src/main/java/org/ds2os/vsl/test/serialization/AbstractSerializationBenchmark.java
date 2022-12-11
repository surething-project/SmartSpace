package org.ds2os.vsl.test.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.test.TestNodes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * Abstract benchmark of the serialization of VSL nodes.
 *
 * @author felix
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public abstract class AbstractSerializationBenchmark {

    /**
     * Ten megabytes, more than any serialized structure's size to avoid performance impact of byte
     * array resizing in {@link ByteArrayOutputStream}.
     */
    private static final int TEN_MB = 10 * 1024 * 1024;

    /**
     * The {@link VslMapper} to benchmark.
     */
    private final VslMapper mapper;

    /**
     * The output stream (byte array based).
     */
    private final ByteArrayOutputStream output;

    /**
     * Constructor with mapper implementation from superclass.
     *
     * @param mapper
     *            the {@link VslMapper}.
     */
    protected AbstractSerializationBenchmark(final VslMapper mapper) {
        this.mapper = mapper;

        output = new ByteArrayOutputStream(TEN_MB);
    }

    /**
     * Serialize the simple data node.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int simpleDataNode() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.SIMPLE_DATA_NODE);
        return output.size();
    }

    /**
     * Serialize the big data node.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int bigDataNode() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.BIG_DATA_NODE);
        return output.size();
    }

    /**
     * Serialize the simple node.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int simpleNode() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.SIMPLE_NODE);
        return output.size();
    }

    /**
     * Serialize the metadata node.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int metadataNode() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.METADATA_NODE);
        return output.size();
    }

    /**
     * Serialize the big node.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int bigNode() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.BIG_NODE);
        return output.size();
    }

    /**
     * Serialize the simple data structure.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int simpleDataStructure() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.SIMPLE_DATA_STRUCTURE);
        return output.size();
    }

    /**
     * Serialize the big data structure.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int bigDataStructure() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.BIG_DATA_STRUCTURE);
        return output.size();
    }

    /**
     * Serialize the simple structure.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int simpleStructure() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.SIMPLE_STRUCTURE);
        return output.size();
    }

    /**
     * Serialize the metadata structure.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int metadataStructure() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.METADATA_STRUCTURE);
        return output.size();
    }

    /**
     * Serialize the big structure.
     *
     * @return the size of the written data.
     * @throws IOException
     *             Should not happen.
     */
    @Benchmark
    public final int bigStructure() throws IOException {
        output.reset();
        mapper.writeValue(output, TestNodes.BIG_STRUCTURE);
        return output.size();
    }
}
