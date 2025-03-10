package org.openmrs.module.appointments.web.mapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.*;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.appointments.model.*;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.util.DateUtil;
import org.openmrs.module.appointments.web.contract.*;
import org.openmrs.module.appointments.web.extension.AppointmentResponseExtension;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class AppointmentMapperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private PatientService patientService;

    @Mock
    private LocationService locationService;

    @Mock
    private ProviderService providerService;

    @Mock
    private AppointmentServiceDefinitionService appointmentServiceDefinitionService;

    @Mock
    private AppointmentServiceMapper appointmentServiceMapper;

    @Mock
    private AppointmentsService appointmentsService;

    @Mock
    private AppointmentResponseExtension extension;

    @InjectMocks
    private AppointmentMapper appointmentMapper;

    private Patient patient;
    private AppointmentServiceDefinition service;
    private AppointmentServiceType serviceType;
    private AppointmentServiceType serviceType2;
    private Provider provider;
    private Location location;

    private AppointmentServiceDefinition service2;

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
        PatientIdentifierType patientIdentifierType=new PatientIdentifierType(1);
        patientIdentifierType.setName("PatientId");
        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setIdentifier("GAN230901");
        identifier.setIdentifierType(patientIdentifierType);
        identifier.setPreferred(true);
//        A patient can have 2 identifiers from the same type
        PatientIdentifier secondIdentifier = new PatientIdentifier();
        secondIdentifier.setIdentifier("GAN230901FromMerge");
        secondIdentifier.setIdentifierType(patientIdentifierType);
        patient.setIdentifiers(new HashSet<>(Arrays.asList(secondIdentifier,identifier)));
        when(patientService.getPatientByUuid("patientUuid")).thenReturn(patient);
        service = new AppointmentServiceDefinition();
        service.setUuid("serviceUuid");

        service2 = new AppointmentServiceDefinition();
        service2.setUuid("service2Uuid");

        Speciality speciality = new Speciality();
        speciality.setName("Cardiology");
        speciality.setUuid("specialityUuid");
        service.setSpeciality(speciality);
        Set<AppointmentServiceType> serviceTypes = new LinkedHashSet<>();
        serviceType = new AppointmentServiceType();
        serviceType2 = new AppointmentServiceType();
        serviceType.setName("Type1");
        serviceType.setUuid("serviceTypeUuid");
        serviceType.setDuration(10);
        serviceType2.setName("Type2");
        serviceType2.setUuid("serviceType2Uuid");
        serviceType2.setDuration(20);
        serviceType2.setVoided(true);
        serviceTypes.add(serviceType);
        serviceTypes.add(serviceType2);
        service.setServiceTypes(serviceTypes);
        when(appointmentServiceDefinitionService.getAppointmentServiceByUuid("serviceUuid")).thenReturn(service);
        provider = new Provider();
        provider.setUuid("providerUuid");
        when(providerService.getProviderByUuid("providerUuid")).thenReturn(provider);
        location = new Location();
        location.setUuid("locationUuid");
        when(locationService.getLocationByUuid("locationUuid")).thenReturn(location);
    }

    @Test
    public void shouldGetAppointmentFromPayload() throws Exception {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        assertNotNull(appointment);
        verify(patientService, times(1)).getPatientByUuid(appointmentRequest.getPatientUuid());
        assertEquals(patient, appointment.getPatient());
        verify(appointmentServiceDefinitionService, times(1)).getAppointmentServiceByUuid(appointmentRequest.getServiceUuid());
        assertEquals(service, appointment.getService());
        assertEquals(serviceType, appointment.getServiceType());
        verify(providerService, times(1)).getProviderByUuid(appointmentRequest.getProviderUuid());
        assertEquals(provider, appointment.getProviders().iterator().next().getProvider());
        verify(locationService, times(1)).getLocationByUuid(appointmentRequest.getLocationUuid());
        assertEquals(location, appointment.getLocation());
        assertEquals(appointmentRequest.getStartDateTime(), appointment.getStartDateTime());
        assertEquals(appointmentRequest.getEndDateTime(), appointment.getEndDateTime());
        assertEquals(AppointmentKind.valueOf(appointmentRequest.getAppointmentKind()), appointment.getAppointmentKind());
        assertEquals(AppointmentStatus.Scheduled, appointment.getStatus());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());
    }

    @Test
    public void shouldNotSearchForExistingAppointmentWhenPayLoadUuidIsNull() throws Exception {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(null);
        appointmentMapper.fromRequest(appointmentRequest);
        verify(appointmentsService, never()).getAppointmentByUuid(anyString());
    }

    @Test
    public void shouldGetExistingAppointmentFromPayload() throws Exception {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(appointmentUuid);
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        verify(appointmentsService, times(1)).getAppointmentByUuid(appointmentUuid);
        assertNotNull(appointment);
        assertEquals(appointmentUuid, appointment.getUuid());
        assertEquals(service, appointment.getService());
        assertEquals(serviceType, appointment.getServiceType());
        assertEquals(provider, appointment.getProviders().iterator().next().getProvider());
        assertEquals(location, appointment.getLocation());
        assertEquals(appointmentRequest.getStartDateTime(), appointment.getStartDateTime());
        assertEquals(appointmentRequest.getEndDateTime(), appointment.getEndDateTime());
        assertEquals(AppointmentKind.valueOf(appointmentRequest.getAppointmentKind()), appointment.getAppointmentKind());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());
    }

    @Test
    public void shouldCreateAppointmentWithStatusFromPayload() throws ParseException {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setStatus("CheckedIn");

        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        existingAppointment.setStatus(AppointmentStatus.CheckedIn);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);

        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        assertNotNull(appointment);
        assertEquals(AppointmentStatus.CheckedIn, appointment.getStatus());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());

    }

    @Test
    public void shouldMapExistingAppointmentWhenPayloadHasAppointmentStatus() throws ParseException {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setStatus("Requested");
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        assertNotNull(appointment);
        assertEquals(AppointmentStatus.Requested, appointment.getStatus());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());

    }

    @Test
    public void shouldGetExistingAppointmentBookedAgainstVoidedServiceTypeFromPayload() throws Exception {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        existingAppointment.setServiceType(serviceType2);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(appointmentUuid);
        appointmentRequest.setServiceTypeUuid(serviceType2.getUuid());
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        verify(appointmentsService, times(1)).getAppointmentByUuid(appointmentUuid);
        assertNotNull(appointment);
        assertEquals(appointmentUuid, appointment.getUuid());
        assertEquals(service, appointment.getService());
        assertEquals(serviceType2, appointment.getServiceType());
        assertEquals(provider, appointment.getProviders().iterator().next().getProvider());
        assertEquals(location, appointment.getLocation());
        assertEquals(appointmentRequest.getStartDateTime(), appointment.getStartDateTime());
        assertEquals(appointmentRequest.getEndDateTime(), appointment.getEndDateTime());
        assertEquals(AppointmentKind.valueOf(appointmentRequest.getAppointmentKind()), appointment.getAppointmentKind());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());
    }

    @Test
    public void shouldSetServiceTypeAsNullWhenServiceTypeIsNotThereInPayload() throws Exception {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        existingAppointment.setServiceType(serviceType2);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(appointmentUuid);
        appointmentRequest.setServiceTypeUuid(null);
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        verify(appointmentsService, times(1)).getAppointmentByUuid(appointmentUuid);
        assertNotNull(appointment);
        assertEquals(existingAppointment.getUuid(), appointment.getUuid());
        assertNull(appointment.getServiceType());
    }

    @Test
    public void shouldNotChangePatientOnEditAppointment() throws Exception {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(appointmentUuid);
        appointmentRequest.setPatientUuid("newPatientUuid");
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);
        verify(patientService, never()).getPatientByUuid(appointmentRequest.getPatientUuid());
        assertEquals(existingAppointment.getPatient(), appointment.getPatient());
    }

    @Test
    public void shouldCreateDefaultResponse() throws Exception {
        Appointment appointment = createAppointment();
        List<Appointment> appointmentList = new ArrayList<>();
        appointmentList.add(appointment);
        Map additionalInfo = new HashMap();
        additionalInfo.put("Program Name", "Tuberculosis");
        additionalInfo.put("Last Visit", "02/09/2016");

        AppointmentServiceDefaultResponse serviceDefaultResponse = new AppointmentServiceDefaultResponse();
        when(appointmentServiceMapper.constructDefaultResponse(service)).thenReturn(serviceDefaultResponse);
        when(extension.run(appointment)).thenReturn(additionalInfo);
        List<AppointmentDefaultResponse> appointmentDefaultResponse = appointmentMapper.constructResponse(appointmentList);
        AppointmentDefaultResponse response = appointmentDefaultResponse.get(0);
        assertEquals(appointment.getUuid(), response.getUuid());
        assertEquals(appointment.getAppointmentNumber(), response.getAppointmentNumber());
        assertEquals(appointment.getPatient().getPersonName().getFullName(), response.getPatient().get("name"));
        assertEquals(appointment.getPatient().getUuid(), response.getPatient().get("uuid"));
        assertEquals(appointment.getPatient().getPatientIdentifier().getIdentifier(), response.getPatient().get("identifier"));
        assertEquals(serviceDefaultResponse, response.getService());
        assertEquals(appointment.getServiceType().getName(), response.getServiceType().get("name"));
        assertEquals(appointment.getServiceType().getUuid(), response.getServiceType().get("uuid"));
        assertEquals(appointment.getServiceType().getDuration(), response.getServiceType().get("duration"));
        assertEquals(appointment.getProviders().iterator().next().getProvider().getName(), response.getProviders().iterator().next().getName());
        assertEquals(appointment.getProviders().iterator().next().getProvider().getUuid(), response.getProviders().iterator().next().getUuid());
        assertEquals(appointment.getLocation().getName(), response.getLocation().get("name"));
        assertEquals(appointment.getLocation().getUuid(), response.getLocation().get("uuid"));
        assertEquals(appointment.getStartDateTime(), response.getStartDateTime());
        assertEquals(appointment.getEndDateTime(), response.getEndDateTime());
        assertEquals(appointment.getAppointmentKind(), AppointmentKind.valueOf(response.getAppointmentKind()));
        assertEquals(appointment.getStatus(), AppointmentStatus.valueOf(response.getStatus()));
        assertEquals(appointment.getComments(), response.getComments());
        assertEquals(appointment.getVoided(), response.getVoided());
        verify(extension, times(1)).run(appointment);
        assertEquals(2, response.getAdditionalInfo().keySet().size());
        assertEquals(false, response.getExtensions().get("patientEmailDefined"));
    }


    @Test
    public void shouldCreateDefaultResponseForSingleAppointment() throws Exception {
        Appointment appointment = createAppointment();
        AppointmentServiceDefaultResponse serviceDefaultResponse = new AppointmentServiceDefaultResponse();
        when(appointmentServiceMapper.constructDefaultResponse(service)).thenReturn(serviceDefaultResponse);
        AppointmentDefaultResponse response = appointmentMapper.constructResponse(appointment);
        assertEquals(appointment.getUuid(), response.getUuid());
        assertEquals(appointment.getAppointmentNumber(), response.getAppointmentNumber());
        assertEquals(appointment.getPatient().getPersonName().getFullName(), response.getPatient().get("name"));
        assertEquals(appointment.getPatient().getUuid(), response.getPatient().get("uuid"));
        Set<PatientIdentifierType> patientIdentifiers = appointment.getPatient().getActiveIdentifiers().stream().map(PatientIdentifier::getIdentifierType).collect(Collectors.toSet());
        for (PatientIdentifierType type:patientIdentifiers){
            assertEquals(appointment.getPatient().getPatientIdentifier(type).getIdentifier(), response.getPatient().get(type.getName()));

        }
        assertEquals(serviceDefaultResponse, response.getService());
        assertEquals(appointment.getServiceType().getName(), response.getServiceType().get("name"));
        assertEquals(appointment.getServiceType().getUuid(), response.getServiceType().get("uuid"));
        assertEquals(appointment.getServiceType().getDuration(), response.getServiceType().get("duration"));
        assertEquals(appointment.getProviders().iterator().next().getProvider().getName(), response.getProviders().iterator().next().getName());
        assertEquals(appointment.getProviders().iterator().next().getProvider().getUuid(), response.getProviders().iterator().next().getUuid());
        assertEquals(appointment.getLocation().getName(), response.getLocation().get("name"));
        assertEquals(appointment.getLocation().getUuid(), response.getLocation().get("uuid"));
        assertEquals(appointment.getStartDateTime(), response.getStartDateTime());
        assertEquals(appointment.getEndDateTime(), response.getEndDateTime());
        assertEquals(appointment.getAppointmentKind(), AppointmentKind.valueOf(response.getAppointmentKind()));
        assertEquals(appointment.getStatus(), AppointmentStatus.valueOf(response.getStatus()));
        assertEquals(appointment.getComments(), response.getComments());
        assertEquals(appointment.getVoided(), response.getVoided());
    }

    @Test
    public void shouldCreateDefaultResponseWhenNoExtension() throws Exception {
        Appointment appointment = createAppointment();
        appointmentMapper.appointmentResponseExtension = null;
        AppointmentServiceDefaultResponse serviceDefaultResponse = new AppointmentServiceDefaultResponse();
        when(appointmentServiceMapper.constructDefaultResponse(service)).thenReturn(serviceDefaultResponse);
        AppointmentDefaultResponse response = appointmentMapper.constructResponse(appointment);
        assertEquals(appointment.getUuid(), response.getUuid());
        assertNull(response.getAdditionalInfo());
    }

    @Test
    public void shouldReturnNullIfNoProviderInDefaultResponse() throws Exception {
        Appointment appointment = createAppointment();
        appointment.getService().setSpeciality(null);
        appointment.setServiceType(null);
        appointment.setProvider(null);
        appointment.setLocation(null);
        List<Appointment> appointmentList = new ArrayList<>();
        appointmentList.add(appointment);


        AppointmentServiceDefaultResponse serviceDefaultResponse = new AppointmentServiceDefaultResponse();
        when(appointmentServiceMapper.constructDefaultResponse(service)).thenReturn(serviceDefaultResponse);

        List<AppointmentDefaultResponse> appointmentDefaultResponse = appointmentMapper.constructResponse(appointmentList);
        AppointmentDefaultResponse response = appointmentDefaultResponse.get(0);
        assertEquals(appointment.getUuid(), response.getUuid());
        assertNull(response.getAppointmentNumber());
        assertEquals(appointment.getPatient().getPersonName().getFullName(), response.getPatient().get("name"));
        assertEquals(appointment.getPatient().getUuid(), response.getPatient().get("uuid"));
        assertEquals(serviceDefaultResponse, response.getService());

        assertNull(response.getServiceType());
        assertNull(response.getProvider());
        assertNull(response.getLocation());
        assertEquals(appointment.getStartDateTime(), response.getStartDateTime());
        assertEquals(appointment.getEndDateTime(), response.getEndDateTime());
        assertEquals(appointment.getAppointmentKind(), AppointmentKind.valueOf(response.getAppointmentKind()));
        assertEquals(appointment.getStatus(), AppointmentStatus.valueOf(response.getStatus()));
        assertEquals(appointment.getComments(), response.getComments());
        assertEquals(appointment.getVoided(), response.getVoided());
    }

    private AppointmentRequest createAppointmentRequest() throws ParseException {
        AppointmentRequest appointmentRequest = new AppointmentRequest();
        appointmentRequest.setPatientUuid("patientUuid");
        appointmentRequest.setServiceUuid("serviceUuid");
        appointmentRequest.setServiceTypeUuid("serviceTypeUuid");
        appointmentRequest.setLocationUuid("locationUuid");
        appointmentRequest.setProviderUuid("providerUuid");
        String startDateTime = "2017-03-15T16:57:09.0Z";
        Date startDate = DateUtil.convertToDate(startDateTime, DateUtil.DateFormatType.UTC);
        appointmentRequest.setStartDateTime(startDate);
        String endDateTime = "2017-03-15T17:57:09.0Z";
        Date endDate = DateUtil.convertToDate(endDateTime, DateUtil.DateFormatType.UTC);
        appointmentRequest.setEndDateTime(endDate);
        appointmentRequest.setAppointmentKind("Scheduled");
        appointmentRequest.setComments("Initial Consultation");

        List<AppointmentProviderDetail> providerDetails = new ArrayList<>();
        AppointmentProviderDetail providerDetail = new AppointmentProviderDetail();
        providerDetail.setUuid("providerUuid");
        providerDetail.setResponse("ACCEPTED");
        providerDetails.add(providerDetail);
        appointmentRequest.setProviders(providerDetails);

        return appointmentRequest;
    }

    private Appointment createAppointment() throws ParseException {
        Appointment appointment = new Appointment();
        PersonName name = new PersonName();
        name.setGivenName("test patient");
        Set<PersonName> personNames = new HashSet<>();
        personNames.add(name);
        Patient patient = new Patient();
        patient.setNames(personNames);

        PatientIdentifierType patientIdentifierType=new PatientIdentifierType(1);
        patientIdentifierType.setName("PatientId");

        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setIdentifier("GAN230901");
        identifier.setPreferred(true);
        identifier.setVoided(false);
        identifier.setIdentifierType(patientIdentifierType);
        //        A patient can have 2 identifiers from the same type
        PatientIdentifier secondIdentifier = new PatientIdentifier();
        secondIdentifier.setIdentifier("GAN230901FromMerge");
        secondIdentifier.setPreferred(false);
        secondIdentifier.setVoided(false);
        secondIdentifier.setIdentifierType(patientIdentifierType);


        PatientIdentifierType cardIdType=new PatientIdentifierType(2);
        cardIdType.setName("CardId");

        PatientIdentifier cardIdIdentifier = new PatientIdentifier();
        cardIdIdentifier.setIdentifier("CardId");
        cardIdIdentifier.setIdentifierType(cardIdType);
        cardIdIdentifier.setVoided(false);
        cardIdIdentifier.setPreferred(true);

        PatientIdentifier cardIdIdentifierVoided = new PatientIdentifier();
        cardIdIdentifierVoided.setIdentifier("CardIdVoided");
        cardIdIdentifierVoided.setVoided(true);
//        normally won't be the case in openmrs
        cardIdIdentifierVoided.setPreferred(true);
        cardIdIdentifierVoided.setIdentifierType(cardIdType);

        patient.setIdentifiers(new HashSet<>(Arrays.asList(secondIdentifier,identifier,cardIdIdentifier,cardIdIdentifierVoided)));
        assertEquals(4, patient.getIdentifiers().size());
        assertEquals(3, patient.getActiveIdentifiers().size());
        appointment.setUuid("appointmentUuid");
        appointment.setPatient(patient);
        appointment.setService(service);
        appointment.setServiceType(serviceType);
        appointment.setLocation(location);

        AppointmentProvider appointmentProvider = new AppointmentProvider();
        appointmentProvider.setProvider(provider);
        appointmentProvider.setResponse(AppointmentProviderResponse.ACCEPTED);

        Set<AppointmentProvider> appProviders = new HashSet<>();
        appProviders.add(appointmentProvider);
        appointment.setProviders(appProviders);

        appointment.getProviders().add(appointmentProvider);

        String startDateTime = "2017-03-15T16:57:09.0Z";
        Date startDate = DateUtil.convertToDate(startDateTime, DateUtil.DateFormatType.UTC);
        appointment.setStartDateTime(startDate);
        String endDateTime = "2017-03-15T17:57:09.0Z";
        Date endDate = DateUtil.convertToDate(endDateTime, DateUtil.DateFormatType.UTC);
        appointment.setEndDateTime(endDate);
        appointment.setAppointmentKind(AppointmentKind.Scheduled);
        appointment.setStatus(AppointmentStatus.Scheduled);
        appointment.setComments("Initial Consultation");
        return appointment;
    }

    @Test
    public void shouldMapAppointmentQueryToAppointment() {
        AppointmentQuery appointmentQuery = new AppointmentQuery();
        appointmentQuery.setServiceUuid("someServiceUuid");
        appointmentQuery.setProviderUuid("providerUuid");
        appointmentQuery.setPatientUuid("patientUuid");
        appointmentQuery.setLocationUuid("locationUuid");
        appointmentQuery.setStatus("Completed");
        appointmentQuery.setAppointmentKind("Scheduled");
        appointmentQuery.setServiceTypeUuid("serviceTypeUuid");
        Appointment appointment = appointmentMapper.mapQueryToAppointment(appointmentQuery);

        AppointmentServiceDefinition appointmentServiceDefinition = new AppointmentServiceDefinition();
        appointmentServiceDefinition.setUuid("serviceUuid");
        when(appointmentServiceDefinitionService.getAppointmentServiceByUuid("someServiceUuid")).thenReturn(appointmentServiceDefinition);
        Patient patient = new Patient();
        patient.setUuid("patientUuid");
        when(patientService.getPatientByUuid("patientUuid")).thenReturn(patient);

        Provider provider = new Provider();
        provider.setUuid("providerUuid");
        when(providerService.getProviderByUuid("providerUuid")).thenReturn(provider);

        Location location = new Location();
        location.setUuid("locationUuid");
        when(locationService.getLocationByUuid("locationUuid")).thenReturn(location);

        assertEquals("locationUuid", appointment.getLocation().getUuid());
        assertEquals("patientUuid", appointment.getPatient().getUuid());
        assertEquals("providerUuid", appointment.getProvider().getUuid());
        assertEquals("Completed", appointment.getStatus().toString());
    }

    @Test
    public void shouldMapAppointmentProvider() {
        AppointmentProviderDetail providerDetail = new AppointmentProviderDetail();
        providerDetail.setResponse("ACCEPTED");
        providerDetail.setUuid("providerUuid");

        Provider provider = new Provider();
        provider.setUuid("providerUuid");
        when(providerService.getProviderByUuid("providerUuid")).thenReturn(provider);

        AppointmentProvider appointmentProvider = appointmentMapper.mapAppointmentProvider(providerDetail);
        assertEquals(AppointmentProviderResponse.ACCEPTED, appointmentProvider.getResponse());
    }

    @Test
    public void shouldMapResponses() {
        assertEquals(AppointmentProviderResponse.ACCEPTED, appointmentMapper.mapProviderResponse("ACCEPTED"));
        assertEquals(AppointmentProviderResponse.AWAITING, appointmentMapper.mapProviderResponse("AWAITING"));
        assertEquals(AppointmentProviderResponse.CANCELLED, appointmentMapper.mapProviderResponse("CANCELLED"));
        assertEquals(AppointmentProviderResponse.REJECTED, appointmentMapper.mapProviderResponse("REJECTED"));
        assertEquals(AppointmentProviderResponse.TENTATIVE, appointmentMapper.mapProviderResponse("TENTATIVE"));
    }


    @Test
    public void shouldMapProviderDetailsForAppointment() throws Exception {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);

        assertNotNull(appointment);
        verify(patientService, times(1)).getPatientByUuid(appointmentRequest.getPatientUuid());
        assertEquals(this.patient, appointment.getPatient());
        verify(appointmentServiceDefinitionService, times(1)).getAppointmentServiceByUuid(appointmentRequest.getServiceUuid());
        assertEquals(service, appointment.getService());
        assertEquals(serviceType, appointment.getServiceType());

        AppointmentProvider appointmentProvider = appointment.getProviders().iterator().next();
        assertEquals(provider.getUuid(), appointmentProvider.getProvider().getUuid());
        assertEquals(AppointmentProviderResponse.ACCEPTED, appointmentProvider.getResponse());
    }

    @Test
    public void shouldCancelProvidersOfExistingAppointment() throws Exception {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        existingAppointment.setServiceType(serviceType2);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);

        AppointmentRequest appointmentRequest = createAppointmentRequest();
        //removing existing providers
        appointmentRequest.setProviders(Collections.EMPTY_LIST);
        appointmentRequest.setUuid(appointmentUuid);
        appointmentRequest.setServiceTypeUuid(serviceType2.getUuid());

        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);

        AppointmentProvider appointmentProvider = appointment.getProviders().iterator().next();
        assertEquals(provider.getUuid(), appointmentProvider.getProvider().getUuid());
        assertEquals(AppointmentProviderResponse.CANCELLED, appointmentProvider.getResponse());

    }

    @Test
    public void shouldReturnEmptyListWhenProvidersAreNullForAnAppointment() throws ParseException {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment appointment = createAppointment();
        appointment.setUuid(appointmentUuid);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(appointment);
        appointment.setProvider(null);
        appointment.setProviders(null);

        AppointmentDefaultResponse appointmentDefaultResponse = appointmentMapper.constructResponse(appointment);

        assertEquals(Collections.EMPTY_LIST, appointmentDefaultResponse.getProviders());
    }

    @Test
    public void shouldReturnEmptyListWhenProvidersListIsEmptyForAnAppointment() throws ParseException {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment appointment = createAppointment();
        appointment.setUuid(appointmentUuid);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(appointment);
        appointment.setProvider(null);
        appointment.setProviders(Collections.EMPTY_SET);

        AppointmentDefaultResponse appointmentDefaultResponse = appointmentMapper.constructResponse(appointment);

        assertEquals(Collections.EMPTY_LIST, appointmentDefaultResponse.getProviders());
    }

    @Test
    public void shouldChangeTheResponseAndVoidedDataWhenProviderIsVoidedAndAddedAgain() throws ParseException {
        String appointmentUuid = "7869637c-12fe-4121-9692-b01f93f99e55";
        Appointment existingAppointment = createAppointment();
        existingAppointment.setUuid(appointmentUuid);
        AppointmentProvider appointmentProvider = new AppointmentProvider();
        appointmentProvider.setProvider(provider);
        appointmentProvider.setResponse(AppointmentProviderResponse.ACCEPTED);
        Set<AppointmentProvider> appProviders = new HashSet<>();
        appProviders.add(appointmentProvider);
        existingAppointment.setProviders(appProviders);
        appointmentProvider.setVoided(true);
        when(appointmentsService.getAppointmentByUuid(appointmentUuid)).thenReturn(existingAppointment);
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        appointmentRequest.setUuid(appointmentUuid);

        Appointment appointment = appointmentMapper.fromRequest(appointmentRequest);

        assertEquals(((AppointmentProvider)appointment.getProviders().toArray()[0]).getVoided(), false);
        assertEquals(((AppointmentProvider)appointment.getProviders().toArray()[0]).getResponse(),
                AppointmentProviderResponse.ACCEPTED);
        assertEquals(((AppointmentProvider)appointment.getProviders().toArray()[0]).getVoidReason(), null);
    }

    @Test
    public void shouldSetIsRecurringToTrueInResponseIfAppointmentHasRecurringPattern() throws ParseException {
        Appointment appointment = createAppointment();
        appointment.setAppointmentRecurringPattern(new AppointmentRecurringPattern());

        AppointmentDefaultResponse appointmentDefaultResponse = appointmentMapper.constructResponse(appointment);

        assertTrue(appointmentDefaultResponse.getRecurring());
    }

    @Test
    public void shouldSetIsRecurringToFalseInResponseIfAppointmentDoesNotHaveRecurringPattern() throws ParseException {
        Appointment appointment = createAppointment();

        AppointmentDefaultResponse appointmentDefaultResponse = appointmentMapper.constructResponse(appointment);

        assertFalse(appointmentDefaultResponse.getRecurring());
    }

    @Test
    public void shouldReturnClonedAppointmentFromRequest() throws ParseException {
        AppointmentRequest appointmentRequest = createAppointmentRequest();
        Appointment appointment = appointmentMapper.fromRequestClonedAppointment(appointmentRequest);
        assertNotNull(appointment);
        verify(patientService, times(1)).getPatientByUuid(appointmentRequest.getPatientUuid());
        assertEquals(patient, appointment.getPatient());
        verify(appointmentServiceDefinitionService, times(1)).getAppointmentServiceByUuid(appointmentRequest.getServiceUuid());
        assertEquals(service, appointment.getService());
        assertEquals(serviceType, appointment.getServiceType());
        verify(providerService, times(1)).getProviderByUuid(appointmentRequest.getProviderUuid());
        assertEquals(provider, appointment.getProviders().iterator().next().getProvider());
        verify(locationService, times(1)).getLocationByUuid(appointmentRequest.getLocationUuid());
        assertEquals(location, appointment.getLocation());
        assertEquals(appointmentRequest.getStartDateTime(), appointment.getStartDateTime());
        assertEquals(appointmentRequest.getEndDateTime(), appointment.getEndDateTime());
        assertEquals(AppointmentKind.valueOf(appointmentRequest.getAppointmentKind()), appointment.getAppointmentKind());
        assertEquals(AppointmentStatus.Scheduled, appointment.getStatus());
        assertEquals(appointmentRequest.getComments(), appointment.getComments());
    }
}
