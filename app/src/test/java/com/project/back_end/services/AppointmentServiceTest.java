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
    private final AppointmentService service =
            new AppointmentService(
                    appointments,
                    doctors,
                    patients,
                    mock(TokenService.class, withSettings().mockMaker(MockMakers.SUBCLASS)));

    private Appointment booking() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        Patient patient = new Patient();
        patient.setId(2L);
        when(doctors.findById(1L)).thenReturn(Optional.of(doctor));
        when(patients.findById(2L)).thenReturn(Optional.of(patient));
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
                        () -> service.bookAppointment(appointment)));
    }

    @Test
    void rejectsPastAppointmentsWithoutSaving() {
        Appointment appointment = booking();
        appointment.setAppointmentTime(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment(appointment));
        verifyNoInteractions(appointments);
    }

    @Test
    void rejectsMissingPatientWithoutSaving() {
        Appointment appointment = booking();
        when(patients.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.bookAppointment(appointment));
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
                service.updateAppointment(7L, updated, 2L));
        verify(appointments, never()).save(any(Appointment.class));
    }
}
