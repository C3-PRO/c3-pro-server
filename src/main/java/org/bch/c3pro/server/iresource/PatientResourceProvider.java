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
 * The Patient resource provider class
 * @author CHIP-IHL
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
     * The patient PUT handle
     * @param thePatient
     * @return
     */
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
     * Returns the resource type: Patient
     * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }


}
