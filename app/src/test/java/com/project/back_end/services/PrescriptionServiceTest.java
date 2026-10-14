package com.project.back_end.services;

import java.util.List;
import java.util.Optional;
import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import org.junit.jupiter.api.*;
import org.mockito.MockMakers;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrescriptionServiceTest {
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AppointmentRepository appointments = mock(AppointmentRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final TokenService tokens = mock(TokenService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AppointmentService completion = mock(AppointmentService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final PrescriptionService service = new PrescriptionService(prescriptions, appointments, tokens, completion);
    private Appointment appointment;
    private Prescription prescription;

    @BeforeEach void setUp() {
        Doctor doctor = new Doctor(); doctor.setEmail("doctor@example.com");
        Patient patient = new Patient(); patient.setName("Actual Patient");
        appointment = new Appointment(doctor, patient, null, 0); appointment.setId(7L);
        prescription = new Prescription("Forged Name", "Medicine", "Once daily", "", 7L);
        prescription.setId("caller-supplied-id");
        when(appointments.findById(7L)).thenReturn(Optional.of(appointment));
        when(tokens.extractEmail("token")).thenReturn("doctor@example.com");
        when(prescriptions.findByAppointmentId(7L)).thenReturn(List.of());
        when(completion.changeStatus(7L, 1)).thenReturn("Appointment status updated successfully");
    }
    @Test void savesWithServerIdentityAndCompletesAppointment() {
        assertEquals(201, service.savePrescription(prescription, "token").getStatusCode().value());
        assertEquals("Actual Patient", prescription.getPatientName());
        assertEquals("appointment-7", prescription.getId());
        var order = inOrder(prescriptions, completion);
        order.verify(prescriptions).insert(prescription);
        order.verify(completion).changeStatus(7L, 1);
        verify(prescriptions, never()).save(any(Prescription.class));
    }
    @Test void rejectsMissingAppointment() {
        when(appointments.findById(7L)).thenReturn(Optional.empty());
        assertEquals(404, service.savePrescription(prescription, "token").getStatusCode().value());
        assertEquals(404, service.getPrescription(7L, "token").getStatusCode().value());
        verifyNoInteractions(prescriptions, completion);
    }
    @Test void rejectsAnotherDoctorsReadAndWrite() {
        when(tokens.extractEmail("token")).thenReturn("other@example.com");
        assertEquals(403, service.savePrescription(prescription, "token").getStatusCode().value());
        assertEquals(403, service.getPrescription(7L, "token").getStatusCode().value());
        verifyNoInteractions(prescriptions, completion);
    }
    @Test void rejectsExistingPrescription() {
        when(prescriptions.findByAppointmentId(7L)).thenReturn(List.of(prescription));
        assertEquals(409, service.savePrescription(prescription, "token").getStatusCode().value());
        verify(prescriptions, never()).insert(any(Prescription.class));
        verifyNoInteractions(completion);
    }
    @Test void handlesConcurrentDuplicateWithoutCompletingOrDeleting() {
        when(prescriptions.insert(prescription)).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals(409, service.savePrescription(prescription, "token").getStatusCode().value());
        verifyNoInteractions(completion);
        verify(prescriptions, never()).deleteById(anyString());
    }
    @Test void rejectsCompletedAppointment() {
        appointment.setStatus(1);
        assertEquals(400, service.savePrescription(prescription, "token").getStatusCode().value());
        verify(prescriptions, never()).insert(any(Prescription.class));
    }
    @Test void undoesInsertWhenCompletionFails() {
        when(completion.changeStatus(7L, 1)).thenThrow(new IllegalStateException("database unavailable"));
        var response = service.savePrescription(prescription, "token");
        assertEquals(500, response.getStatusCode().value());
        verify(prescriptions).deleteById("appointment-7");
        assertFalse(response.getBody().get("message").contains("database unavailable"));
    }
    @Test void undoesInsertWhenAppointmentDisappears() {
        when(completion.changeStatus(7L, 1)).thenReturn("Appointment not found");
        assertEquals(500, service.savePrescription(prescription, "token").getStatusCode().value());
        verify(prescriptions).deleteById("appointment-7");
    }
    @Test void returnsExpectedViewStructure() {
        when(prescriptions.findByAppointmentId(7L)).thenReturn(List.of(prescription));
        var response = service.getPrescription(7L, "token");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(prescription), response.getBody().get("prescriptions"));
    }
}
