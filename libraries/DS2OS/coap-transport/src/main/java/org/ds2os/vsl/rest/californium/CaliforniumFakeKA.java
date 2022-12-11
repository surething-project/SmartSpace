package org.ds2os.vsl.rest.californium;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.config.VslRestConfig;
import org.ds2os.vsl.core.utils.StaticConfig;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;

public class CaliforniumFakeKA {

	private final CoapServer server;

	public CaliforniumFakeKA() throws IOException, GeneralSecurityException {
		final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
		final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);
		final RestTransportContext context = new RestTransportContext(new VslRestConfig() {
			@Override
			public Set<String> getUsableInterfaces() {
				return StaticConfig.DEFAULT_REST_CONFIG.getUsableInterfaces();
			}

			@Override
			public boolean isLoopbackAllowed() {
				return StaticConfig.DEFAULT_REST_CONFIG.isLoopbackAllowed();
			}

			@Override
			public int getCallbackTimeout() {
				return StaticConfig.DEFAULT_REST_CONFIG.getCallbackTimeout();
			}

			@Override
			public int getPort() {
				return 8081;
			}

			@Override
			public String getContentTypePreference() {
				return StaticConfig.DEFAULT_REST_CONFIG.getContentTypePreference();
			}
		}, SSLUtils.loadKeyStore("agent1.jks", "K3yst0r3"), "K3yst0r3", mapperFactory);

		final ScandiumContext scandium = new ScandiumContext(context);
		server = new CoapServer();
		server.addEndpoint(new CoapEndpoint(scandium.getDtlsConnector(), NetworkConfig.getStandard()));
		server.add(new CaliforniumTransportResource(context, "vsl")
				.add(new CaliforniumTransportResource(context, "agent1")
						.add(new CaliforniumTransportResource(context, "benchmark")
								.add(new CaliforniumTransportResource(context, "simple"))))
				.add(new CaliforniumTransportResource(context, "benchmark")
						.add(new CaliforniumTransportResource(context, "simple"))));
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.destroy();
	}

	public static void main(final String[] args) {
		System.setProperty("tls.ephemeralDHKeySize", "2048");
		CaliforniumFakeKA ka;
		try {
			ka = new CaliforniumFakeKA();
		} catch (IOException e) {
			// TODO Automatisch generierter Erfassungsblock
			e.printStackTrace();
			return;
		} catch (GeneralSecurityException e) {
			// TODO Automatisch generierter Erfassungsblock
			e.printStackTrace();
			return;
		}
		ka.start();

		try {
			System.in.read();
		} catch (IOException e) {
		}

		ka.stop();
		System.exit(0);
	}
}
