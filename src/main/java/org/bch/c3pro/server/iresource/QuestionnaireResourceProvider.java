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

    @Create()
    public MethodOutcome createQuestionnaire(@ResourceParam Questionnaire theQ) {
        String newId;
        String version = null;

        if (theQ.getId() == null) {
            newId = generateNewId();
        } else {
            if (theQ.getId().getIdPart()== null) {
                newId = generateNewId();
            } else {
                newId = theQ.getId().getIdPart();
            }
            if (theQ.getId().getVersionIdPart()!=null) {
                version = theQ.getId().getVersionIdPart();
            }
        }
        addNewVersion(theQ, newId, version);
        this.sendMessage(theQ);
        // Let the caller know the ID of the newly created resource
        this.putResource(theQ);
        return new MethodOutcome(new IdDt(newId));
    }

    private void addNewVersion(Questionnaire theQt, String theId, String version) {
        InstantDt publishedDate;
        if (!myIdToQVersions.containsKey(theId)) {
            myIdToQVersions.put(theId, new LinkedList<Questionnaire>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            Questionnaire currentQ = myIdToQVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentQ.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        theQt.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        theQt.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<Questionnaire> existingVersions = myIdToQVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion;
        if (version == null) {
            newVersion = Integer.toString(existingVersions.size());
        } else {
            newVersion = version;
        }

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("Questionnaire", theId, newVersion);
        theQt.setId(newId);
        existingVersions.add(theQt);
    }

    @Read(version = true)
    public Questionnaire readQuestionnaire(@IdParam IdDt theId) {
        Deque<Questionnaire> retVal;
        retVal = myIdToQVersions.get(theId.getIdPart());

        if (theId.hasVersionIdPart() == false) {
            Questionnaire theQ = (Questionnaire) getResource(theId.getIdPart());
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

    @Search
    public List<Questionnaire> findQuestionnairesUsingArbitraryCtriteria() {
        LinkedList<Questionnaire> retVal = new LinkedList<>();

        for (Deque<Questionnaire> nextQList : myIdToQVersions.values()) {
            Questionnaire nextQ = nextQList.getLast();
            retVal.add(nextQ);
        }

        return retVal;
    }
}
