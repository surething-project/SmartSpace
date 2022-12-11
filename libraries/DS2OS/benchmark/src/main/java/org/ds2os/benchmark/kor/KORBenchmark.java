package org.ds2os.benchmark.kor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.config.VslInitialConfig;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.exception.InvalidModelException;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.ds2os.vsl.modelCache.VslModelCache;
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
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 3)
public class KORBenchmark {

    /**
     * Vsl node factory.
     */
    private static final VslNodeFactory NODE_FACTORY = new VslNodeFactoryImpl();

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
            // create VirtualNodeManager mock
            final VslVirtualNodeManager vManagerMock = mock(VslVirtualNodeManager.class);
            when(vManagerMock.isVirtualNode(any(String.class))).thenReturn(false);
            // create SubscriptionManager mock
            final VslSubscriptionManager sManagerMock = mock(VslSubscriptionManager.class);

            // create initialConfiguration mock
            final VslInitialConfig configMock = mock(VslInitialConfig.class);
            when(configMock.getBooleanProperty("kor.db.persist", false)).thenReturn(false);
            when(configMock.getProperty("kor.db.location", "hsqldb/db")).thenReturn("hsqldb/db");
            when(configMock.getProperty("kor.db.username", "admin")).thenReturn("ds2os");
            when(configMock.getProperty("kor.db.password", "password"))
                    .thenReturn("thisMustBeSecure");
            when(configMock.getBooleanProperty("kor.archive", false)).thenReturn(true);
            when(configMock.getProperty("kor.archive", "0")).thenReturn("1");
            when(configMock.getProperty("ka.agentName", "agent1")).thenReturn("agent1");
            when(configMock.getProperty("kor.db.memoryMode", "CACHED")).thenReturn("CACHED");
            when(configMock.getProperty("kor.db.maxValueLength", "16M")).thenReturn("16M");
            // create initialConfiguration mock memory mode
            final VslInitialConfig configMockInMemory = mock(VslInitialConfig.class);
            when(configMockInMemory.getBooleanProperty("kor.db.persist", false)).thenReturn(false);
            when(configMockInMemory.getProperty("kor.db.location", "hsqldb/db"))
                    .thenReturn("hsqldb/dbInMemory");
            when(configMockInMemory.getProperty("kor.db.username", "admin")).thenReturn("ds2os");
            when(configMockInMemory.getProperty("kor.db.password", "password"))
                    .thenReturn("thisMustBeSecure");
            when(configMockInMemory.getBooleanProperty("kor.archive", false)).thenReturn(true);
            when(configMockInMemory.getProperty("kor.archive", "0")).thenReturn("1");
            when(configMockInMemory.getProperty("ka.agentName", "agent1")).thenReturn("agent1");
            when(configMockInMemory.getProperty("kor.db.memoryMode", "CACHED"))
                    .thenReturn("MEMORY");
            when(configMockInMemory.getProperty("kor.db.maxValueLength", "16M")).thenReturn("16M");

            // define cache mock and the model used for the tests.
            final VslModelCache modelCacheMock = mock(VslModelCache.class);
            final LinkedHashMap<String, InternalNode> tModel = new LinkedHashMap<String, InternalNode>();
            tModel.put("id1", new InternalNode(Arrays.asList("/testmodel"), "1",
                    Arrays.asList("id1"), Arrays.asList("id1"), -1, null, "", ""));
            tModel.put("id1/a", new InternalNode(Arrays.asList("/testmodel"), "2",
                    Arrays.asList("id1"), Arrays.asList("id1"), -1, null, "", ""));
            tModel.put("id1/a/a", new InternalNode(Arrays.asList("/testmodel"), "3",
                    Arrays.asList("id1"), Arrays.asList("id1"), -1, null, "", ""));
            tModel.put("id1/a/b", new InternalNode(Arrays.asList("/testmodel"), "4",
                    Arrays.asList("id1"), Arrays.asList("id1"), -1, null, "", ""));
            tModel.put("id1/b", new InternalNode(Arrays.asList("/testmodel"), "5",
                    Arrays.asList("id1"), Arrays.asList("id1"), -1, null, "", ""));
            try {
                when(modelCacheMock.getCompleteModelNodes("/testmodel", "id1")).thenReturn(tModel);
            } catch (final ModelNotFoundException e1) {
                e1.printStackTrace();
            } catch (InvalidModelException e) {
                e.printStackTrace();
            }
            // define the VslNode used for the singleSet tests.
            singleSetNode = NODE_FACTORY.createImmutableLeaf("42");

            // define the VslNode used for the subtreeSet tests.
            multiSetNode = NODE_FACTORY.createMutableNode("id1");
            multiSetNode.putChild("a", NODE_FACTORY.createMutableNode("id1/a"));
            multiSetNode.putChild("a/a", NODE_FACTORY.createMutableNode("id1/a/a"));
            multiSetNode.putChild("a/b", NODE_FACTORY.createMutableNode("id1/a/b"));
            multiSetNode.putChild("b", NODE_FACTORY.createMutableNode("id1/b"));
            final VslStatisticsProvider statisticsProvider = new DummyStatisticsProvider();
            final VslTypeSearchProvider typeSearchProviderMock = mock(VslTypeSearchProvider.class);
            // initialize the KOR with the created mocks.
            kor = new KnowledgeRepository(vManagerMock, sManagerMock, null, modelCacheMock,
                    configMock, null, statisticsProvider, typeSearchProviderMock, NODE_FACTORY);
            try {
                kor.activate();
                kor.registerService(manifest, identity);

            } catch (final Exception e) {
                e.printStackTrace();
            }

