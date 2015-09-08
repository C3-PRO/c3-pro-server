package org.bch.security.oauth.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by CH176656 on 5/19/2015.
 */
public class OAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {

            HttpServletRequest request = (HttpServletRequest)servletRequest;
            request.login("", "");
            String auth = request.getHeader("Authorization");
            System.out.println("UNAUTHORIZED");
            System.out.println(auth);
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
            //err.setErrorDesc(e.getMessage());
            err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    public void destroy() {}

}

