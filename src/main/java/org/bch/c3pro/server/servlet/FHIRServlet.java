package org.bch.c3pro.server.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.bch.c3pro.server.iresource.PatientResourceProvider;
import org.bch.c3pro.server.iresource.QuestionnaireResourceProvider;

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
        setFhirContext(FhirContext.forDstu2());

        List<IResourceProvider> resourceProviders = new ArrayList<>();
        resourceProviders.add(new PatientResourceProvider());
        resourceProviders.add(new QuestionnaireResourceProvider());
        setResourceProviders(resourceProviders);
        setUseBrowserFriendlyContentTypes(true);
    }

}
