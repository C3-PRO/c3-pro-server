package org.bch.security.oauth.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Filter for end points that need protection via oauth2
 * @author CHIP-IHL
 */
public class OAuthFilter implements Filter {

    /**
     * The init filter
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
            request.login("", "");
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
            //err.setErrorDesc(e.getMessage());
            err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * The destroy method
     */
    @Override
    public void destroy() {}

}

