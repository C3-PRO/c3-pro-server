package org.bch.c3pro.server.servlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.bch.c3pro.server.iresource.*;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * The fhir servlet to accept fhir rest methods
 * @author CHIP-IHL
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
	    resourceProviders.add(new QuestionnaireResponseResourceProvider());
        resourceProviders.add(new ConsentResourceProvider());
        resourceProviders.add(new ObservationResourceProvider());
        setResourceProviders(resourceProviders);
        setUseBrowserFriendlyContentTypes(true);
    }

}
