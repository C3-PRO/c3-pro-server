package org.bch.c3pro.server.servlet;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.bch.c3pro.server.iresource.PatientResourceProvider;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by CH176656 on 4/30/2015.
 */
public class FHIRServlet extends RestfulServer {
    private static final long serialVersionUID = 1L;

    @Override
    protected void initialize() throws ServletException {
      /*
       * The servlet defines any number of resource providers, and
       * configures itself to use them by calling
       * setResourceProviders()
       */
        List<IResourceProvider> resourceProviders = new ArrayList<>();
        resourceProviders.add(new PatientResourceProvider());
        setResourceProviders(resourceProviders);
        setUseBrowserFriendlyContentTypes(true);
    }

}
