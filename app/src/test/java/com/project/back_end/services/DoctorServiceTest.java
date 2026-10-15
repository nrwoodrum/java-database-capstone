package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

class DoctorServiceTest {
    private final DoctorRepository doctors =
            mock(DoctorRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AppointmentRepository appointments =
            mock(AppointmentRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final DoctorService service =
            new DoctorService(
                    doctors,
                    appointments,
                    mock(TokenService.class, withSettings().mockMaker(MockMakers.SUBCLASS)));
    private Doctor morning;
    private Doctor afternoon;
    private Doctor both;

    @BeforeEach
    void setUp() {
        morning = doctor(1L, "Morning", "Cardiologist", List.of("09:00-10:00", "11:00-12:00"));
        afternoon = doctor(2L, "Afternoon", "Dermatologist", List.of("12:00-13:00", "16:00-17:00"));
        both = doctor(3L, "Both", "Cardiologist", List.of("10:00-11:00", "14:00-15:00"));
        Doctor empty = doctor(4L, "Empty", "Cardiologist", List.of());
        when(doctors.findAll()).thenReturn(List.of(morning, afternoon, both, empty));
    }

    @Test
    void filtersBySavedScheduleAndTreatsNoonAsPm() {
        assertEquals(
                List.of(morning, both),
                service.filterDoctorsByNameSpecialtyAndTime(null, null, "AM").get("doctors"));
        assertEquals(
                List.of(afternoon, both),
                service.filterDoctorsByNameSpecialtyAndTime(null, null, "PM").get("doctors"));
        assertEquals(List.of(afternoon, both), service.filterDoctorByTime(doctors.findAll(), "PM"));
    }

    @Test
    void combinesTimeWithNameAndSpecialtyAndAllowsClearingTime() {
        assertEquals(
                List.of(both),
                service.filterDoctorsByNameSpecialtyAndTime("bo", "Cardiologist", "PM")
                        .get("doctors"));
        assertEquals(
                List.of(morning, both),
                service.filterDoctorsByNameSpecialtyAndTime(null, "Cardiologist", "AM")
                        .get("doctors"));
        assertEquals(
                doctors.findAll(),
                service.filterDoctorsByNameSpecialtyAndTime(null, null, "").get("doctors"));
    }

    @Test
    void excludesBookedSlotsFromAvailabilityAndTimeFilter() {
        Appointment booked = new Appointment();
        booked.setAppointmentTime(LocalDate.now().atTime(10, 0));
        when(appointments.findByDoctorIdAndAppointmentTimeBetween(eq(both.getId()), any(), any()))
                .thenReturn(List.of(booked));

        assertEquals(
                List.of(LocalTime.of(14, 0)),
                service.getDoctorAvailability(both.getId(), LocalDate.now()));
        assertEquals(
                List.of(morning),
                service.filterDoctorsByNameSpecialtyAndTime(null, null, "AM").get("doctors"));
    }

    private Doctor doctor(Long id, String name, String specialty, List<String> slots) {
        Doctor doctor =
                new Doctor(name, specialty, "doctor@example.com", "password", "1234567890", slots);
        doctor.setId(id);
        when(doctors.findById(id)).thenReturn(Optional.of(doctor));
        when(appointments.findByDoctorIdAndAppointmentTimeBetween(eq(id), any(), any()))
                .thenReturn(List.of());
        return doctor;
    }
}
