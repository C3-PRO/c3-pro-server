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
 * The Questionnaire resource provider
 * @author CHIP-IHL
 */
public class QuestionnaireResourceProvider extends C3PROResourceProvider implements IResourceProvider  {

    private Map<String, Deque<Questionnaire>> myIdToQVersions = new HashMap<>();

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the resource type: Questionnaire
     * @return
     */
    @Override
    public Class<Questionnaire> getResourceType() {
        return Questionnaire.class;
    }

    @Override
    protected Class getResourceClass() {
        return Questionnaire.class;
    }

    /**
     * The Questionnaire GET handle
     * @param theId The id of the questionnaire
     * @return The Questionnaire
     */
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
