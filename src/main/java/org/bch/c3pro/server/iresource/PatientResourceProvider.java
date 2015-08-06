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

    /**
     * The "@Create" annotation indicates that this method implements "create=type", which adds a
     * new instance of a resource to the server.
     */
    @Create()
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        String newId = generateNewId();
        addNewVersion(thePatient, newId);
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
        return new MethodOutcome(new IdDt(newId));
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

    @Search
    public List<Patient> findPatientsUsingArbitraryCtriteria() {
        LinkedList<Patient> retVal = new LinkedList<Patient>();

        for (Deque<Patient> nextPatientList : myIdToPatientVersions.values()) {
            Patient nextPatient = nextPatientList.getLast();
            retVal.add(nextPatient);
        }

        return retVal;
    }


    /**
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    /**
     * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
     * <p>
     * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
     * </p>
     *
     * @param theId
     *            The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
     * @return Returns a resource matching this identifier, or null if none exists.
     */
    @Read(version = true)
    public Patient readPatient(@IdParam IdDt theId) {
        Deque<Patient> retVal;
        retVal = myIdToPatientVersions.get(theId.getIdPart());

        if (theId.hasVersionIdPart() == false) {
            return retVal.getLast();
        } else {
            for (Patient nextVersion : retVal) {
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
