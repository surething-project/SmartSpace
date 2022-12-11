package org.ds2os.vsl.rest.jetty.server;

import org.ds2os.vsl.rest.HttpHandler;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.jetty.JettyContext;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Creates an embedded Jetty HTTP server.
 *
 * @author felix
 */
public class JettyServer {

    /**
     * The SLF4J logger.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

    /**
     * The {@link JettyContext}.
     */
    protected final JettyContext context;

    /**
     * The Jetty server bootstrapper.
     */
    protected final Server jetty;

    /**
     * The listen addresses which were added.
     */
    protected final List<InetAddress> listenAddresses = new ArrayList<InetAddress>();

    /**
     * Create a new Jetty server instance without connectors.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     * @param handler
     *            the handler of all requests.
     * @param otherHandlers
     *            TODO: extra handlers hack.
     */
    public JettyServer(final RestTransportContext context, final HttpHandler handler,
            final Handler... otherHandlers) {
        this.context = new JettyContext(context);

        final ThreadPool pool = new QueuedThreadPool();
        jetty = new Server(pool);
        jetty.manage(pool);

        // no dumps, stop server on JVM shutdown
        jetty.setDumpAfterStart(false);
        jetty.setDumpBeforeStop(false);
        jetty.setStopAtShutdown(true);

        // create error handler
        final ErrorHandler errorHandler = new JettyErrorHandler(handler);

        // create a servlet contextFactory handler
        final ServletContextHandler servletContext = new ServletContextHandler(
                null, "/",
                ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY
        );
        servletContext.setAttribute(JettyServlet.HTTP_HANDLER_ATTRIBUTE, handler);
        servletContext.addServlet(JettyWebsocketServlet.class, "/callbacks");
        servletContext.addServlet(JettyServlet.class, "/*");
        servletContext.setErrorHandler(errorHandler);

        HandlerList handlers = new HandlerList();

        // add other handlers
        for (Handler h : otherHandlers) {
            handlers.addHandler(h);
        }

        // add the servlet contextFactory handler to the server
        handlers.addHandler(servletContext);
        jetty.setHandler(handlers);
    }

    /**
     * Add a new HTTPs connector to the server.
     *
     * @param listenAddrs
     *            the listen addresses.
     * @param port
     *            the port.
     */
    public final void addHttpsConnector(final Collection<InetAddress> listenAddrs, final int port) {
        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecurePort(port);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        httpsConfig.setSendServerVersion(false);
        httpsConfig.setSendXPoweredBy(false);

        for (final InetAddress listen : listenAddrs) {
            final ServerConnector httpsConnector = new ServerConnector(jetty,
                    new SslConnectionFactory(context.getSslContextFactory(),
                            HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setHost(listen.getHostAddress());
            httpsConnector.setPort(port);
            httpsConnector.setReuseAddress(true);
            httpsConnector.setIdleTimeout(RestTransportContext.IDLE_TIMEOUT);

            jetty.addConnector(httpsConnector);
            listenAddresses.add(listen);
        }
    }

    /**
     * Start the Jetty server.
     *
     * @throws IOException
     *             If Jetty throws an exception.
     */
    public final void start() throws IOException {
        synchronized (jetty) {
            if (!jetty.isRunning()) {
                LOGGER.info("Starting Jetty server...");
                try {
                    jetty.start();
                } catch (final Exception e) {
                    LOGGER.error("Failed to start Jetty:", e);
                    throw new IOException("Jetty startup failed.", e);
                }
                LOGGER.info("Jetty server is started.");
            }
        }
    }

    /**
     * Stop the Jetty server.
     */
    public final void stop() {
        synchronized (jetty) {
            if (jetty.isRunning()) {
                LOGGER.info("Stopping Jetty server...");
                try {
                    jetty.stop();
                } catch (final Exception e) {
                    LOGGER.error("Failed to stop Jetty:", e);
                }
            }
        }
    }

    /**
     * Join the Jetty server, i.e. wait for its clean shutdown.
     *
     * @throws InterruptedException
     *             If the current thread is interrupted.
     */
    public final void join() throws InterruptedException {
        synchronized (jetty) {
            if (!jetty.isRunning()) {
                return;
            }
        }
        jetty.join();
        LOGGER.info("Jetty server stopped.");
    }

    /**
     * Get all local URLs.
     *
     * <p>
     * TODO: get local network addresses only optionally (esp. loopback).
     * </p>
     *
     * @return an iterable of all local URLs.
     */
    public final Iterable<URL> getLocalURLs() {
        final List<URL> result = new ArrayList<URL>();
        for (final Connector connector : jetty.getConnectors()) {
            if (connector instanceof ServerConnector) {
                try {
                    final int port = ((ServerConnector) connector).getLocalPort();
                    final InetAddress address = InetAddress
                            .getByName(((ServerConnector) connector).getHost());

                    if (address instanceof Inet4Address) {
                        result.add(
                                new URL("https://" + address.getHostAddress() + ":" + port + "/"));
                    } else if (address instanceof Inet6Address) {
                        String strippedAddress = address.getHostAddress();
                        if (strippedAddress.contains("%")) {
                            strippedAddress = strippedAddress.substring(0,
                                    strippedAddress.indexOf("%"));
                        }
                        result.add(new URL("https://[" + strippedAddress + "]:" + port + "/"));
                    }
                } catch (final MalformedURLException e) {
                    LOGGER.error("Cannot build server URL:", e);
                } catch (final UnknownHostException e) {
                    LOGGER.error("Cannot resolve Jetty host to IP address:", e);
                }
            }
        }
        return result;
    }
}
