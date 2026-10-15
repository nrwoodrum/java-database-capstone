package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class PrescriptionWorkflowIT {
    @Autowired PrescriptionService service;
    @Autowired com.project.back_end.controllers.PrescriptionController controller;
    @Autowired PrescriptionRepository prescriptions;
    @Autowired AppointmentRepository appointments;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired TokenService tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void savesPrescriptionAndCompletesPastPendingAppointment() {
        checkWorkflow(true);
    }

    @Test
    void savesPrescriptionAndCompletesFuturePendingAppointment() {
        checkWorkflow(false);
    }

    private void checkWorkflow(boolean past) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String email = "prescription-test-" + UUID.randomUUID() + "@example.com";
        Appointment fixture =
                transaction.execute(
                        status -> {
                            Doctor doctor =
                                    doctors.saveAndFlush(
                                            new Doctor(
                                                    "Prescription Test Doctor",
                                                    "general",
                                                    email,
                                                    "test-password",
                                                    "1234567890",
                                                    List.of("09:00-10:00")));
                            Patient patient =
                                    patients.saveAndFlush(
                                            new Patient(
                                                    "Prescription Test Patient",
                                                    "patient-" + email,
                                                    "test-password",
                                                    "1234567890",
                                                    "Test Address"));
                            return appointments.saveAndFlush(
                                    new Appointment(
                                            doctor, patient, LocalDateTime.now().plusDays(1), 0));
                        });
        try {
            if (past)
                jdbc.update(
                        "UPDATE appointment SET appointment_time = ? WHERE id = ?",
                        LocalDateTime.now().minusDays(1),
                        fixture.getId());
            String token = tokens.generateToken(fixture.getDoctor().getId(), "doctor", email);
            Prescription prescription =
                    new Prescription(
                            "Prescription Test Patient",
                            "Test medication",
                            "Once daily",
                            "Regression test",
                            fixture.getId());
            assertEquals(
                    201, service.savePrescription(prescription, token).getStatusCode().value());
            assertEquals(1, appointments.findById(fixture.getId()).orElseThrow().getStatus());
            var response = service.getPrescription(fixture.getId(), token);
            assertEquals(200, response.getStatusCode().value());
            assertEquals(1, ((List<?>) response.getBody().get("prescriptions")).size());
            String patientToken =
                    tokens.generateToken(
                            fixture.getPatient().getId(),
                            "patient",
                            fixture.getPatient().getEmail());
            assertEquals(
                    200,
                    controller
                            .getPatientPrescription(fixture.getId(), patientToken)
                            .getStatusCode()
                            .value());
            var patientResponse = service.getPatientPrescription(fixture.getId(), patientToken);
            Prescription visible =
                    (Prescription)
                            ((List<?>) patientResponse.getBody().get("prescriptions")).get(0);
            assertEquals("Once daily", visible.getDosage());
            assertEquals("Regression test", visible.getDoctorNotes());
            assertEquals(
                    401,
                    controller
                            .savePrescription(prescription, patientToken)
                            .getStatusCode()
                            .value());
            String otherPatientToken =
                    tokens.generateToken(999L, "patient", "different@example.com");
            assertEquals(
                    403,
                    service.getPatientPrescription(fixture.getId(), otherPatientToken)
                            .getStatusCode()
                            .value());
        } finally {
            for (Prescription prescription : prescriptions.findByAppointmentId(fixture.getId()))
                prescriptions.deleteById(prescription.getId());
            transaction.executeWithoutResult(
                    status -> {
                        appointments.deleteById(fixture.getId());
                        appointments.flush();
                        patients.deleteById(fixture.getPatient().getId());
                        doctors.deleteById(fixture.getDoctor().getId());
                    });
        }
    }
}
