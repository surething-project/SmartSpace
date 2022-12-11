package org.ds2os.benchmark.resttransport;

import static org.ds2os.vsl.netutils.TestHelper.randomData;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.cert.CertificateAuthority;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.config.VslRestConfig;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.SimpleVslHttpHandler;
import org.ds2os.vsl.rest.client.RestConnector;
import org.ds2os.vsl.rest.jetty.client.JettyClient;
import org.ds2os.vsl.rest.jetty.server.JettyServer;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark the rest transport.
 *
 * @author Johannes Straßer
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 5)
@Warmup(iterations = 3)
@Fork(warmups = 1, value = 2)
@State(value = Scope.Thread)
public class RestTransportBenchmark {

    /**
     * Length of the test data.
     */
    @Param({ ("100"), ("10000"), ("1000000") })
    private int dataLength;

    /**
     * The path to the keystore for the server.
     */
    private static final String KEYSTORE_PATH_1 = "agent1.jks";
    /**
     * The path to the keystore for the client.
     */
    private static final String KEYSTORE_PATH_2 = "agent2.jks";
    /**
     * The passowrd for the keystores.
     */
    private static final String KEYSTORE_PASSWORD = "K3yst0r3";
    /**
     * The used port.
     */
    private static final int PORT = 23456;

    /**
     * The rest server used for benchmarking.
     */
    private JettyServer server;
    /**
     * The rest client used for benchmarking.
     */
    private JettyClient client;
    /**
     * The connector used for benchmarking.
     */
    private VslConnector testConnector;

    /**
     * data string prepared per invocation.
     */
    private String data;

    /**
     * Mocked requestHandler that needs to be set per invocation.
     */
    private VslRequestHandler requestHandler;

    /**
     * Set up the server, the client, and the connector.
     *
     * @throws IOException
     *             should not happen
     * @throws KeyStoreException
     *             should not happen
     * @throws NoSuchAlgorithmException
     *             should not happen
     * @throws CertificateException
     *             should not happen
     * @throws GeneralSecurityException
     *             should not happen
     */
    @Setup(value = Level.Iteration)
    public final void setupEnvironment() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, GeneralSecurityException {
        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);

        // Start Server
        requestHandler = mock(VslRequestHandler.class);
        final VslRestConfig conf = mock(VslRestConfig.class);
        when(conf.getUsableInterfaces()).thenReturn(Collections.singleton("*"));
        when(conf.getCallbackTimeout()).thenReturn(30);
        when(conf.getContentTypePreference()).thenReturn("application/json");
        final RestTransportContext context = new RestTransportContext(conf,
                SSLUtils.loadKeyStore(KEYSTORE_PATH_1, KEYSTORE_PASSWORD), KEYSTORE_PASSWORD,
                mapperFactory);
        server = new JettyServer(context, new SimpleVslHttpHandler(
                new CertificateAuthority(KEYSTORE_PATH_1, KEYSTORE_PASSWORD), requestHandler,
                mock(VslKORSyncHandler.class), mock(VslKORUpdateHandler.class), context));
        server.addHttpsConnector(Arrays.asList(InetAddress.getLocalHost()), PORT);
        server.start();

        // Start client
        final RestTransportContext context2 = new RestTransportContext(conf,
                SSLUtils.loadKeyStore(KEYSTORE_PATH_2, KEYSTORE_PASSWORD), KEYSTORE_PASSWORD,
                mapperFactory);
        client = new JettyClient(context2);
        client.start();

        // Start connector
        testConnector = new RestConnector(client, "https://localhost:" + PORT + "/", context2,
                nodeFactory);
    }

    /**
     * Prepares data and requestHandler for the test methods.
     *
     * @throws VslException
     *             cannot happen
     */
    @Setup(value = Level.Invocation)
    public final void setupData() throws VslException {
        data = DatatypeConverter.printHexBinary(randomData(dataLength));
        when(requestHandler.get(eq("/bla"), any(VslIdentity.class)))
                .thenReturn(testConnector.getNodeFactory().createImmutableLeaf(data));
    }

    /**
     * Shut down client and server.
     *
     * @throws InterruptedException
     *             Should not happen
     */
    @TearDown(value = Level.Iteration)
    public final void teardownEnvironment() throws InterruptedException {
        client.stop();

        server.stop();
        server.join();
    }

    /**
     * Use a setRequest to generate traffic.
     *
     * @throws VslException
     *             When the set fails
     */
    @Benchmark
    public final void setBenchmark() throws VslException {
        testConnector.set("/bla/", testConnector.getNodeFactory().createImmutableLeaf(data));
    }

    /**
     * Use a getRequest to generate traffic.
     *
     * @throws VslException
     *             When the get fails
     */
    @Benchmark
    public final void getBenchmark() throws VslException {
        assertThat(testConnector.get("/bla").getValue(), is(data));
    }
}
