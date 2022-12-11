package org.ds2os.vsl.test.serialization;

import java.io.File;
import java.util.Arrays;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the benchmark execution.
 *
 * @author liebald
 * @author felix
 */
public final class SerializationBenchmarks {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SerializationBenchmarks.class);

    /**
     * Dummy constructor.
     */
    private SerializationBenchmarks() {
    }

    /**
     * Main method for all benchmarks in this project. Using the arguments, the specific tests that
     * should be ran can be specified by their classname (without .java). If no are specified, all
     * tests are run.
     *
     * @param args
     *            The test classes that should be ran.
     */
    public static void main(final String[] args) {
        final File resultFile = new File(
                "target/jmh/" + SerializationBenchmarks.class.getSimpleName() + ".jmh.json");

        LOG.info("Starting benchmarks with arguments: {}", Arrays.toString(args));

        final ChainedOptionsBuilder optionBuilder = new OptionsBuilder()
                .resultFormat(ResultFormatType.JSON).result(resultFile.getPath());

        // the classes that should be benchmarked can be given as command line parameters
        for (final String className : args) {
            optionBuilder.include(className);
        }

        // fine tune the forks, iterations and warm up iterations
        optionBuilder.forks(5).measurementIterations(20).warmupIterations(10);

        try {
            resultFile.getParentFile().mkdirs();
            new Runner(optionBuilder.build()).run();
        } catch (final RunnerException e) {
            LOG.error("Exception during benchmarks:", e);
        }
    }
}
