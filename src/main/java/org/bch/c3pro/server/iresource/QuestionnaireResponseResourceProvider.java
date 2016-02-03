package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;

import java.util.*;

/**
 * Created by CH176656 on 4/30/2015.
 */
public class QuestionnaireResponseResourceProvider extends C3PROResourceProvider implements IResourceProvider  {

    private Map<String, Deque<QuestionnaireResponse>> myIdToQVersions = new HashMap<>();

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Class<QuestionnaireResponse> getResourceType() {
        return QuestionnaireResponse.class;
    }

    @Override
    protected Class getResourceClass() {
        return QuestionnaireResponse.class;
    }

    @Create()
    public MethodOutcome createQA(@ResourceParam QuestionnaireResponse theQR) {
        String newId = generateNewId();
        addNewVersion(theQR, newId);
        this.sendMessage(theQR);
        // Let the caller know the ID of the newly created resource
        return new MethodOutcome(new IdDt(newId));
    }

    private void addNewVersion(QuestionnaireResponse theQA, String theId) {
        InstantDt publishedDate;
        if (!myIdToQVersions.containsKey(theId)) {
            myIdToQVersions.put(theId, new LinkedList<QuestionnaireResponse>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            QuestionnaireResponse currentQA = myIdToQVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentQA.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        theQA.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<QuestionnaireResponse> existingVersions = myIdToQVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("QuestionnaireResponse", theId, newVersion);
        theQA.setId(newId);
        existingVersions.add(theQA);
    }
}
