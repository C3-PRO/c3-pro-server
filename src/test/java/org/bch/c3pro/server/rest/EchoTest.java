package org.bch.c3pro.server.rest;

import static org.junit.Assert.*;

import javax.ws.rs.core.Response;

import org.bch.c3pro.server.config.AppConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Demo: Unit test for Echo services
 * @author CH176656
 *
 */
public class EchoTest {
    
    
    /**
     * Test base cases of the method
     * @throws Exception
     */
    @Test
    public void echoGetTestBaseCase() throws Exception {
        Echo echo = new Echo();
        
        // We test empty string
        String send = "";
        Response resp = echo.getEcho(send, null);
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        Echo.ReturnDTO ret = (Echo.ReturnDTO) resp.getEntity();
        assertEquals("Echo: ",ret.getVar());
        
        // We test non-empty string
        send = "ping";
        resp = echo.getEcho(send, null);
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        ret = (Echo.ReturnDTO) resp.getEntity();
        assertEquals("Echo: " + send, ret.getVar());
        
        //Test bad request
        resp = echo.getEcho(null, null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

}
