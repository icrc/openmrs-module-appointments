package org.openmrs.module.appointments.dao;

import java.util.Date;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentSearchRequest;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AppointmentDao {

    @Transactional
    void save(Appointment appointment);

    List<Appointment> getAllAppointments(Date forDate, List<String> locationIds);

    List<Appointment> search(Appointment appointment, List<String> locationIds);

    List<Appointment> getAllFutureAppointmentsForService(AppointmentServiceDefinition appointmentServiceDefinition, List<String> locationIds);

    List<Appointment> getAllFutureAppointmentsForServiceType(AppointmentServiceType appointmentServiceType, List<String> locationIds);

    List<Appointment> getAppointmentsForService(AppointmentServiceDefinition appointmentServiceDefinition, Date startDate, Date endDate, List<AppointmentStatus> appointmentStatusFilterList, List<String> locationIds);

	Appointment getAppointmentByUuid(String uuid);

    List<Appointment> getAllAppointmentsInDateRange(Date startDate, Date endDate, List<String> locationIds);

    List<Appointment> search(AppointmentSearchRequest appointmentSearchRequest, List<String> locationIds);

    List<Appointment> getAppointmentsForPatient(Integer patientId);
}
