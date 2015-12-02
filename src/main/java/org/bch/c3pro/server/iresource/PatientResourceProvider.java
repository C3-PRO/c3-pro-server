package org.bch.c3pro.server.iresource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.resource.OperationOutcome;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bch.c3pro.server.exception.C3PROException;
import org.bch.c3pro.server.util.Utils;

import java.util.*;

/**
 * Created by CH176656 on 4/30/2015.
 */
public class PatientResourceProvider extends C3PROResourceProvider implements IResourceProvider {

    /**
     * This map has a resource ID as a key, and each key maps to a Deque list containing all versions of the resource with that ID.
     */
    private Map<String, Deque<Patient>> myIdToPatientVersions = new HashMap<>();

    /**
     * This is used to generate new IDs
     */
    private long myNextId = 1;

    @Override
    protected String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected Class getResourceClass() {
        return Patient.class;
    }


    public PatientResourceProvider() {
    }

    @Update()
    public MethodOutcome updatePatient(@ResourceParam Patient thePatient) {
        this.sendMessage(thePatient);
        if (thePatient.getAddress()!=null) {
            if (!thePatient.getAddress().isEmpty()) {
                // We get just the first adress
                AddressDt address = thePatient.getAddress().get(0);
                if (address.getState()!=null) {
                    try {
                        Utils.updateMapInfo(address.getState(), this.s3, 1);
                    } catch (C3PROException e) {
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        // Let the caller know the ID of the newly created resource
        return new MethodOutcome();
    }

    /**
     * Stores a new version of the patient in memory so that it can be retrieved later.
     *
     * @param thePatient
     *            The patient resource to store
     * @param theId
     *            The ID of the patient to retrieve
     */
    private void addNewVersion(Patient thePatient, String theId) {
        InstantDt publishedDate;
        if (!myIdToPatientVersions.containsKey(theId)) {
            myIdToPatientVersions.put(theId, new LinkedList<Patient>());
            publishedDate = InstantDt.withCurrentTime();
        } else {
            Patient currentPatient = myIdToPatientVersions.get(theId).getLast();
            Map<ResourceMetadataKeyEnum<?>, Object> resourceMetadata = currentPatient.getResourceMetadata();
            publishedDate = (InstantDt) resourceMetadata.get(ResourceMetadataKeyEnum.PUBLISHED);
        }

		/*
		 * PUBLISHED time will always be set to the time that the first version was stored. UPDATED time is set to the time that the new version was stored.
		 */
        thePatient.getResourceMetadata().put(ResourceMetadataKeyEnum.PUBLISHED, publishedDate);
        thePatient.getResourceMetadata().put(ResourceMetadataKeyEnum.UPDATED, InstantDt.withCurrentTime());

        Deque<Patient> existingVersions = myIdToPatientVersions.get(theId);

        // We just use the current number of versions as the next version number
        String newVersion = Integer.toString(existingVersions.size());

        // Create an ID with the new version and assign it back to the resource
        IdDt newId = new IdDt("Patient", theId, newVersion);
        thePatient.setId(newId);

        existingVersions.add(thePatient);
    }

    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }


}
