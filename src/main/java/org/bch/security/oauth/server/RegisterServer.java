package org.bch.security.oauth.server;

import com.amazonaws.services.route53.model.ResourceRecordSet;
import org.apache.axiom.om.util.Base64;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.util.Mail;
import org.bch.c3pro.server.util.Utils;
import org.jboss.security.auth.spi.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.UUID;

/**
 * Created by CH176656 on 5/29/2015.
 * Registration server for credentials generation
 */
public class RegisterServer extends HttpServlet {
    private Logger log = LoggerFactory.getLogger(RegisterServer.class);

    protected static final String CONF_PARAM_HASH_ALGORITHM = "hashAlgorithm";
    protected static final String CONF_PARAM_DATASOURCE = "dataSource";

    protected static final String JSON_TAG_SANDBOX = "sandbox";
    protected static final String JSON_TAG_RECEIPT = "receipt-data";

    protected static final String DEFAULT_HASH_ALGORITHM = "SHA1";

    protected static final String ANTI_SPAM_HEADER = "Antispam";

    protected static final String SELECT_ANTISPAM = "Select token from AntiSpamToken where token='%s'";
    protected static final String INSERT_USER = "Insert into Users values ('%s', '%s')";
    protected static final String INSERT_USER_ROLE = "Insert into UserRoles values ('%s', '%s')";

    protected static final String USER_ROLES = "AppUser";
    protected static final String CONTENT_TYPE = "application/json";

    //protected static final String APPLE_ENDPOINT = "https://sandbox.itunes.apple.com/verifyReceipt";

    protected static final String APPLE_JSON_KEY_STATUS = "status";
    protected static final String APPLE_JSON_KEY_RECEIPT = "receipt";
    protected static final String APPLE_JSON_KEY_RECEIPT_BID = "bid";
    protected static final String APPLE_JSON_KEY_BUNDLE = "bundle_id";

    protected static final String JSON_REQUEST_APPLE =
            "{\n" +
            "  \"" + JSON_TAG_RECEIPT + "\":\"%s\" "+
            "}";

    protected static final String JSON_RESPONSE =
            "{\n" +
            "  \"client_id\":\"%s\",\n" +
            "  \"client_secret\": \"%s\",\n" +
            "  \"grant_types\": [\"client_credentials\"],\n" +
            "  \"token_endpoint_auth_method\":\"client_secret_basic\",\n" +
            "}";

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean validationOK=false;

