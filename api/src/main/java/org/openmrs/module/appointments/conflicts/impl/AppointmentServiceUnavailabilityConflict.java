package org.openmrs.module.appointments.conflicts.impl;

import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.conflicts.AppointmentConflict;
import org.openmrs.module.appointments.model.AppointmentConflictType;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.ServiceWeeklyAvailability;
import org.openmrs.module.appointments.util.DateUtil;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.appointments.model.AppointmentConflictType.SERVICE_UNAVAILABLE;
import static org.openmrs.module.appointments.util.DateUtil.getEpochTime;

public class AppointmentServiceUnavailabilityConflict implements AppointmentConflict {

    private final static String DAY_OF_WEEK_PATTERN = "EEEE";
    private final SimpleDateFormat DayFormat = new SimpleDateFormat(DAY_OF_WEEK_PATTERN);

    @Override
    public AppointmentConflictType getType() {
        return SERVICE_UNAVAILABLE;
    }

    @Override
    public List<Appointment> getConflicts(List<Appointment> appointments) {
        List<Appointment> conflictingAppointments = new ArrayList<>();
        for (Appointment appointment : appointments) {
            AppointmentServiceDefinition appointmentServiceDefinition = appointment.getService();
            boolean isConflicting = checkConflicts(appointment, appointmentServiceDefinition);
            if (isConflicting)
                conflictingAppointments.add(appointment);
        }
        return conflictingAppointments;
    }

    private boolean checkConflicts(Appointment appointment, AppointmentServiceDefinition appointmentServiceDefinition) {
        Set<ServiceWeeklyAvailability> weeklyAvailableDays = appointmentServiceDefinition.getWeeklyAvailability();
        if (isObjectPresent(weeklyAvailableDays)) {
            String appointmentDay = DayFormat.format(appointment.getStartDateTime());
            List<ServiceWeeklyAvailability> dayAvailabilities = weeklyAvailableDays.stream()
                    .filter(day -> day.isSameDay(appointmentDay)).collect(Collectors.toList());
            if (!dayAvailabilities.isEmpty())
                return dayAvailabilities.stream().allMatch(availableDay ->
                        checkTimeAvailability(appointment, availableDay.getStartTime().getTime(), availableDay.getEndTime().getTime()));
            return true;
        }
        Time serviceStartTime = appointmentServiceDefinition.getStartTime();
        Time serviceEndTime = appointmentServiceDefinition.getEndTime();
        long serviceStartMillis = serviceStartTime != null ? serviceStartTime.getTime() : DateUtil.getStartOfDay().getTime();
        long serviceEndMillis = serviceEndTime != null ? serviceEndTime.getTime() : DateUtil.getEndOfDay().getTime();
        return checkTimeAvailability(appointment, serviceStartMillis, serviceEndMillis);
    }

    private boolean isObjectPresent(Collection<?> object) {
        return Objects.nonNull(object) && !object.isEmpty();
    }

    private boolean checkTimeAvailability(Appointment appointment, long serviceStartTime, long serviceEndTime) {
        String clientTimezoneProperty = Context.getAuthenticatedUser().getUserProperty("clientTimezone");

        Date appointmentStartDate;
        Date appointmentEndDate;

        if (clientTimezoneProperty != null) {
            appointmentStartDate = convertDateTimezone(appointment.getStartDateTime(), clientTimezoneProperty, "UTC");
            appointmentEndDate = convertDateTimezone(appointment.getEndDateTime(), clientTimezoneProperty, "UTC");
        } else {
            appointmentStartDate = appointment.getStartDateTime();
            appointmentEndDate = appointment.getEndDateTime();
        }

        long appointmentStartTimeMilliSeconds = getEpochTime(appointmentStartDate.getTime());
        long appointmentEndTimeMilliSeconds = getEpochTime(appointmentEndDate.getTime());
        long serviceStartTimeMilliSeconds = getEpochTime(serviceStartTime);
        long serviceEndTimeMilliSeconds = getEpochTime(serviceEndTime);
        boolean isConflict = (appointmentStartTimeMilliSeconds >= appointmentEndTimeMilliSeconds)
                || ((appointmentStartTimeMilliSeconds < serviceStartTimeMilliSeconds)
                || (appointmentEndTimeMilliSeconds > serviceEndTimeMilliSeconds));
        return isConflict;
    }

    private Date convertDateTimezone(Date date, String fromTimezone, String toTimezone) {

        Calendar fromCalendar = Calendar.getInstance(TimeZone.getTimeZone(fromTimezone));
        fromCalendar.setTime(date);

        Calendar toCalendar = Calendar.getInstance(TimeZone.getTimeZone(toTimezone));
        toCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
        toCalendar.set(Calendar.MONTH, fromCalendar.get(Calendar.MONTH));
        toCalendar.set(Calendar.DAY_OF_MONTH, fromCalendar.get(Calendar.DAY_OF_MONTH));
        toCalendar.set(Calendar.HOUR_OF_DAY, fromCalendar.get(Calendar.HOUR_OF_DAY));
        toCalendar.set(Calendar.MINUTE, fromCalendar.get(Calendar.MINUTE));
        toCalendar.set(Calendar.SECOND, fromCalendar.get(Calendar.SECOND));
        toCalendar.set(Calendar.MILLISECOND, fromCalendar.get(Calendar.MILLISECOND));

        return toCalendar.getTime();
    }
}
