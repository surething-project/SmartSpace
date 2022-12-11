package org.ds2os.benchmark.kor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.core.config.VslKORDatabaseConfig;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.HSQLDatabase;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark class for KOR benchmarks.
 *
 * @author liebald
 */
@Measurement(iterations = 10, time = 2)
@Warmup(iterations = 10)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1)
public class HSQLDBBenchmark {

    /**
     * State class used for holding necessary state informations for all benchmarks.
     *
     * @author liebald
     */
    @State(Scope.Thread)
    public static class MyState {
        /**
         * Initialize the state. Level is Trial, means this is done for every fork.
         */
        @Setup(Level.Trial)
        public final void doSetup() {

            // create initialConfiguration mock
            final VslKORDatabaseConfig configMock = mock(VslKORDatabaseConfig.class);
            when(configMock.getDatabaseMaxValueLength()).thenReturn("16M");
            when(configMock.getDatabaseMemoryMode()).thenReturn("CACHED");
            when(configMock.getDatabasePassword()).thenReturn("thisMustBeSecure");
            when(configMock.getDatabaseUsername()).thenReturn("ds2os");
            when(configMock.getDatabasePath()).thenReturn("hsqldb/db");
            when(configMock.getArchiveNodeVersionLimit()).thenReturn(1);
            when(configMock.isArchiveEnabled()).thenReturn(true);
            when(configMock.isDatabasePersistent()).thenReturn(false);

            final VslStatisticsProvider statisticsProvider = new DummyStatisticsProvider();
            db = new HSQLDatabase(configMock, statisticsProvider);
            db.activate();
            final List<String> types = Arrays.asList("type");
            final List<String> readerIds = Arrays.asList("reader");
            final List<String> writerIds = Arrays.asList("writer");
            final String restriction = "";
            final String cacheParameters = "";

            db.addNode(agent1, types, readerIds, writerIds, restriction, cacheParameters);
            db.addNode(service, types, readerIds, writerIds, restriction, cacheParameters);
            db.addNode(node1, types, readerIds, writerIds, restriction, cacheParameters);
            db.addNode(node2, types, readerIds, writerIds, restriction, cacheParameters);
            db.addNode(node1node1, types, readerIds, writerIds, restriction, cacheParameters);

            try {
                final LinkedHashMap<String, String> nodesToSet = new LinkedHashMap<String, String>();
                nodesToSet.put(agent1, "1");
                nodesToSet.put(node1, "2");
                nodesToSet.put(node2, "3");
                nodesToSet.put(node1node1, "4");
                nodesToSet.put(service, "5");

                db.setValueTree(nodesToSet);

            } catch (final NodeNotExistingException e) {
                e.printStackTrace();
            }

        }

        /**
         *
         */
        private final String agent1 = "/agent1", service = "/agent1/service",
                node1 = "/agent1/service/node1", node2 = "/agent1/service/node2",
                node1node1 = "/agent1/service/node1/node1";

        /**
         * finished.
         */
        @TearDown(Level.Trial)
        public final void doTearDown() {
        }

        /**
         * The database used for the benchmarks.
         */
        private VslNodeDatabase db;
    }

    // /**
    // * Benchmarks gets on retrieving a value using the old version.
    // *
    // * @param state
    // * Contains the necessary state information for the benchmark.
    // * @return The get result (in order to avoid jvm optimizations because the result isn't used)
    // * @throws VslException
    // * shouldn't happen.
    // */
    // @Benchmark
    // public final TreeMap<String, InternalNode> benchmarkSingleGetOld(final MyState state)
    // throws VslException {
    // return state.db.getNodeRecord(state.agent1, true);
    // }

    /**
     * Benchmarks gets on retrieving a value using the new version.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @return The get result (in order to avoid jvm optimizations because the result isn't used)
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final TreeMap<String, InternalNode> benchmarkSingleGetNew(final MyState state)
            throws VslException {
        return state.db.getNodeRecord(state.agent1, new AddressParameters().withDepth(-1)
                .withNodeInformationScope(NodeInformationScope.COMPLETE));
    }
}