        // Apply the AntiSpam filter
        if (!passFilter(request)) {
            log.info("Antispam token not validated!");
            //response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Mail.emailIfError("Antispam not validated: ", "Antispam token not validated", "Antispam");
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
            err.setErrorDesc("Antispam token not validated");
            err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // We read the content and validate the receipt
        String jsonPost = Utils.getPostContent(request);
        JSONObject json=null;
        try {
            json = new JSONObject(jsonPost);
            String receipt = json.getString(JSON_TAG_RECEIPT);
            validationOK = validateAppleReceipt(receipt);

        } catch (JSONException e) {
            log.error(e.getMessage());
            Mail.emailIfError("JSONException  Error during registration: ", "JSONException: " + e.getMessage()+"\n" +
                    "json string: " + jsonPost, "JSONException");
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_INVALID_REQUEST);
            err.writeError(response, HttpServletResponse.SC_BAD_REQUEST);
            return;
        } catch (Exception e) {
            log.error(e.getMessage());
            Mail.emailIfError("Exception during registration: ", "Exception: " + e.getMessage(), "Exception");
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_INVALID_REQUEST);
            err.setErrorDesc(e.getMessage());
            err.writeError(response, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // If no validation, the request is not authorized
        if (!validationOK) {
            log.warn("Apple receipt not valid");
            Mail.emailIfError("Apple receipt not valid: ","Invalid Apple receipt. Check logs for details" , "Apple");
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_UNAUTHORIZED_CLIENT);
            err.setErrorDesc("Apple receipt not valid");
            err.writeError(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } else {
            log.info("Apple receipt validated");
        }

        // At this point the request is authorized. We generate the credentials
        String clientId = generateClientId();
        String password = generatePassword();
        String encPassword = Util.createPasswordHash(this.getHashAlgorithm(),Util.BASE64_ENCODING,null,null, password);
        String insert = String.format(INSERT_USER,clientId, encPassword);
        String insertRoles = String.format(INSERT_USER_ROLE, clientId, USER_ROLES);
        Connection conn = null;
        Statement stmt=null;
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(this.getDatasourceName());
            conn = ds.getConnection();
            stmt = conn.createStatement();
            stmt.execute(insert);
            stmt.execute(insertRoles);
        } catch (Exception e) {
            log.error(e.getMessage());
            ErrorReturn err = new ErrorReturn();
            err.setErrorType(ErrorReturn.ErrorType.ERROR_INVALID_REQUEST);
            err.setErrorDesc(e.getMessage());
            err.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if( stmt != null ) {
                try {
                    stmt.close();
                } catch(SQLException e) {}
            }
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ex) {}
            }
        }

        // We generate the response and return 201
        String jsonResp = String.format(JSON_RESPONSE, clientId, password);
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        out.write(jsonResp);
        response.setStatus(HttpServletResponse.SC_CREATED);
        out.flush();
    }

    /**
     * Generates a clientId. In the default implementation, it is a randomly generated UUID
     * @return the new clientId
     */
    protected String generateClientId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the app ios id that will be used to compare the receipt data
     * @return
     */
    protected String getAppId() {
        try {
            return AppConfig.getProp(AppConfig.APP_IOS_ID);
        } catch (C3PROException e) {
            return "";
        }
    }

    protected String generatePassword() {
        Random rnd = new SecureRandom();
        byte[] key = new byte[64];
        rnd.nextBytes(key);
        return Base64.encode(key);
    }

    protected int validateAppeReceipt(String receipt, String urlStr) throws Exception {
        String jsonReq = String.format(JSON_REQUEST_APPLE, receipt);
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-type", "application/json");
        con.setRequestProperty("Content-Length", Integer.toString(jsonReq.getBytes().length));
        con.getOutputStream().write(jsonReq.getBytes());
        con.getOutputStream().flush();
        con.getOutputStream().close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String line=null;
        StringBuilder sb = new StringBuilder();
        while((line = in.readLine())!= null) {
            sb.append(line);
        }
        con.getInputStream().close();

        JSONObject jsonRet = new JSONObject(sb.toString());
        int status = jsonRet.getInt(APPLE_JSON_KEY_STATUS);
        boolean ret = false;
        if (status == 0) {
            JSONObject receiptJSON = jsonRet.getJSONObject(APPLE_JSON_KEY_RECEIPT);
            String bid=null;
            try {
                bid = receiptJSON.getString(APPLE_JSON_KEY_BUNDLE);
                ret = bid.trim().toLowerCase().equals(this.getAppId().trim().toLowerCase());
                if (ret) status = 0;
            } catch (JSONException e) {
                log.warn(APPLE_JSON_KEY_BUNDLE + " json field not found");
                ret = AppConfig.getProp(AppConfig.APP_IOS_VERIF_ENDPOINT).contains("sandbox");
                if (ret) status=0;
            }
            if (ret) {
                log.info("Receipt validated against Apple servers");
            } else {
                log.warn("Receipt status 0, but iOS app id not valid:" + bid);
            }
        } else {
            log.info("Apple receipt status:" + status);
        }
        return status;
    }

    /**
     * Performs a validation of the receipt to Apple servers
     * @param receipt
     * @return
     */
    protected boolean validateAppleReceipt(String receipt) throws Exception {
        log.info("Validating Apple Receipt");
        log.info(receipt);
        if (receipt.equals("NO-APP-RECEIPT")) return true;
        int status = validateAppeReceipt(receipt, AppConfig.getProp(AppConfig.APP_IOS_VERIF_ENDPOINT));
        System.out.println("Returned code: " + status);
        if (status == 21007) {
            // It means we have a receipt from a test environment
            status = validateAppeReceipt(receipt, AppConfig.getProp(AppConfig.APP_IOS_VERIF_TEST_ENDPOINT));
            System.out.println("Returned code: " + status);
        }
        return (status==0);
    }

    /**
     * Performs an antispam filter
     * @param request The request from the servlet
     * @return true if passes the filter. False otherwise
     * @throws Exception
     */
    protected boolean passFilter(HttpServletRequest request) throws ServletException {
        ResultSet rs=null;
        Connection conn=null;
        Statement stmt=null;
        try {
            String token = request.getHeader(ANTI_SPAM_HEADER);
            String tokenEnc = Util.createPasswordHash(this.getHashAlgorithm(),Util.BASE64_ENCODING,null,null, token);
            String query = String.format(SELECT_ANTISPAM, tokenEnc);
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(this.getDatasourceName());
            conn = ds.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery(query);
            boolean ret = rs.next();
            rs.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch(SQLException e) {}
            }
            if( stmt != null ) {
                try {
                    stmt.close();
                } catch(SQLException e) {}
            }
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ex) {}
            }
        }
    }

    /**
     * @return the hash algorithm used to store the generated passwords
     */
    protected String getHashAlgorithm() {
        String alg = getServletConfig().getInitParameter(CONF_PARAM_HASH_ALGORITHM);
        if (alg==null) return DEFAULT_HASH_ALGORITHM;
        return alg;
    }

    /**
     * @return The jndi name of the data source to store the tokens
     */
    protected String getDatasourceName() {
        String out = getServletConfig().getInitParameter(CONF_PARAM_DATASOURCE);
        return out;
    }

}


