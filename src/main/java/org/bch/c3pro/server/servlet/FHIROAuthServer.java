package org.bch.c3pro.server.servlet;

import org.bch.security.oauth.server.OAuth2Server;

/**
 * The Oauth fhir server class. See {@link OAuth2Server}
 * @author CHIP-IHL
 */
public class FHIROAuthServer extends OAuth2Server {
    protected String getDatasourceName() {
        return "java:jboss/datasources/c3proAuthDS";
    }
}
