package org.bch.security.oauth.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by CH176656 on 5/20/2015.
 * Follows specifications for oauth2 two legged according to
 * <a href="https://tools.ietf.org/html/draft-ietf-oauth-v2-31#section-4.4">this</a>
 */
public class OAuth2Server extends HttpServlet {
    private Logger log = LoggerFactory.getLogger(OAuth2Server.class);

    public static final long ONE_SECOND_IN_MILLIS = 1000;
    public static final int DEFAULT_TOKEN_SIZE= 64;
    public static final int DEFAULT_SECONDS = 3600;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._";
    //private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9/_~\\.\\+\\-]+=*");
    private static final String DEFAULT_DATE_PATTERN_ORACLE_DB = "YYYYMMDD-HH24:MI:SS";
    private static final String  DEFAULT_DATE_PATTERN_JAVA= "yyyyMMdd-HH:mm:ss";
    private static final String DEFAULT_INSERT_TOKEN= "Insert into UserTokens values('%s', '%s', to_date('%s', '%s'))";

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String CONTENT_TYPE = "application/json";

    public static final String CONF_PARAM_DATASOURCE = "dataSource";
    public static final String CONF_PARAM_SECONDS = "secondsToExpiration";
    public static final String CONF_PARAM_TOKEN_SIZE = "tokenSize";

    private static final String JSON_OUTPUT =
            "{\n" +
            "    \"access_token\": \"%s\",\n" +
            "    \"expires_in\": %d,\n" +
            "    \"token_type\": \"bearer\"\n" +
            "}";

    /**
     * @return The jndi name of the data source to store the tokens
     */
    protected String getDatasourceName() {
        String out = getServletConfig().getInitParameter(CONF_PARAM_DATASOURCE);
        return out;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String grantType = request.getParameter(GRANT_TYPE);
        if (grantType==null) {
            log.info("Bad request: granttype is null");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!(grantType.toLowerCase().equals(CLIENT_CREDENTIALS.toLowerCase()))) {
            log.info("Bad request: granttype: " + grantType + " not supported");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            String newToken = this.randomToken();
            Date date = new Date();
            long aux=date.getTime();
            Date expirationDate = new Date(aux + (this.getSeconds()*ONE_SECOND_IN_MILLIS));
            SimpleDateFormat dateFormat = new SimpleDateFormat(this.getDateJavaFormat());
            String dateStr = dateFormat.format(expirationDate);

            String username = request.getUserPrincipal().getName();
            String insertSQL = String.format(this.getInsertSentence(),
                    username, newToken, dateStr, this.getDBDateFormat());

            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(this.getDatasourceName());
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(insertSQL);
            conn.close();

            String responseJSON = String.format(JSON_OUTPUT, newToken, this.getSeconds());
            response.setContentType(CONTENT_TYPE);
            PrintWriter out = response.getWriter();
            out.write(responseJSON);
            log.info("User " + username + " authenticated. Access token has been generated");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
        catch(NamingException ex) {
            ex.printStackTrace();
            LoginException le = new LoginException("Error looking up DataSource from: "+this.getDatasourceName());
            le.initCause(ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch(SQLException ex) {
            ex.printStackTrace();
            LoginException le = new LoginException("Query failed");
            le.initCause(ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch(Exception ex) {
            ex.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Override if another insert sentence is needed
     * @return The insert sentence pattern: with four %s: username, token, expiration date in string, the pattern
     */
    protected String getInsertSentence() {
        return DEFAULT_INSERT_TOKEN;
    }
    /**
     * Override if another date pattern is needed
     * @return the date format to be used in the DB
     */
    protected String getDBDateFormat() {
        return DEFAULT_DATE_PATTERN_ORACLE_DB;
    }

    /**
     * Override if another java date format is needed
     * @return the date format used to store the token expiration dates
     */
    protected String getDateJavaFormat () {
        return DEFAULT_DATE_PATTERN_JAVA;
    }

    /**
     * @return the seconds that the token will be valid for.: from Now to Now+seconds
     */
    protected int getSeconds() {
        String sec = getServletConfig().getInitParameter(CONF_PARAM_SECONDS);
        if (sec==null) return DEFAULT_SECONDS;
        return Integer.parseInt(sec);
    }


    /**
     * @return the size in bytes of the token.
     */
    protected int getTokenSize() {
        String sizeStr = getServletConfig().getInitParameter(CONF_PARAM_TOKEN_SIZE);
        if (sizeStr==null) return DEFAULT_TOKEN_SIZE;
        return Integer.parseInt(sizeStr);
    }

    /**
     * Generate a random token that conforms to RFC 6750 Bearer Token
     * @return a new token that is URL Safe (no '+' or '/' characters). */
    protected String randomToken() {
        byte[] bytes = new byte[this.getTokenSize()];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(this.getTokenSize());
        for (byte b : bytes) {
            sb.append(TOKEN_CHARS.charAt(b & 0x3F));
        }
        return sb.toString();
    }
}
