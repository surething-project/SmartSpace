package org.ds2os.vsl.rest.jetty.server;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.exception.FailedAuthenticationException;
import org.ds2os.vsl.rest.HttpHandler;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Wraps a {@link HttpHandler} in a servlet for Jetty.
 *
 * @author felix
 */
public final class JettyServlet extends HttpServlet {

    /**
     * The servlet attribute where the {@link HttpHandler} is stored.
     */
    public static final String HTTP_HANDLER_ATTRIBUTE = "org.ds2os.httpHandler";

    /**
     * Servlets must be serializable.
     */
    private static final long serialVersionUID = 1634192547300517993L;

    @Override
    public void init() throws ServletException {
        if (getServletContext().getAttribute(JettyServlet.HTTP_HANDLER_ATTRIBUTE) == null) {
            throw new UnavailableException("Could not get HTTP handler.");
        }
    }

    @Override
    protected void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final X509Certificate[] certs = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");
        final HttpHandler handler = (HttpHandler) getServletContext()
                .getAttribute(HTTP_HANDLER_ATTRIBUTE);

        /*
         * from https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS#
         * Requests_with_credentials When responding to a credentialed request, server must specify
         * a domain and cannot use wild carding. The example would fail if the header was wildcarded
         * as: Access-Control-Allow-Origin: *.
         *
         * https://bugzilla.mozilla.org/show_bug.cgi?id=1019603
         *
         * -> Therefore we have to send the Allow-Origin to the Origin from which the request comes
         * from. --Andreas Hubel, Sep 2016
         */

        if (request.getHeader("Origin") != null) {
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        if (certs == null || certs.length < 1) {
            // Allow OPTIONS requests without client certs (aka Firefox Workaround)
            if (request.getMethod().equals("OPTIONS")) {
                handler.handle(request, response, null);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "TLS client auth is required.");
            }
            return;
        }
        final VslIdentity identity;
        try {
            identity = handler.authenticate(certs, request.getHeader("Authorization"));
        } catch (final FailedAuthenticationException e) {
            response.sendError(e.getErrorCode(), e.getMessage());
            return;
        }
        handler.handle(request, response, identity);
    }
}
