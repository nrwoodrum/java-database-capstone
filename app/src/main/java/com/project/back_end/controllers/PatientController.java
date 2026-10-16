package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.PatientService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final AdminService adminservice;

    public PatientController(PatientService patientService, AdminService adminservice) {
        this.patientService = patientService;
        this.adminservice = adminservice;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPatientDetails(@PathVariable String token) {
        if (!adminservice.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        ResponseEntity<Map<String, Object>> patient = patientService.getPatientDetails(token);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found");
        }

        return patient;
    }

    @PostMapping
    public ResponseEntity<?> createPatient(@RequestBody @Valid Patient patient) {
        if (adminservice.validatePatient(patient)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Patient already exists");
        }

        int result = patientService.createPatient(patient);

        if (result == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Signup successful");
        } else if (result == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating patient");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        ResponseEntity<Map<String, String>> response =
                adminservice.validatePatientLogin(login.getEmail(), login.getPassword());
        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.getBody());
        }
    }

    @GetMapping("/appointments/{patientId}/{token}/{user}")
    public ResponseEntity<?> getPatientAppointment(
            @PathVariable Long patientId, @PathVariable String token, @PathVariable String user) {
        if (!adminservice.validateToken(token, user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        ResponseEntity<Map<String, Object>> response =
                patientService.getPatientAppointment(patientId, token);

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/appointments/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(
            @PathVariable String condition, @PathVariable String name, @PathVariable String token) {
        if (!adminservice.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        ResponseEntity<Map<String, Object>> filteredAppointments =
                adminservice.filterPatient(token, condition, name);
        return ResponseEntity.ok(filteredAppointments);
    }
}
