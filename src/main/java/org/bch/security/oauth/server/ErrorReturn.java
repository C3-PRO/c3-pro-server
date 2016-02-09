package org.bch.security.oauth.server;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class that handles json ERROR return messages accoring to
 * <a href='http://tools.ietf.org/html/rfc6749#section-5.2'>RFC 6749</a>
 * @author CHIP-IHL
 */
public class ErrorReturn {

    private static String JSON_TAG_ERROR = "error";
    private static String JSON_TAG_ERROR_DESC = "error_description";
    private static String JSON_TAG_ERROR_URI = "error_uri";

    public static enum ErrorType {
        ERROR_INVALID_REQUEST("invalid_request"),
        ERROR_INVALID_CLIENT("invalid_client"),
        ERROR_INVALID_GRANT("invalid_grant"),
        ERROR_UNAUTHORIZED_CLIENT("unauthorized_client"),
        ERROR_UNSUPPORTED_GRANT_TYPE("unsupported_grant_type");

        private final String text;
        ErrorType(String text) {
            this.text = text;
        }

        public String toString() {
            return this.text;
        }
    }

    // Required
    private ErrorType errorType=null;

    // Optional
    private String errorDesc=null;

    // optional
    private String errorUri = null;

    /**
     * Returns the json equivalent output
     * @return a json formatted String
     */
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_TAG_ERROR, this.errorType.toString());
            if (this.errorDesc != null) {
                json.put(JSON_TAG_ERROR_DESC, this.errorDesc);
            }
            if (this.errorUri != null) {
                json.put(JSON_TAG_ERROR_URI, this.errorUri);
            }
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Returns the error tye
     * @return the error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Sets the error type
     * @param errorType The new error type
     */
    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    /**
     * Returns error description
     * @return the error description
     */
    public String getErrorDesc() {
        return errorDesc;
    }

    /**
     * Sets the error description
     * @param errorDesc The new error description
     */
    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    /**
     * Write the error message to the http servlet response
     * @param response  The http servlet response
     * @param errorCode The http status code
     * @throws IOException In case of I/O error
     */
    public void writeError(HttpServletResponse response, int errorCode) throws IOException {
        String message = this.toString();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        response.setStatus(errorCode);
        out.write(message);
        out.flush();
    }

}
