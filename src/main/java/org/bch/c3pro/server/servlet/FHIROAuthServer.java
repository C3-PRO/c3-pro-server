package org.bch.c3pro.server.servlet;

import org.bch.security.oauth.server.OAuth2Server;

/**
 * Created by CH176656 on 5/20/2015.
 */
public class FHIROAuthServer extends OAuth2Server {
    protected String getDatasourceName() {
        return "java:jboss/datasources/c3proAuthDS";
    }
}
