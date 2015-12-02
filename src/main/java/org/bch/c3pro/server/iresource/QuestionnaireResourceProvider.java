package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.Questionnaire;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.*;

/**
 * Created by CH176656 on 4/30/2015.
 */
public class QuestionnaireResourceProvider extends C3PROResourceProvider implements IResourceProvider  {

    private Map<String, Deque<Questionnaire>> myIdToQVersions = new HashMap<>();

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Class<Questionnaire> getResourceType() {
        return Questionnaire.class;
    }

    @Override
    protected Class getResourceClass() {
        return Questionnaire.class;
    }

    @Read(version = true)
    public Questionnaire readQuestionnaire(@IdParam IdDt theId) {
        Deque<Questionnaire> retVal;
        retVal = myIdToQVersions.get(theId.getIdPart());

        if (theId.hasVersionIdPart() == false) {
            Questionnaire theQ = (Questionnaire) getResource(theId.getIdPart());
            if (theQ==null) {
                throw new ResourceNotFoundException("Questionnaire " + theId.getIdPart() + " not found");
            }
            return theQ;
            //return retVal.getLast();
        } else {
            for (Questionnaire nextVersion : retVal) {
                String nextVersionId = nextVersion.getId().getVersionIdPart();
                if (theId.getVersionIdPart().equals(nextVersionId)) {
                    return nextVersion;
                }
            }
            // No matching version
            throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
        }

    }

}