            // initialize the KOR with the created mocks.
            korInMemory = new KnowledgeRepository(vManagerMock, sManagerMock, null, modelCacheMock,
                    configMockInMemory, null, statisticsProvider, typeSearchProviderMock,
                    NODE_FACTORY);
            try {
                korInMemory.activate();
                korInMemory.registerService(manifest, identity);

            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * finished.
         */
        @TearDown(Level.Trial)
        public final void doTearDown() {
        }

        /**
         * The kor used for the benchmarks.
         */
        private KnowledgeRepository kor;

        /**
         * The kor used for the benchmarks, as in Memory.
         */
        private KnowledgeRepository korInMemory;

        /**
         * Single VslNode used for benchmarking set operations.
         */
        private VslNode singleSetNode;

        /**
         * VslNode used for benchmarking set operations for entire subtrees.
         */
        private VslMutableNode multiSetNode;

        /**
         * Dummy identity for the benchmarks.
         */

        private final VslIdentity identity = new ServiceIdentity("id1", "id1");

        /**
         * Dummy manifest for the benchmarks.
         */
        private final VslServiceManifest manifest = new VslServiceManifest() {

            @Override
            public String getModelId() {
                return "/testmodel";
            }

            @Override
            public String getModelHash() {
                return "";
            }

            @Override
            public String getBinaryHash() {
                return "";
            }
        };

    }

    /**
     * Benchmarks gets on a single node.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @return The get result (in order to avoid jvm optimizations because the result isn't used)
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final VslNode benchmarkSingleGet(final MyState state) throws VslException {
        return state.kor.get("/agent1/id1/b", state.identity);
    }

    /**
     * Benchmarks gets on a single node when the database is in inMemory mode.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @return The get result (in order to avoid jvm optimizations because the result isn't used)
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final VslNode benchmarkSingleGetInMemory(final MyState state) throws VslException {
        return state.korInMemory.get("/agent1/id1/b", state.identity);
    }

    /**
     * Benchmarks sets on a single node.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmarkSingleSet(final MyState state) throws VslException {
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
    }

    /**
     * Benchmarks sets on a single node when the database is in inMemory mode.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmarkSingleSetInMemory(final MyState state) throws VslException {
        state.korInMemory.set("/agent1/id1/b", state.singleSetNode, state.identity);
    }

    /**
     * Benchmarks gets on node with a total of 4 children (subtree requests).
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @return The get result (in order to avoid jvm optimizations because the result isn't used)
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final VslNode benchmarkSubtreeGet(final MyState state) throws VslException {
        return state.kor.get("/agent1/id1", state.identity);
    }

    /**
     * Benchmarks gets on node with a total of 4 children (subtree requests) when the database is in
     * inMemory mode.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @return The get result (in order to avoid jvm optimizations because the result isn't used)
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final VslNode benchmarkSubtreeGetInMemory(final MyState state) throws VslException {
        return state.kor.get("/agent1/id1", state.identity);
    }

    /**
     * Benchmarks sets on node with a total of 4 children (subtree requests).
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmarkSubtreeSet(final MyState state) throws VslException {
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
    }

    /**
     * Benchmarks alternating get/set operations on the same node.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmark5AlternatingGetSet(final MyState state) throws VslException {
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
        state.kor.get("/agent1/id1/b", state.identity);
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
        state.kor.get("/agent1/id1/b", state.identity);
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
        state.kor.get("/agent1/id1/b", state.identity);
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
        state.kor.get("/agent1/id1/b", state.identity);
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
        state.kor.get("/agent1/id1/b", state.identity);
    }

    /**
     * Benchmarks alternating get/set operations on the same subtree.
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmark5AlternatingGetSetSubtree(final MyState state) throws VslException {
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
        state.kor.get("/agent1/id1", state.identity);
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
        state.kor.get("/agent1/id1", state.identity);
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
        state.kor.get("/agent1/id1", state.identity);
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
        state.kor.get("/agent1/id1", state.identity);
        state.kor.set("/agent1/id1", state.multiSetNode, state.identity);
        state.kor.get("/agent1/id1", state.identity);
    }

    /**
     * Benchmarks sets on node with a total of 4 children (subtree requests).
     *
     * @param state
     *            Contains the necessary state information for the benchmark.
     * @throws VslException
     *             shouldn't happen.
     */
    @Benchmark
    public final void benchmarkSubtreeSetSingleNodes(final MyState state) throws VslException {
        state.kor.set("/agent1/id1", state.singleSetNode, state.identity);
        state.kor.set("/agent1/id1/a", state.singleSetNode, state.identity);
        state.kor.set("/agent1/id1/a/a", state.singleSetNode, state.identity);
        state.kor.set("/agent1/id1/a/b", state.singleSetNode, state.identity);
        state.kor.set("/agent1/id1/b", state.singleSetNode, state.identity);
    }

}
