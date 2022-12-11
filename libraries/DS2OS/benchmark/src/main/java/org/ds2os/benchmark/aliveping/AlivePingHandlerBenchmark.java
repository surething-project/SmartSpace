package org.ds2os.benchmark.aliveping;

import static org.ds2os.vsl.netutils.TestHelper.randomString;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.ds2os.vsl.agentregistry.AgentRegistryService;
import org.ds2os.vsl.aliveping.AlivePingHandler;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslAlivePingHandler;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.config.VslAgentRegistryConfig;
import org.ds2os.vsl.core.config.VslAlivePingConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks the AlivepingHandler by sending static and randomly generated AlivePings from a number
 * of different senders.
 *
 * @author Johannes Straßer
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 5)
@Warmup(iterations = 2)
@Fork(warmups = 1, value = 2)
public class AlivePingHandlerBenchmark {

    /**
     * Inner class for encapsulating stuff.
     *
     */
    @State(value = Scope.Thread)
    public static class MyState {
        /**
         * Number of senders used for this benchmark.
         */
        @Param({ ("1"), ("10"), ("100") })
        private int numberOfSenders;

        /**
         * An array of AlivePings for the tests. Changes per Invocation.
         */
        private VslAlivePing[] alivePings;

        /**
         * Random number generator.
         */
        private final Random random = new Random();

        /**
         * The PingHandler to be benchmarked.
         */
        private VslAlivePingHandler pingHandler;

        /**
         * VslIdentity for the AgentRegistry.
         */
        private static final VslIdentity IDENTITY = new ServiceIdentity(
                "system/agentRegistryService", "system/agentRegistryService");

        /**
         * Dummy manifest for the benchmarks.
         */
        private final VslServiceManifest manifest = new VslServiceManifest() {

            @Override
            public String getModelId() {
                return "/agentRegistryService";
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

        /**
         * Setting up a PingHandler as front end, an AgentRegistry as back end.
         *
         * @throws Exception
         *             shouldn't happen.
         */
        @Setup(value = Level.Iteration)
        public final void setUpRegistry() throws Exception {
            // Create AgentRegistry
            final VslVirtualNodeManager virtualNodeManger = new VirtualNodeManager();
            final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
            final DummyStatisticsProvider dummyStatistics = new DummyStatisticsProvider();
            final VslTypeSearchProvider typeSearchProviderMock = mock(VslTypeSearchProvider.class);
            final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

            final KnowledgeRepository kor = new KnowledgeRepository(virtualNodeManger,
                    subscriptionManager, InitialConfigFromFile.getInstance(), dummyStatistics,
                    typeSearchProviderMock, nodeFactory);
            kor.activate();
            kor.registerService(manifest, IDENTITY);
            final RequestHandlerToConnectorBridge reqBridge = new RequestHandlerToConnectorBridge(
                    kor, IDENTITY, nodeFactory);
            final VslAgentRegistryConfig agentName = new VslAgentRegistryConfig() {

                @Override
                public String getAgentName() {
                    return "KA1";
                }

                @Override
                public long getAgentRegistryStalenessTime() {
                    return 0;
                }

                @Override
                public long getAgentRegistryCleanerInterval() {
                    return 0;
                }
            };
            final AgentRegistryService agentRegistry = new AgentRegistryService(reqBridge,
                    agentName);
            agentRegistry.activate();
            final VslAlivePingConfig alivePingConf = new VslAlivePingConfig() {

                @Override
                public String getAgentName() {
                    return "KA1";
                }

                @Override
                public int getAlivePingIntervall() {
                    return 1;
                }
            };
            // Create PingHandler
            final VslTransportManager transportManager = mock(VslTransportManager.class);
            final VslX509Authenticator certAuth = mock(VslX509Authenticator.class);
            final VslKORSyncHandler korSync = mock(VslKORSyncHandler.class);

            pingHandler = new AlivePingHandler(transportManager, certAuth, kor, agentRegistry,
                    alivePingConf, korSync);
        }

        /**
         * Creates an array of new random AlivePings per Invocation. Fields groupID, and CA public
         * key are still static.
         */
        @Setup(value = Level.Invocation)
        public final void setupAlivePings() {
            alivePings = new VslAlivePing[numberOfSenders];
            for (int i = 0; i < numberOfSenders; i++) {
                // Initialize variables
                final HashSet<VslTransportConnector> conns = new HashSet<VslTransportConnector>();
                final String groupID = "argg";
                final String korHash = String.valueOf(random.nextInt(10000));
                final int numKAs = random.nextInt(numberOfSenders);
                conns.add(new TransportConnector(randomString(20)));
                conns.add(new TransportConnector(randomString(50)));
                conns.add(new TransportConnector(randomString(100)));

                // Create ping
                alivePings[i] = new AlivePing("agent" + i, numKAs, "CA Pub Key", conns, groupID,
                        korHash);
            }
        }

        /**
         * Processes the created alivePings.
         */
        public final void processAlivePings() {
            for (final VslAlivePing alivePing : alivePings) {
                pingHandler.handleAlivePing(alivePing, true);
            }
        }

    }

    /**
     * Processes the AlivePings.
     *
     * @param state
     *            encapsulated test class
     */
    @Benchmark
    public final void processSingleStaticAlivePingBenchmark(final MyState state) {
        state.processAlivePings();
    }

}
