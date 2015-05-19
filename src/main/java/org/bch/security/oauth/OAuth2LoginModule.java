package org.bch.security.oauth;

import org.jboss.security.auth.spi.DatabaseServerLoginModule;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.transaction.Transaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by CH176656 on 5/19/2015.
 */
public class OAuth2LoginModule extends DatabaseServerLoginModule {
    protected static final String AUTHORIZATION_HEADER="Authorization";
    protected static final String BEARER_AUTH = "Bearer";
    protected static final String PARAM_NAME_TOKENS_QUERY = "tokensQuery";
    protected static final String FAKE_PASSWORD="";

    protected String tokensQuery = "Select username from UserTokens where expirationDate > sysdate and token = ?";

    protected String usernameAuth=null;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState, Map<String,?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        System.out.println("In OUR MODULE!!!!!!!!!!!!!!");
        Object tmp = options.get(PARAM_NAME_TOKENS_QUERY);
        if (tmp!=null) {
            this.tokensQuery = tmp.toString();
        }

    }
    @Override
    public boolean login() {

        try {
            HttpServletRequest request = (HttpServletRequest) javax.security.jacc.PolicyContext.
                    getContext("javax.servlet.http.HttpServletRequest");
            String auth = request.getHeader(AUTHORIZATION_HEADER);
            System.out.println("AUTH:::" + auth);
            // if auth header is not provided we deny the login
            if (auth == null) return false;

            // if its not a Bearer header we deny the access
            if (auth.length() < BEARER_AUTH.length()) return false;
            String pre = auth.substring(0,BEARER_AUTH.length());
            if (!pre.toLowerCase().equals(BEARER_AUTH.toLowerCase())) return false;

            String [] parts = auth.split(" ");
            //if the bearer token is not informed, we deny the login
            if (parts.length<2) return false;

            String bearerToken = parts[1];
            String username = validateToken(bearerToken);
            if (username==null) return false;

            // At this point we know that the token is correct and corresponds to the username
            sharedState.put("javax.security.auth.login.name", username);
            // Since the token is validated we do not need any other credentials. So we set the password to empty.
            sharedState.put("javax.security.auth.login.password", FAKE_PASSWORD);
            this.usernameAuth=username;
            // Now we perform the standard login validation
            return super.login();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected String validateToken(String token) throws LoginException {
        String username = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(dsJndiName);
            conn = ds.getConnection();
            // Get the username from the table
            ps = conn.prepareStatement(this.tokensQuery);
            ps.setString(1, token);
            System.out.println("Query: " +  this.tokensQuery);
            System.out.println("Token: " + token);

            rs = ps.executeQuery();
            if(!rs.next()) {
                throw new FailedLoginException("No matching username found in Principals");
            }
            username = rs.getString(1);
            //username = convertRawPassword(username);
        }
        catch(NamingException ex) {
            LoginException le = new LoginException("Error looking up DataSource from: "+dsJndiName);
            le.initCause(ex);
            throw le;
        } catch(SQLException ex) {
            LoginException le = new LoginException("Query failed");
            le.initCause(ex);
            throw le;
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch(SQLException e) {}
            }
            if( ps != null ) {
                try {
                    ps.close();
                } catch(SQLException e) {}
            }
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ex) {}
            }
        }
        return username;
    }

    @Override
    protected String[] getUsernameAndPassword() throws LoginException {
        String []out = {this.usernameAuth, FAKE_PASSWORD};
        return out;
    }

    @Override
    protected String getUsersPassword() throws LoginException {
        return FAKE_PASSWORD;
    }

}
