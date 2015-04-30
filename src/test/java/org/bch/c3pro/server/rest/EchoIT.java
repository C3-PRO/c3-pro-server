package org.bch.c3pro.server.rest;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bch.c3pro.server.util.JSONPRequestFilter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
//import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.junit.Test;
//import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
//import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EchoIT {
	
	@Deployment
    public static Archive<?> createTestArchive() {
       //MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
       //         .loadMetadataFromPom("pom.xml");  
       return ShrinkWrap.create(WebArchive.class, "test.war")
    		    //.addAsLibraries(resolver.artifact("org.mockito:mockito-all:1.8.3").resolveAsFiles())
                .addClasses(Echo.class, EchoIT.class, JaxRsActivator.class, JSONPRequestFilter.class)
               //.addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
               //.setWebXML(new File("src/main/webapp/WEB-INF/web.xml"))
               .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
               .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-web.xml"))
               .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      }

    /*
    @Before
    public void setUp() throws Exception {
    }
    
    @After
    public void tearDown() throws Exception {
    }
    */
    // Requires credentials in JBoss for MedRec2:MedRecApp1_ in the role RestClient
	@Test
	public void getEchoIT() throws Exception {
		BufferedReader in;
		HttpURLConnection con;
		String response = "";
		URL url = new URL("http://127.0.0.1:8080/i2me2/rest/echo/getEcho/hola");
		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		String authentication = "MedRec2:MedRecApp1_";
		String encoding =  javax.xml.bind.DatatypeConverter.printBase64Binary(authentication.getBytes("UTF-8"));
		con.setRequestProperty("Authorization", "Basic " + encoding);
		assertEquals(HttpURLConnection.HTTP_OK, con.getResponseCode());
		in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		char[] cbuf = new char[200 + 1];			
		while (true) {		
			int numCharRead = in.read(cbuf, 0, 200);
			if (numCharRead == -1) {
					break;
			}
			String line = new String(cbuf, 0, numCharRead);
			response += line;
		}
		assertEquals(response.trim(), "{\"var\":\"Echo: hola\"}");
		System.out.println(response);
		in.close();
		con.disconnect();
	}
	
	@Test
	public void getEchoNoPermIT() throws Exception {
		HttpURLConnection con;
		URL url = new URL("http://127.0.0.1:8080/i2me2/rest/echo/getEcho/hola");
		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		String authentication = "MedRec:MedRe";
		String encoding =  javax.xml.bind.DatatypeConverter.printBase64Binary(authentication.getBytes("UTF-8"));
		con.setRequestProperty("Authorization", "Basic " + encoding);
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
		con.disconnect();
	}
	
	@Test
	public void getEchoNoPerm2IT() throws Exception {
		HttpURLConnection con;
		URL url = new URL("http://127.0.0.1:8080/i2me2/rest/echo/getEcho/hola");
		con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		String authentication = "MedRe:MedRecApp1_";
		String encoding =  javax.xml.bind.DatatypeConverter.printBase64Binary(authentication.getBytes("UTF-8"));
		con.setRequestProperty("Authorization", "Basic " + encoding);
		assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, con.getResponseCode());
		con.disconnect();
	}
}
