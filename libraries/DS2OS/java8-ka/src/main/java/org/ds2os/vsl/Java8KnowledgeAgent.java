package org.ds2os.vsl;

import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdateHandler;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.config.DescriptionProcessor;
import org.ds2os.vsl.core.config.VslConfigurationService;
import org.ds2os.vsl.ka.TransportManager;
import org.ds2os.vsl.rest.JettyHTTP2RestTransport;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * Main class for the knowledge agent bundle for Java 8.
 *
 * @author felix
 */
public class Java8KnowledgeAgent extends Java7KnowledgeAgent {

    /**
     * Construct a Java8KnowledgeAgent instance with the given agent number.
     *
     * @param agentNum
     *            the agent number parsed from the command line.
     */
    public Java8KnowledgeAgent(final int agentNum) {
        super(agentNum);
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
    @Override
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

        final JettyHTTP2RestTransport restTransport = new JettyHTTP2RestTransport(sslAuthenticator,
                requestHandler, korSyncHandler, korUpdateHandler, restContext, resourceHandler);

        // activate the module and register the transport
        activateModule(restTransport);
        transportManager.registerTransport(restTransport);
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

        // enable 2048 bit DHE
        System.setProperty("tls.ephemeralDHKeySize", "2048");

        // add Bouncy Castle provider in case JVM does not support all ciphers
        Security.addProvider(new BouncyCastleProvider());

        final Java8KnowledgeAgent agent = new Java8KnowledgeAgent(agentNum);
        agent.run();
    }
}
