package org.bch.c3pro.server.util;

import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.external.S3Access;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple utilities to read files
 * Created by CH176656 on 3/26/2015.
 */
public class Utils {

    /**
     * Reads a text file under resource and appends it in a String Buffer
     * Example textFileToStringBuffer(FileUtils.class, "hello.txt", sb, "\n")
     * It will read the file "hello.txt" located where FileUtils.class is located
     * @param cl        The Class
     * @param fileName  The filename
     * @param sb        The StringBuffer
     * @param sep       The line sepparator
     * @throws Exception
     */
    public static final String [] US_STATES = {"AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN",
            "IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH",
            "OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY"};

    public static final String [] US_STATES_EXT = {
    "alabama",
    "alaska",
    "arizona",
    "arkansas",
    "california",
    "colorado",
    "connecticut",
    "delaware",
    "florida",
    "georgia",
    "hawaii",
    "idaho",
    "illinois",
    "indiana",
    "iowa",
    "kansas",
    "kentucky",
    "louisiana",
    "maine",
    "maryland",
    "massachusetts",
    "michigan",
    "minnesota",
    "mississippi",
    "missouri",
    "montana",
    "nebraska",
    "nevada",
    "new hampshire",
    "new jersey",
    "new mexico",
    "new york",
    "north carolina",
    "north dakota",
    "ohio",
    "oklahoma",
    "oregon",
    "pennsylvania",
    "rhode island",
    "south carolina",
    "south dakota",
    "tennessee",
    "texas",
    "utah",
    "vermont",
    "virginia",
    "washington",
    "west virginia",
    "wisconsin",
    "wyoming"
    };

    public static final Map<String, String> stateMapAbbr = new HashMap<>();

    public static final String TOTAL_LABEL = "TOTAL";

    public static void textFileToStringBuffer(Class cl, String fileName, StringBuffer sb, String sep)
throws IOException {
        InputStream in = cl.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line).append(sep);
            }
                    } finally {
            in.close();
        }
    }


    // We synchronized the method to avoid mis counting: Only one thread at a time can execute the method
    synchronized public static void updateMapInfo(String state, S3Access s3, int num) throws C3PROException {
        String filename = AppConfig.getProp(AppConfig.APP_FILENAME_MAPCOUNT);
        String jsonContent = s3.get(filename);
        String stateAbbr = state;
        if (state.length()>2) {
            // We need to find the abbreviation
            stateAbbr = Utils.getStateAbbr(state);
        }
        stateAbbr = stateAbbr.toUpperCase();
        try {
            JSONObject json = new JSONObject(jsonContent);
            if (json.has(stateAbbr)) {
                // update the count by the state
                long count = json.getLong(stateAbbr);
                count = count + num;
                json.remove(stateAbbr);
                json.put(stateAbbr, count);

                // update the total count
                long total = json.getLong(TOTAL_LABEL);
                total = total + num;
                json.remove(TOTAL_LABEL);
                json.put(TOTAL_LABEL, total);

                // update the bucket
                s3.put(filename, json.toString());
            } else {
                throw new C3PROException("State " + state + " is not a valid US state");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String generateURL(String protocol, String host, String port, String endpoint) {
        StringBuffer sb = new StringBuffer();
        sb.append(protocol);
        sb.append("://");
        sb.append(host);
        if (!port.trim().equals("")) {
            sb.append(":");
            sb.append(port);
        }
        sb.append(endpoint);
        return sb.toString();
    }

    public static Date subtractDays(Date date, int days) {
        Date dateWindow = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -days);
        dateWindow.setTime(c.getTime().getTime());
        return dateWindow;
    }

    public static String getPostContent(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line=null;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static void sendJSONError(HttpServletResponse response, String msg, int code) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write("{'error':'" + msg + "'}");
        out.flush();
        response.setStatus(code);
    }

    public static String getStateAbbr(String state) {
        String st = state.toLowerCase();
        if (Utils.stateMapAbbr.isEmpty()) {
            for (int i = 0; i<Utils.US_STATES.length; i++) {
                Utils.stateMapAbbr.put(Utils.US_STATES_EXT[i], Utils.US_STATES[i]);
            }
        }
        if (Utils.stateMapAbbr.containsKey(st)) {
            return Utils.stateMapAbbr.get(st);
        }
        return null;
    }
}
