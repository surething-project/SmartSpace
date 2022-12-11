package org.ds2os.benchmark;

import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the benchmarks.
 *
 * @author liebald
 */
@Measurement(iterations = 5)
@Warmup(iterations = 3)
public final class DS2OSBenchmarks {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DS2OSBenchmarks.class);

    /**
     * Dummy constructor.
     */
    private DS2OSBenchmarks() {
    }

    /**
     * Main method for all benchmarks in this project. Using the arguments, the specific tests that
     * should be ran can be specified by their classname (without .java). If no are specified, all
     * tests are run.
     *
     * @param args
     *            The testclasses that should be ran.
     */
    public static void main(final String[] args) {
        final ChainedOptionsBuilder optionBuilder = new OptionsBuilder()
                .resultFormat(ResultFormatType.JSON)
                .result("target/" + DS2OSBenchmarks.class.getSimpleName() + ".jmh.json");

        // register Bouncy Castle security provider for encryption benchmarks
        Security.addProvider(new BouncyCastleProvider());

        // This way the classes that should be benchmarked can be given as command line parameters
        for (final String className : args) {
            optionBuilder.include(className);
        }

        final Options options = optionBuilder.build();
        LOGGER.info("starting benchmarks with arguments: ", Arrays.toString(args));
        try {
            new Runner(options).run();
        } catch (final RunnerException e) {
            LOGGER.error("Exception during benchmarks:", e);
        }
    }
}
