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
 * Extension of the standard DatabaseServerLoginModule class to incorporate oauth2 and bearer tokens
 * @author CHIP-IHL
 */
public class OAuth2LoginModule extends DatabaseServerLoginModule {
    protected static final String AUTHORIZATION_HEADER="Authorization";
    protected static final String BEARER_AUTH = "Bearer";
    protected static final String PARAM_NAME_TOKENS_QUERY = "tokensQuery";

    protected static final String FAKE_PASSWORD="";

    protected String tokensQuery = "Select username from UserTokens where expirationDate > sysdate and token = ?";

    protected String usernameAuth=null;
    private boolean isBearerAuth = false;

    /**
     * Initialize the module.
     * Grab the configuration parameters
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState, Map<String,?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        // We grab the possible configuration parameters of the module
        Object tmp = options.get(PARAM_NAME_TOKENS_QUERY);
        if (tmp!=null) {
            this.tokensQuery = tmp.toString();
        }
    }

    /**
     * Override the login method.
     * We detect if using Bearer Authentication
     * @return
     */
    @Override
    public boolean login() {

        try {
            HttpServletRequest request = (HttpServletRequest) javax.security.jacc.PolicyContext.
                    getContext("javax.servlet.http.HttpServletRequest");
            String auth = request.getHeader(AUTHORIZATION_HEADER);

            if (isBearerAuth(auth)) {
                String [] parts = auth.split(" ");
                //if the bearer token is not informed, we deny the login
                if (parts.length!=2) return false;

                String bearerToken = parts[1];
                String username = validateToken(bearerToken);
                if (username==null) return false;
                // At this point we know that the token is correct and corresponds to the username
                sharedState.put("javax.security.auth.login.name", username);
                // Since the token is validated we do not need any other credentials. So we set the password to empty.
                sharedState.put("javax.security.auth.login.password", FAKE_PASSWORD);
                this.usernameAuth=username;
                // Now we perform the standard login validation
                this.isBearerAuth = true;
            }

            return super.login();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Return true if we are handling bearer Authentication
     * @param header
     * @return
     */
    protected boolean isBearerAuth(String header) {
        if (header == null) return false;

        // if its not a Bearer header we deny the access
        if (header.length() < BEARER_AUTH.length()) return false;
        String pre = header.substring(0,BEARER_AUTH.length());
        if (!pre.toLowerCase().equals(BEARER_AUTH.toLowerCase())) return false;
        return true;
    }

    /**
     * Validates the token against the defined data source
     * @param token     The base64 encoded token
     * @return          (1) Null if token does not exists or is expired
     *                  (2) The corresponding username when token is valid
     * @throws LoginException
     */
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

    /**
     * Override the method to return a fake password if we are in Bearer mode.
     * Otherwise, we execute the parent method
     * @return
     * @throws LoginException
     */
    @Override
    protected String[] getUsernameAndPassword() throws LoginException {
        String[] out= new String[2];
        if (this.isBearerAuth) {
            out[0]= this.usernameAuth;
            out[1]= FAKE_PASSWORD;
        } else {
            out = super.getUsernameAndPassword();
        }
        return out;
    }

    /**
     * Override to return a fake password if we are in Bearer mode
     * Otherwise, we execute the parent method
     * @return
     * @throws LoginException
     */
    @Override
    protected String getUsersPassword() throws LoginException {
        String out=null;
        if (this.isBearerAuth) {
            out = FAKE_PASSWORD;
        } else {
            out = super.getUsersPassword();
        }
        return out;
    }

    /**
     * Override to return always true in the case we are in Bearer mode
     * Otherwise, we execute the parent method
     * @param inputPassword
     * @param expectedPassword
     * @return
     */
    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        if (this.isBearerAuth) return true;
        return super.validatePassword(inputPassword,expectedPassword);
    }

}
