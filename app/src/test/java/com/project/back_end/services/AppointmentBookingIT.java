package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AppointmentBookingIT {
    @Autowired AppointmentService service;
    @Autowired TokenService tokens;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;

    @Test
    void persistsIdOnlyPatientAndDoctorReferencesForFutureBooking() {
        Doctor savedDoctor =
                doctors.saveAndFlush(
                        new Doctor(
                                "Booking Test Doctor",
                                "general",
                                "booking-test-doctor@example.com",
                                "test-password",
                                "1234567890",
                                java.util.List.of("09:00-10:00")));
        Patient savedPatient =
                patients.saveAndFlush(
                        new Patient(
                                "Booking Test Patient",
                                "booking-test-patient@example.com",
                                "test-password",
                                "1234567890",
                                "Test Address"));
        Doctor doctor = new Doctor();
        doctor.setId(savedDoctor.getId());
        Patient patient = new Patient();
        patient.setId(savedPatient.getId());
        Appointment booking = new Appointment(doctor, patient, LocalDateTime.now().plusYears(1), 0);
        assertEquals(
                1,
                service.bookAppointment(
                        booking,
                        tokens.generateToken(
                                savedPatient.getId(), "patient", savedPatient.getEmail())));
        assertNotNull(booking.getId());
        assertTrue(appointments.findById(booking.getId()).isPresent());
        assertEquals(savedPatient.getName(), booking.getPatient().getName());
    }
}
