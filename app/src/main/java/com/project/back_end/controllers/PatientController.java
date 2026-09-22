package com.project.back_end.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.PatientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patient")
public class PatientController {
    
    @Autowired
    private final PatientService patientService;
    private final AdminService service;

    public PatientController(PatientService patientService, AdminService service) {
        this.patientService = patientService;
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPatient(@PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            Patient patient = patientService.getPatientDetails(token);
            return ResponseEntity.ok(patient);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPatient(@RequestBody @Valid Patient patient) {
        String error = service.validatePatient(patient);
        if (error == null) {
            int result = patientService.createPatient(patient);
            if (result == 1) {
                return ResponseEntity.ok("Patient created successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create patient");
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid Login login) {
        Map<String, String> response = service.validatePatientLogin(login);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/{token}")
    public ResponseEntity<?> getPatientAppointment(@PathVariable Long id, @PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            List<Appointment> appointments = patientService.getPatientAppointments(id);
            return ResponseEntity.ok(appointments);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(@PathVariable String condition, @PathVariable String name, @PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            List<Appointment> filteredAppointments = service.filterPatient(token, condition, name);
            return ResponseEntity.ok(filteredAppointments);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.



}


