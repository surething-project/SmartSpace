package org.ds2os.vsl;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.agentregistry.AgentRegistryService;
import org.ds2os.vsl.aliveping.AlivePingHandler;
import org.ds2os.vsl.cache.Cache;
import org.ds2os.vsl.cert.CertificateAuthority;
import org.ds2os.vsl.core.AbstractRequestRouter;
import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslSubscriptionManager;
import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.bridge.RequestHandlerToConnectorBridge;
import org.ds2os.vsl.core.config.DescriptionProcessor;
import org.ds2os.vsl.core.config.InitialConfigFromFile;
import org.ds2os.vsl.core.config.VslConfigurationService;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.statistics.DummyStatisticsProvider;
import org.ds2os.vsl.core.statistics.VslStatisticsProvider;
import org.ds2os.vsl.ka.CachingRequestRouterDecorator;
import org.ds2os.vsl.ka.RequestRouter;
import org.ds2os.vsl.ka.SubscriptionManager;
import org.ds2os.vsl.ka.TransportManager;
import org.ds2os.vsl.ka.VirtualNodeManager;
import org.ds2os.vsl.kor.KnowledgeRepository;
import org.ds2os.vsl.korsync.KORSyncHandler;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.multicasttransport.MulticastTransport;
import org.ds2os.vsl.rest.JettyRestTransport;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.searchProvider.SearchProviderService;
import org.ds2os.vsl.service.config.ConfigService;
import org.ds2os.vsl.slmr.SlmrService;
import org.ds2os.vsl.statistics.StatisticsProvider;
import org.ds2os.vsl.statistics.service.StatisticService;
import org.ds2os.vsl.test.BenchmarkRequestRouterDecorator;
import org.ds2os.vsl.typeSearch.TypeSearchProvider;
import org.ds2os.vsl.typeSearch.service.TypeSearchService;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the knowledge agent bundle for Java 7.
 *
 * @author felix
 */
