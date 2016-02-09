package org.bch.security.oauth.server;

import org.jboss.resteasy.util.Base64;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URLDecoder;

/**
 * Filter for the oauth server.
 * Expects BASIC authentication, which is validated through the standard LoginModule
 * @author CHIP-IHL
 */
public class OAuthServerFilter implements Filter{
    private static String BASIC_AUTH = "Basic";

    /**
     * The init method
     * @param filterConfig
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /**
     * The doFilter method
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            String auth64 = request.getHeader("Authorization");
            if (!isBasicAuth(auth64)) {
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                ErrorReturn err = new ErrorReturn();
                err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
                err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String [] parts = auth64.split(" ");
            if (parts.length!=2) {
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                ErrorReturn err = new ErrorReturn();
                err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
                err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            byte [] authBytes = Base64.decode(parts[1]);
            String auth = new String(authBytes, "UTF-8");
            auth = URLDecoder.decode(auth, "UTF-8");
            String []cred = auth.split(":");
            request.login(cred[0], cred[1]);
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            e.printStackTrace();
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
            err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * The destroy method
     */
    @Override
    public void destroy() {}

    /**
     * Return true if we are handling bearer Authentication
     * @param header
     * @return
     */
    protected boolean isBasicAuth(String header) {
        if (header == null) return false;

        // if its not a Basic header we deny the access
        if (header.length() < BASIC_AUTH.length()) return false;
        String pre = header.substring(0,BASIC_AUTH.length());
        if (!pre.toLowerCase().equals(BASIC_AUTH.toLowerCase())) return false;
        return true;
    }
}
