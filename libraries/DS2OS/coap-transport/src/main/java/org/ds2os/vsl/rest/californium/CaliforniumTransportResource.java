package org.ds2os.vsl.rest.californium;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.test.TestNodes;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class CaliforniumTransportResource extends CoapResource {
	private final RestTransportContext context;

	public CaliforniumTransportResource(final RestTransportContext context, final String name) {
		super(name);
		this.context = context;
	}

	@Override
	public final void handleGET(CoapExchange exchange) {
		final int coapAccept = exchange.getRequestOptions().getAccept();
		final String accept = MediaTypeRegistry.toString(coapAccept);
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		Response response;
		try {
			context.getMapper(accept).writeValue(output, TestNodes.SIMPLE_DATA_NODE);
			response = new Response(ResponseCode.CONTENT);
			response.setOptions(new OptionSet().setContentFormat(coapAccept));
			response.setPayload(output.toByteArray());
		} catch (final IOException e) {
			response = new Response(ResponseCode.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				output.close();
			} catch (final IOException e) {
				// forget it...
			}
		}
		exchange.respond(response);
	}
}
