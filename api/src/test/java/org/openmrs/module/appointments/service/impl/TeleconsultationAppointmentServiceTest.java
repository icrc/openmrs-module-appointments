package org.openmrs.module.appointments.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.AdhocTeleconsultationResponse;
import org.openmrs.module.appointments.model.Appointment;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@PowerMockIgnore("javax.management.*")
@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class TeleconsultationAppointmentServiceTest {
    @Mock
    private AdministrationService administrationService;

    @Mock
    private PatientService patientService;

    @Mock
    private PatientAppointmentNotifierService patientAppointmentNotifierService;

    @InjectMocks
    private TeleconsultationAppointmentService teleconsultationAppointmentService;

    private Patient patient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        patient = new Patient();
        patient.setUuid("patientUuid");
        PersonName name = new PersonName();
        name.setGivenName("test patient");
        Set<PersonName> personNames = new HashSet<>();
        personNames.add(name);
        patient.setNames(personNames);
        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setIdentifier("GAN230901");
        PatientIdentifierType patientIdentifierType = new PatientIdentifierType();
        patientIdentifierType.setName("Patient Identifier");
        identifier.setIdentifierType(patientIdentifierType);
        patient.setIdentifiers(new HashSet<>(Arrays.asList(identifier)));
        when(patientService.getPatientByUuid("patientUuid")).thenReturn(patient);
        PowerMockito.mockStatic(Context.class);
        when(Context.getAdministrationService()).thenReturn(administrationService);
        when(administrationService.getGlobalProperty("bahmni.appointment.teleConsultation.serverUrlPattern")).thenReturn("https://test.server/{0}");
        when(administrationService.getGlobalProperty("bahmni.adhoc.teleConsultation.id")).thenReturn("Patient Identifier");
    }

    @Test
    public void shouldGenerateTCLinkForAppointment() {
        Appointment appointment = new Appointment();
        UUID uuid = UUID.randomUUID();
        appointment.setUuid(uuid.toString());
        String link = teleconsultationAppointmentService.generateTeleconsultationLink(appointment.getUuid());
        assertEquals("https://test.server/"+appointment.getUuid(), link);

    }

    @Test
    public void shouldGetAdhocTCLinkBasedOnPatientID() {
        AdhocTeleconsultationResponse response = teleconsultationAppointmentService.generateAdhocTeleconsultationLink(patient.getUuid(), "");
        assertEquals("https://test.server/GAN230901", response.getLink());
    }

    @Test
    public void shouldPublishAdhocTeleconsultationEvent() {
        AdhocTeleconsultationResponse response = teleconsultationAppointmentService.generateAdhocTeleconsultationLink(patient.getUuid(), "");
        assertEquals("https://test.server/GAN230901", response.getLink());
        verify(patientAppointmentNotifierService, times(1)).notifyAll(patient, "", response.getLink());
    }
}
