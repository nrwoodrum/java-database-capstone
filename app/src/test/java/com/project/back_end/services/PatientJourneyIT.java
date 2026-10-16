package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;

import com.project.back_end.models.Doctor;
import com.project.back_end.repo.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PatientJourneyIT {
    @Autowired TestRestTemplate http;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void patientCanRegisterSignInBookAndSeeAppointment() {
        String suffix = UUID.randomUUID().toString();
        String email = "journey-" + suffix + "@example.com";
        Doctor doctor =
                doctors.saveAndFlush(
                        new Doctor(
                                "Journey Test Doctor",
                                "general",
                                "doctor-" + suffix + "@example.com",
                                "test-password",
                                "1234567890",
                                List.of("09:00-10:00")));
        try {
            var signup =
                    http.postForEntity(
                            "/patient",
                            Map.of(
                                    "name",
                                    "Journey Patient",
                                    "email",
                                    email,
                                    "password",
                                    "test-password",
                                    "phone",
                                    String.format("%010d", Math.abs((long) suffix.hashCode())),
                                    "address",
                                    "Test Address"),
                            String.class);
            assertEquals(HttpStatus.CREATED, signup.getStatusCode());
            var login =
                    http.postForEntity(
                            "/patient/login",
                            Map.of("email", email, "password", "test-password"),
                            Map.class);
            assertEquals(HttpStatus.OK, login.getStatusCode());
            String token = (String) login.getBody().get("token");
            var patient = patients.findByEmail(email);
            var booking =
                    http.postForEntity(
                            "/appointments/" + token,
                            Map.of(
                                    "doctor",
                                    Map.of("id", doctor.getId()),
                                    "patient",
                                    Map.of("id", 999999L),
                                    "appointmentTime",
                                    LocalDate.now().plusDays(2) + "T09:00:00",
                                    "status",
                                    0),
                            String.class);
            assertEquals(HttpStatus.CREATED, booking.getStatusCode());
            var listing =
                    http.getForEntity(
                            "/patient/appointments/" + patient.getId() + "/" + token + "/patient",
                            Map.class);
            assertEquals(HttpStatus.OK, listing.getStatusCode());
            List<?> rows = (List<?>) listing.getBody().get("appointments");
            assertEquals(1, rows.size());
            Map<?, ?> row = (Map<?, ?>) rows.get(0);
            assertEquals(patient.getId(), ((Number) row.get("patientId")).longValue());
            assertEquals(doctor.getId(), ((Number) row.get("doctorId")).longValue());
        } finally {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                appointments.deleteAllByDoctorId(doctor.getId());
                                appointments.flush();
                                var patient = patients.findByEmail(email);
                                if (patient != null) patients.delete(patient);
                                doctors.deleteById(doctor.getId());
                            });
        }
    }
}