public class Java7KnowledgeAgent extends AbstractVslModule implements Runnable {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Java7KnowledgeAgent.class);

    /**
     * The agent number.
     */
    private final int agentNum;

    /**
     * The agent id as string.
     */
    private final String agentId;

    /**
     * Path to the JKS keystore of the knowledge agent.
     */
    private final String keystorePath;

    /**
     * Password of the given keystore.
     */
    private final String keystorePassword;

    /**
     * List of all activated modules for deactivation on shutdown.
     */
    private final List<AbstractVslModule> activatedModules;

    /**
     * Construct a Java7KnowledgeAgent instance with the given agent number.
     *
     * @param agentNum
     *            the agent number parsed from the command line.
     */
    public Java7KnowledgeAgent(final int agentNum) {
        this.agentNum = agentNum;
        agentId = "agent" + agentNum;

        // HACK
        InitialConfigFromFile.getInstance().setProperty("ka.agentName", agentId);
        InitialConfigFromFile.getInstance().setProperty("kor.db.location", "hsqldb/" + agentId);
        InitialConfigFromFile.getInstance().setProperty("transport.rest.port",
                Integer.toString(8080 + agentNum));

        // Settings for SSL - TODO dummy
        keystorePath = agentId + ".jks";
        keystorePassword = "K3yst0r3";

        activatedModules = new ArrayList<>();
    }

    @Override
    public final void run() {
        try {
            activate();

            // TEMP: wait until an input is received on STDIN
            try {
                System.in.read();
            } catch (final IOException e) {
                LOGGER.debug("IOException on reading input, not relevant: ", e.getMessage());
                // don't care!
            }

            shutdown();
            System.exit(0);
        } catch (final IOException e) {
            LOGGER.error("KeyStore could not get input stream.", e);
        } catch (final GeneralSecurityException e) {
            LOGGER.error("Exception during keystore loading:", e);
        } catch (final Exception e) {
            LOGGER.error("Exception during Jetty startup:", e);
        }
        System.exit(1);
    }

    // FIXME: quite a monster method... needs more modularization
    @Override
    public final void activate() throws Exception {
        // IPv6 multicast group - TODO dummy
        final Inet6Address ipv6MulticastGroup;
        try {
            ipv6MulticastGroup = (Inet6Address) InetAddress.getByName("ff05::1234:1");
        } catch (final IOException e) {
            LOGGER.error("Exception during InetAddress.getByName:", e);
            System.exit(1);
            return;
        }

        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        VslStatisticsProvider statisticsProvider;
        final boolean useDummyStatistics = InitialConfigFromFile.getInstance()
                .getBooleanProperty("statistics.useDummy", false);
        if (!useDummyStatistics) {
            statisticsProvider = new StatisticsProvider();
        } else {
            statisticsProvider = new DummyStatisticsProvider();

        }
        // use this or the dummy statisticsprovider
        final VslTypeSearchProvider typeSearchProvider = new TypeSearchProvider();
        final CertificateAuthority certAuth = new CertificateAuthority(keystorePath,
                keystorePassword);
        certAuth.selectCertificate(agentId);
        final TransportManager transportManager = new TransportManager();
        final VslVirtualNodeManager virtualNodeManger = new VirtualNodeManager();
        final VslSubscriptionManager subscriptionManager = new SubscriptionManager();
        final KnowledgeRepository kor = new KnowledgeRepository(virtualNodeManger,
                subscriptionManager, InitialConfigFromFile.getInstance(), statisticsProvider,
                typeSearchProvider, nodeFactory);

        final VslParametrizedConnector configServiceConnector = new RequestHandlerToConnectorBridge(
                kor, new ServiceIdentity("system/config", "system/config"), nodeFactory);
        final VslConfigurationService configService = new ConfigService(configServiceConnector,
                InitialConfigFromFile.getInstance());

        final VslParametrizedConnector agentRegistryConnector = new RequestHandlerToConnectorBridge(
                kor,
                new ServiceIdentity("system/agentRegistryService", "system/agentRegistryService"),
                nodeFactory);
        final AgentRegistryService agentRegistryService = new AgentRegistryService(
                agentRegistryConnector, configService);

        final VslRequestHandler requestHandler = new RequestRouter(configService, kor,
                transportManager, agentRegistryService, subscriptionManager, configService);

        final VslConnector cacheConnector = new RequestHandlerToConnectorBridge(requestHandler,
                new ServiceIdentity("system/cache", "system/cache"), nodeFactory);
        final Cache cache = new Cache(cacheConnector, kor, configService);
        final VslRequestHandler cachedRequestHandler = new CachingRequestRouterDecorator(
                configService, requestHandler, cache);

        final VslConnector searchProviderConnector = new RequestHandlerToConnectorBridge(
                cachedRequestHandler, new ServiceIdentity("search", "search"), nodeFactory);
        final SearchProviderService searchProviderService = new SearchProviderService(
                searchProviderConnector, statisticsProvider, typeSearchProvider);

        final VslParametrizedConnector korSyncConnector = new RequestHandlerToConnectorBridge(
                cachedRequestHandler,
                new ServiceIdentity("system/" + configService.getAgentName() + "/korSync",
                        "system/korSync;system/agentRegistryService"),
                nodeFactory);
        final KORSyncHandler korSyncHandler = new KORSyncHandler(transportManager, kor,
                agentRegistryService, korSyncConnector, configService);
        final AlivePingHandler pingHandler = new AlivePingHandler(transportManager, certAuth, kor,
                agentRegistryService, configService, korSyncHandler);

        StatisticService statisticsService = null;
        if (!useDummyStatistics) {
            final VslConnector statisticsConnector = new RequestHandlerToConnectorBridge(
                    cachedRequestHandler,
                    new ServiceIdentity("system/statistics", "system/statistics"), nodeFactory);
            statisticsService = new StatisticService((StatisticsProvider) statisticsProvider,
                    statisticsConnector, configService);
        }

        final VslConnector typeSearchConnector = new RequestHandlerToConnectorBridge(
                cachedRequestHandler, new ServiceIdentity("typeSearch", "typeSearch"), nodeFactory);
        final TypeSearchService typeSearchService = new TypeSearchService(typeSearchConnector,
                statisticsProvider, typeSearchProvider);

        SlmrService slmrService = null;
        if (InitialConfigFromFile.getInstance().getBooleanProperty("modelRepository.isSLMR",
                false)) {
            final VslConnector slmrConnector = new RequestHandlerToConnectorBridge(
                    cachedRequestHandler, new ServiceIdentity("slmr", "slmr"), nodeFactory);
            slmrService = new SlmrService(slmrConnector, configService);
        }

        // TODO use simple factory if everything is implemented there
        final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);
        final RestTransportContext restContext = new RestTransportContext(configService,
                certAuth.getKeyStore(), keystorePassword, mapperFactory);

        final MulticastTransport multicastTransport = new MulticastTransport(1234,
                ipv6MulticastGroup, pingHandler, mapperFactory.getMapper(VslMimeTypes.CBOR),
                agentRegistryService, korSyncHandler, configService, certAuth);

        // activate KOR internal mechanisms
        final VslConnector modelCacheConnector = new RequestHandlerToConnectorBridge(
                cachedRequestHandler, new ServiceIdentity("modelCache", "modelCache"), nodeFactory);
        // FIXME: ugly
        kor.activate(configService, modelCacheConnector);
        activatedModules.add(kor);

        activateModule(cache);
        activateModule(searchProviderService);
        activateModule((ConfigService) configService);
        activateModule(typeSearchService);

        if (slmrService != null && InitialConfigFromFile.getInstance()
                .getBooleanProperty("modelRepository.isSLMR", false)) {
            activateModule(slmrService);
        }
        activateModule(agentRegistryService);

        // FIXME: korSyncHandler.shutdown freezes KA?!
        // activateModule(korSyncHandler);
        korSyncHandler.activate();

        subscriptionManager.activate(agentRegistryService, transportManager, configService);
        // FIXME: SubscriptionManager is not a module?!

        // add and activate REST transports from helper function
        // HACK: add benchmarking support here
        final VslRequestHandler benchmarkHandler = new BenchmarkRequestRouterDecorator(
                (AbstractRequestRouter) cachedRequestHandler);
        activateAndAddRestTransports(certAuth, benchmarkHandler, korSyncHandler, kor, restContext, transportManager);

        // run and register multicast transport
        activateModule(multicastTransport);
        transportManager.registerTransport(multicastTransport);

        // activate KA
        activateModule(pingHandler);
        if (statisticsService != null) {
            activateModule(statisticsService);
        }
        LOGGER.info("All KA systems activated.");
    }

    @Override
    public final void shutdown() {
        Collections.reverse(activatedModules);
        for (final AbstractVslModule module : activatedModules) {
            LOGGER.info("Shutdown module {}", module);
            try {
                module.shutdown();
            } catch (final RuntimeException e) {
                LOGGER.error("Shutdown of module {} raised RuntimeException:", module, e);
            }
        }
    }

    /**
     * Activate the module and track it in activated modules.
     *
     * @param module
     *            the module to activate.
     * @throws Exception
     *             If the activation of the module fails.
     */
    protected final void activateModule(final AbstractVslModule module) throws Exception {
        LOGGER.info("Activating module {}", module);
        module.activate();
        activatedModules.add(module);
    }

    /**
     * Internal helper to activate RESTful unicast transports and add them to the activated modules
     * list.
     * <p>
     * This implementation provides the Java 7 RESTful transports, but derived classes can replace
     * them.
     * </p>
     *
     * @param sslAuthenticator
     *            the {@link VslX509Authenticator}.
     * @param requestHandler
     *            the {@link VslRequestHandler}.
     * @param korSyncHandler
     *            the {@link VslKORSyncHandler}.
     * @param korUpdateHandler
     *            the {@link VslKORUpdateHandler}.
     * @param restContext
     *            the {@link RestTransportContext}.
     * @param transportManager
     *            the {@link TransportManager}.
     * @throws Exception
     *             If the activation of the transport fails.
     */
    protected void activateAndAddRestTransports(final VslX509Authenticator sslAuthenticator,
            final VslRequestHandler requestHandler, final VslKORSyncHandler korSyncHandler,
            final VslKORUpdateHandler korUpdateHandler, final RestTransportContext restContext,
            final TransportManager transportManager) throws Exception {
        final ResourceHandler resourceHandler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should
        // be served out of.
        // In this example it is the current directory but it can be configured to anything that the
        // JVM has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[] { "index.html" });
        resourceHandler.setResourceBase(".");

        final JettyRestTransport restTransport = new JettyRestTransport(sslAuthenticator,
                requestHandler, korSyncHandler, korUpdateHandler, restContext, resourceHandler);

        // activate the module and register the transport
        activateModule(restTransport);
        transportManager.registerTransport(restTransport);

        // SSL debugging
        // LOGGER.info("Allowed protocols: {}",
        // Arrays.deepToString(jettyContext.getSslContextFactory().getIncludeProtocols()));
        // LOGGER.info("Available protocols: {}",
        // Arrays.deepToString(jettyContext.getSslContextFactory().getSslContext()
        // .getDefaultSSLParameters().getProtocols()));
        // LOGGER.info("Allowed ciphers: {}", Arrays
        // .deepToString(jettyContext.getSslContextFactory().getIncludeCipherSuites()));
        // LOGGER.info("Available ciphers: {}",
        // Arrays.deepToString(jettyContext.getSslContextFactory().getSslContext()
        // .getDefaultSSLParameters().getCipherSuites()));
        // LOGGER.info("Matching ciphers: {}",
        // findDupes(
        // jettyContext.getSslContextFactory().getSslContext()
        // .getDefaultSSLParameters().getCipherSuites(),
        // jettyContext.getSslContextFactory().getIncludeCipherSuites()));
    }

    /**
     * Get the agent number.
     *
     * @return agent number for internal usage.
     */
    public final int getAgentNum() {
        return agentNum;
    }

    /**
     * The main method.
     *
     * @param args
     *            process arguments.
     */
    public static void main(final String[] args) {
        if (Arrays.asList(args).contains("--helpConfig")) {
            System.out
                    .println(DescriptionProcessor.getDocumentation(VslConfigurationService.class));
            return;
        }

        final int agentNum;
        if (args.length >= 1) {
            agentNum = Integer.parseInt(args[0]);
        } else {
            agentNum = 1;
        }

        // add Bouncy Castle provider in case JVM does not support all ciphers
        Security.addProvider(new BouncyCastleProvider());

        final Java7KnowledgeAgent agent = new Java7KnowledgeAgent(agentNum);
        agent.run();
    }
}
