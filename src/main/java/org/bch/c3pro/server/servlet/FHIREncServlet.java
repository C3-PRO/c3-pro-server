package org.bch.c3pro.server.servlet;

import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.Queue;
import org.bch.c3pro.server.external.SQSAccess;
import org.bch.c3pro.server.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The servlet implementation to accept encrypted messages
 * @author CHIP-IHL
*/
public class FHIREncServlet extends HttpServlet {
    private static String JSON_TAG_RESOURCE = "message";
    private static String JSON_TAG_KEY = "symmetric_key";
    private static String JSON_TAG_KEYID = "key_id";
    private static String JSON_TAG_VERSION = "version";

    protected static Queue sqs = new SQSAccess();

    /**
     * The POST handle
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String post = Utils.getPostContent(request);
        String msg=null;
        String key = null;
        String keyId = null;
        String version = null;
        try {
            JSONObject postJson = new JSONObject(post);
            msg = postJson.getString(JSON_TAG_RESOURCE);
            key = postJson.getString(JSON_TAG_KEY);
            keyId = postJson.getString(JSON_TAG_KEYID);
            version = postJson.getString(JSON_TAG_VERSION);
        } catch (JSONException e) {
            e.printStackTrace();
            Utils.sendJSONError(response, e.getMessage(),HttpServletResponse.SC_BAD_REQUEST );
            return;
        }
        try {
            this.sqs.sendMessageAlreadyEncrypted(msg, key, keyId, version);
        } catch (C3PROException e) {
            e.printStackTrace();
            Utils.sendJSONError(response, e.getMessage(),HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

    }

}
