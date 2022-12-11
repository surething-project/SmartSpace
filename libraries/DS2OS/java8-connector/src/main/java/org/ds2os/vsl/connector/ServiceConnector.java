package org.ds2os.vsl.connector;

import org.ds2os.vsl.core.*;
import org.ds2os.vsl.core.config.VslRestConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.StaticConfig;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.client.RestConnector;
import org.ds2os.vsl.rest.jetty.client.JettyHTTP2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Standard service connector with default wiring for simple instantiation
 * (copied from java7-connector, but changed JettyClient to JettyHTTP2Client).
 *
 * @author felix
 */
public class ServiceConnector extends AbstractVslModule implements VslParametrizedConnector {

	/**
	 * The SLF4J logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ServiceConnector.class);

	/**
	 * The URL to the KA to which the service will be connected.
	 */
	private final String knowledgeAgent;

	/**
	 * The path to the keystore of the service.
	 */
	private final String keystorePath;

	/**
	 * The password of the keystore of the service.
	 */
	private final String keystorePassword;

	/**
	 * The jetty client for shutdown. Will be set once the module is activated.
	 */
	private JettyHTTP2Client jettyClient;

	/**
	 * The connector once the module is activated.
	 */
	private VslParametrizedConnector connector;

	/**
	 * Instantiate a new service connector with the required information.
	 *
	 * @param knowledgeAgent
	 *            the URL to the KA to which the service will be connected.
	 * @param keystorePath
	 *            the path to the keystore of the service.
	 * @param keystorePassword
	 *            the password of the keystore of the service.
	 */
	public ServiceConnector(final String knowledgeAgent, final String keystorePath, final String keystorePassword) {
		this.knowledgeAgent = knowledgeAgent;
		this.keystorePath = keystorePath;
		this.keystorePassword = keystorePassword;
	}

	@Override
	public final void activate() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
		synchronized (this) {
			LOG.info("Activate service connector to {} with keystore from {}.", knowledgeAgent, keystorePath);
			final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
			final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);
			final VslRestConfig config = StaticConfig.DEFAULT_REST_CONFIG;
			final RestTransportContext context = new RestTransportContext(config,
					SSLUtils.loadKeyStore(keystorePath, keystorePassword), keystorePassword, mapperFactory);
			jettyClient = new JettyHTTP2Client(context);
			jettyClient.start();

			final RestConnector rc = new RestConnector(jettyClient, knowledgeAgent, context, nodeFactory);
			jettyClient.setWsClientHandler(rc);

			connector = rc;
			LOG.info("Service connector created.");
		}
	}

	@Override
	public final void shutdown() {
		synchronized (this) {
			LOG.info("Stopping service connector.");
			if (jettyClient != null) {
				jettyClient.stop();
			}
			LOG.info("Service connector stopped.");
		}
	}

	/**
	 * Check the connector and throw a proper exception if this module is not
	 * activated yet.
	 *
	 * @return the connector for convenient one-liners.
	 * @throws VslException
	 *             If the module is not yet activated.
	 */
	protected final VslParametrizedConnector checkConnector() throws VslException {
		if (connector == null) {
			throw new InvalidOperationException("Service connector is not yet activated.");
		} else {
			return connector;
		}
	}

	@Override
	public final VslNodeFactory getNodeFactory() {
		if (connector != null) {
			return connector.getNodeFactory();
		} else {
			return new VslNodeFactoryImpl();
		}
	}

	@Override
	public final String getRegisteredAddress() {
		if (connector != null) {
			return connector.getRegisteredAddress();
		} else {
			return "";
		}
	}

	@Override
	public final String registerService(final VslServiceManifest manifest) throws VslException {
		return checkConnector().registerService(manifest);
	}

	@Override
	public final void unregisterService() throws VslException {
		checkConnector().unregisterService();
	}

	@Override
	public final VslNode get(final String address) throws VslException {
		return checkConnector().get(address);
	}

	@Override
	public final VslNode get(final String address, final VslAddressParameters params) throws VslException {
		return checkConnector().get(address, params);
	}

	@Override
	public InputStream getStream(String address) throws VslException {
		return checkConnector().getStream(address);
	}

	@Override
	public void setStream(String address, InputStream stream) throws VslException {
		checkConnector().setStream(address, stream);
	}

	@Override
	public final void set(final String address, final VslNode knowledge) throws VslException {
		checkConnector().set(address, knowledge);
	}

	@Override
	public final void notify(final String address) throws VslException {
		checkConnector().notify(address);
	}

	@Override
	public final void subscribe(final String address, final VslSubscriber subscriber) throws VslException {
		checkConnector().subscribe(address, subscriber);
	}

	@Override
	public final void unsubscribe(final String address) throws VslException {
		checkConnector().unsubscribe(address);
	}

	@Override
	public final void subscribe(final String address, final VslSubscriber subscriber, final VslAddressParameters params)
			throws VslException {
		checkConnector().subscribe(address, subscriber, params);
	}

	@Override
	public final void unsubscribe(final String address, final VslAddressParameters params) throws VslException {
		checkConnector().unsubscribe(address, params);
	}

	@Override
	public final void lockSubtree(final String address, final VslLockHandler lockHandler) throws VslException {
		checkConnector().lockSubtree(address, lockHandler);
	}

	@Override
	public final void commitSubtree(final String address) throws VslException {
		checkConnector().commitSubtree(address);
	}

	@Override
	public final void rollbackSubtree(final String address) throws VslException {
		checkConnector().rollbackSubtree(address);
	}

	@Override
	public final void registerVirtualNode(final String address, final VslVirtualNodeHandler virtualNodeHandler)
			throws VslException {
		checkConnector().registerVirtualNode(address, virtualNodeHandler);
	}

	@Override
	public final void unregisterVirtualNode(final String address) throws VslException {
		checkConnector().unregisterVirtualNode(address);
	}
}
