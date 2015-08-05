package org.bch.c3pro.server.servlet;

import org.bch.c3pro.server.util.Utils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: CH176656
 * Date: 8/5/15
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class USMap extends HttpServlet {
    public static final String BASE_MAP_FILE_NAME = "baseMap.svg";
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("image/svg-xml");
        StringBuffer sb = new StringBuffer();
        Utils.textFileToStringBuffer(USMap.class, BASE_MAP_FILE_NAME, sb, "\n");
        response.getWriter().println(sb.toString());
        return;
    }
}
