package org.bch.c3pro.server.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Calendar;
import java.util.Date;

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
}
