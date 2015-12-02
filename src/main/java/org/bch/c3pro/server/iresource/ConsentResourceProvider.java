package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

import java.util.*;

/**
 * Created by CH176656 on 5/7/2015.
 */
public class ConsentResourceProvider extends C3PROResourceProvider implements IResourceProvider {

    private Map<String, Deque<Contract>> myIdToContractVersions = new HashMap<>();

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Class<Contract> getResourceType() {
        return Contract.class;
    }

    @Override
    protected Class getResourceClass() {
        return Contract.class;
    }

    @Create()
    public MethodOutcome createContract(@ResourceParam Contract theContract) {
        String newId = generateNewId();

        addNewVersion(theContract, newId);
        this.sendMessage(theContract);
        // Let the caller know the ID of the newly created resource
        this.putResource(theContract);
        return new MethodOutcome(new IdDt(newId));
    }

    private void addNewVersion(Contract theContract, String theId) {
        InstantDt publishedDate;
        if (!myIdToContractVersions.containsKey(theId)) {
            myIdToContractVersions.put(theId, new LinkedList<Contract>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            Contract currentQ = myIdToContractVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentQ.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        theContract.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        theContract.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<Contract> existingVersions = myIdToContractVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("Contract", theId, newVersion);
        theContract.setId(newId);
        existingVersions.add(theContract);
    }

}
