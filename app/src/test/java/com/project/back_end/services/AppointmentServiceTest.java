package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.dao.DataIntegrityViolationException;

class AppointmentServiceTest {
    private final AppointmentRepository appointments =
            mock(AppointmentRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final DoctorRepository doctors =
            mock(DoctorRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final PatientRepository patients =
            mock(PatientRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final TokenService tokens =
            mock(TokenService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AppointmentService service =
            new AppointmentService(appointments, doctors, patients, tokens);

    private Appointment booking() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        Patient patient = new Patient();
        patient.setId(2L);
        when(tokens.extractEmail("token")).thenReturn("patient@example.com");
        when(doctors.findById(1L)).thenReturn(Optional.of(doctor));
        when(patients.findByEmail("patient@example.com")).thenReturn(patient);
        return new Appointment(doctor, patient, LocalDateTime.now().plusDays(1), 0);
    }

    @Test
    void persistenceErrorsEscapeTransactionRatherThanReturningSuccessOrZero() {
        Appointment appointment = booking();
        var error = new DataIntegrityViolationException("insert rejected");
        when(appointments.saveAndFlush(appointment)).thenThrow(error);
        assertSame(
                error,
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> service.bookAppointment(appointment, "token")));
    }

    @Test
    void rejectsPastAppointmentsWithoutSaving() {
        Appointment appointment = booking();
        appointment.setAppointmentTime(LocalDateTime.now().minusDays(1));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.bookAppointment(appointment, "token"));
        verifyNoInteractions(appointments);
    }

    @Test
    void rejectsMissingPatientWithoutSaving() {
        Appointment appointment = booking();
        when(patients.findByEmail("patient@example.com")).thenReturn(null);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.bookAppointment(appointment, "token"));
        verifyNoInteractions(appointments);
    }

    @Test
    void reschedulingStillRejectsPastTimes() {
        Appointment existing = booking();
        when(appointments.findById(7L)).thenReturn(Optional.of(existing));
        Appointment updated = booking();
        updated.setAppointmentTime(LocalDateTime.now().minusDays(1));
        assertEquals(
                "Appointment time must be in the future",
                service.updateAppointment(7L, updated, "token"));
        verify(appointments, never()).save(any(Appointment.class));
    }

    @Test
    void bookingUsesTokenPatientInsteadOfSuppliedPatient() {
        Appointment appointment = booking();
        Patient spoofed = new Patient();
        spoofed.setId(99L);
        appointment.setPatient(spoofed);
        assertEquals(1, service.bookAppointment(appointment, "token"));
        assertEquals(2L, appointment.getPatient().getId());
        verify(appointments).saveAndFlush(appointment);
    }

    @Test
    void cannotChangeAnotherPatientsAppointment() {
        Appointment existing = booking();
        Patient other = new Patient();
        other.setId(99L);
        existing.setPatient(other);
        when(appointments.findById(7L)).thenReturn(Optional.of(existing));
        assertEquals("Patient ID mismatch", service.updateAppointment(7L, booking(), "token"));
        assertEquals("Patient ID mismatch", service.cancelAppointment(7L, "token"));
        verify(appointments, never()).save(any(Appointment.class));
        verify(appointments, never()).delete(any(Appointment.class));
    }

    @Test
    void tokenPatientCanCancelOwnAppointment() {
        Appointment existing = booking();
        when(appointments.findById(7L)).thenReturn(Optional.of(existing));
        assertEquals("Appointment canceled successfully", service.cancelAppointment(7L, "token"));
        verify(appointments).delete(existing);
    }
}
