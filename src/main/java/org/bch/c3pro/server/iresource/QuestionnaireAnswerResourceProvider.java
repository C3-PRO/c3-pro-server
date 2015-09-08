package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireAnswers;
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
public class QuestionnaireAnswerResourceProvider extends C3PROResourceProvider implements IResourceProvider  {

    private Map<String, Deque<QuestionnaireAnswers>> myIdToQVersions = new HashMap<>();

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Class<QuestionnaireAnswers> getResourceType() {
        return QuestionnaireAnswers.class;
    }

    @Override
    protected Class getResourceClass() {
        return QuestionnaireAnswers.class;
    }

    @Create()
    public MethodOutcome createQA(@ResourceParam QuestionnaireAnswers theQA) {
        String newId = generateNewId();
        addNewVersion(theQA, newId);
        this.sendMessage(theQA);
        // Let the caller know the ID of the newly created resource
        return new MethodOutcome(new IdDt(newId));
    }

    private void addNewVersion(QuestionnaireAnswers theQA, String theId) {
        InstantDt publishedDate;
        if (!myIdToQVersions.containsKey(theId)) {
            myIdToQVersions.put(theId, new LinkedList<QuestionnaireAnswers>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            QuestionnaireAnswers currentQA = myIdToQVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentQA.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<QuestionnaireAnswers> existingVersions = myIdToQVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("QuestionnaireAnswers", theId, newVersion);
        theQA.setId(newId);
        existingVersions.add(theQA);
    }
/*
    @Read(version = true)
    public QuestionnaireAnswers readQA(@IdParam IdDt theId) {
        Deque<QuestionnaireAnswers> retVal;
        retVal = myIdToQVersions.get(theId.getIdPart());

        if (theId.hasVersionIdPart() == false) {
            return retVal.getLast();
        } else {
            for (QuestionnaireAnswers nextVersion : retVal) {
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
    public List<QuestionnaireAnswers> findQAUsingArbitraryCtriteria() {
        LinkedList<QuestionnaireAnswers> retVal = new LinkedList<>();

        for (Deque<QuestionnaireAnswers> nextQList : myIdToQVersions.values()) {
            QuestionnaireAnswers nextQA = nextQList.getLast();
            retVal.add(nextQA);
        }

        return retVal;
    }
    */
}
