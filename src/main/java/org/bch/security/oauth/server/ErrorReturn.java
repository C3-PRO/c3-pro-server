package org.bch.security.oauth.server;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by CH176656 on 6/2/2015.
 * JSON Error response class following <a href='http://tools.ietf.org/html/rfc6749#section-5.2'>RFC 6749</a>
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

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public void writeError(HttpServletResponse response, int errorCode) throws IOException {
        String message = this.toString();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(message);
        out.flush();
        response.setStatus(errorCode);
    }

}
