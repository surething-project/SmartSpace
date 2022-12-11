package org.ds2os.vsl.rest;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.utils.Base64;
import org.ds2os.vsl.exception.FailedAuthenticationException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Locale;

/**
 * Abstract {@link RestHandler} for the VSL REST transport.
 *
 * @author felix
 */
public abstract class AbstractVslRestHandler implements RestHandler {

    /**
     * The SLF4J log.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVslRestHandler.class);

    /**
     * {@link VslX509Authenticator} used for mapping X.509 certificates to Vsl identities.
     */
    private final VslX509Authenticator sslAuthenticator;

    /**
     * The {@link RestTransportContext}.
     */
    private final RestTransportContext context;

    /**
     * Inject the {@link VslX509Authenticator} used for mapping X.509 certificates to Vsl
     * identities.
     *
     * @param authenticator
     *            the authenticator used by this handler.
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     */
    protected AbstractVslRestHandler(final VslX509Authenticator authenticator,
            final RestTransportContext context) {
        sslAuthenticator = authenticator;
        this.context = context;
    }

    @Override
    public final RestTransportContext getRestTransportContext() {
        return context;
    }

    @Override
    public final VslMapper getMapper(final String accept) {
        // TODO: proper parsing of accept header (annoying...)
        String[] realAccept = new String[0];
        if (accept != null) {
            final String[] acceptArray = accept.split(",");
            realAccept = new String[acceptArray.length];
            for (int i = 0; i < acceptArray.length; i++) {
                realAccept[i] = acceptArray[i].split(";")[0].trim().toLowerCase(Locale.ROOT);
                if ("".equals(realAccept[i])) {
                    realAccept = Arrays.copyOf(realAccept, realAccept.length - 1);
                    i--;
                }
            }
        }
        return context.getMapper(realAccept);
    }

    @Override
    public final VslIdentity authenticate(final X509Certificate[] certs,
            final String authorizationHeader) throws FailedAuthenticationException {
        final VslIdentity vslIdentity;
        if (sslAuthenticator.isFromKA(certs[0])) {
            if (authorizationHeader == null) {
                vslIdentity = sslAuthenticator.authenticate(certs[0]);
            } else {
                try {
                    final ByteArrayInputStream input = new ByteArrayInputStream(
                            Base64.decode(authorizationHeader.substring("VSL ".length())));
                    try {
                        vslIdentity = context.getMapper(VslMimeTypes.JSON).readValue(input,
                                ServiceIdentity.class);
                    } finally {
                        input.close();
                    }
                } catch (final IOException e) {
                    LOG.debug("Error during parsing of the authorization header:", e);
                    throw new FailedAuthenticationException("Could not read authorization header.");
                }
            }
        } else {
            vslIdentity = sslAuthenticator.authenticate(certs[0]);
        }
        return vslIdentity;
    }

    /**
     * Process a GET request on the specified VSL address.
     *
     * @param address
     *            the VSL address.
     * @param query
     *            Query parameters of the request.
     * @param request
     *            the {@link ReceivedRestRequest} object.
     * @param vslIdentity
     *            the authenticated {@link VslIdentity}.
     * @throws VslException
     *             If a VslException occurs during VSL get operation.
     * @throws IOException
     *             If an I/O exception occurs during data transmission.
     */
    protected abstract void doGet(String address, String query, ReceivedRestRequest request,
            VslIdentity vslIdentity) throws VslException, IOException;
}
