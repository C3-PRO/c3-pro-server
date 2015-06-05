package org.bch.c3pro.server.servlet;

import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.Queue;
import org.bch.c3pro.server.external.SQSAccess;
import org.bch.c3pro.server.util.Utils;
import org.bch.security.oauth.server.ErrorReturn;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by CH176656 on 6/5/2015.
 *
 */
public class FHIREncServlet extends HttpServlet {
    private static String JSON_TAG_RESOURCE = "resource";
    private static String JSON_TAG_KEY = "key";
    private static String JSON_TAG_KEYID = "key_id";

    protected static Queue sqs = new SQSAccess();

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String post = Utils.getPostContent(request);
        String msg=null;
        String key = null;
        String keyId = null;
        try {
            JSONObject postJson = new JSONObject(post);
            msg = postJson.getString(JSON_TAG_RESOURCE);
            key = postJson.getString(JSON_TAG_KEY);
            keyId = postJson.getString(JSON_TAG_KEYID);
        } catch (JSONException e) {
            e.printStackTrace();
            Utils.sendJSONError(response, e.getMessage(),HttpServletResponse.SC_BAD_REQUEST );
            return;
        }
        try {
            this.sqs.sendMessageAlreadyEncrypted(msg, key, keyId);
        } catch (C3PROException e) {
            e.printStackTrace();
            Utils.sendJSONError(response, e.getMessage(),HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

    }

}
