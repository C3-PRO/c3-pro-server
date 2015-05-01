package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Questionnaire;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bch.c3pro.server.external.SQSAccess;

import java.util.*;

/**
 * Created by CH176656 on 4/30/2015.
 */
public class QuestionnaireResourceProvider implements IResourceProvider  {

    private Map<Long, Deque<Questionnaire>> myIdToQVersions = new HashMap<>();
    private SQSAccess sqs = new SQSAccess();
    /**
     * This is used to generate new IDs
     */
    private long myNextId = 1;

    @Override
    public Class<Questionnaire> getResourceType() {
        return Questionnaire.class;
    }

    @Create()
    public MethodOutcome createPatient(@ResourceParam Questionnaire theQ) {

        // Here we are just generating IDs sequentially
        long id = myNextId++;
        addNewVersion(theQ, id);
        try {
            sqs.sendMessage(theQ.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Let the caller know the ID of the newly created resource
        return new MethodOutcome(new IdDt(id));
    }

    private void addNewVersion(Questionnaire thePatient, Long theId) {
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
        thePatient.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        thePatient.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<Questionnaire> existingVersions = myIdToQVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("Questionnaire", Long.toString(theId), newVersion);
        thePatient.setId(newId);
        existingVersions.add(thePatient);
    }

    @Read(version = true)
    public Questionnaire readPatient(@IdParam IdDt theId) {
        Deque<Questionnaire> retVal;
        try {
            retVal = myIdToQVersions.get(theId.getIdPartAsLong());
        } catch (NumberFormatException e) {
			/*
			 * If we can't parse the ID as a long, it's not valid so this is an unknown resource
			 */
            throw new ResourceNotFoundException(theId);
        }

        if (theId.hasVersionIdPart() == false) {
            return retVal.getLast();
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
    public List<Questionnaire> findPatientsUsingArbitraryCtriteria() {
        LinkedList<Questionnaire> retVal = new LinkedList<>();

        for (Deque<Questionnaire> nextQList : myIdToQVersions.values()) {
            Questionnaire nextQ = nextQList.getLast();
            retVal.add(nextQ);
        }

        return retVal;
    }
}
