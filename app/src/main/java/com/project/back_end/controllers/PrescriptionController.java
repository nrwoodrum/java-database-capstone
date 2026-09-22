package com.project.back_end.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {

    @Autowired
    private final PrescriptionService prescriptionService;
    private final AdminService service;
    private final AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService, AdminService service, AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/save/{token}")
    public ResponseEntity<?> savePrescription(@RequestBody @Valid Prescription prescription, @PathVariable String token) {
        String error = service.validateToken(token, "doctor");
        if (error == null) {
            // Update the appointment status to reflect that a prescription has been added
            appointmentService.updateAppointmentStatus(prescription.getAppointment().getId(), "Prescription Added");
            int result = prescriptionService.savePrescription(prescription);
            if (result == 1) {
                return ResponseEntity.ok("Prescription saved successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save prescription");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/get/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(@PathVariable Long appointmentId, @PathVariable String token) {
        String error = service.validateToken(token, "doctor");
        if (error == null) {
            Prescription prescription = prescriptionService.getPrescriptionByAppointmentId(appointmentId);
            if (prescription != null) {
                return ResponseEntity.ok(prescription);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Prescription not found for the given appointment ID");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("${api.path}prescription")` to set the base path for all prescription-related endpoints.
//    - This controller manages creating and retrieving prescriptions tied to appointments.


// 2. Autowire Dependencies:
//    - Inject `PrescriptionService` to handle logic related to saving and fetching prescriptions.
//    - Inject the shared `Service` class for token validation and role-based access control.
//    - Inject `AppointmentService` to update appointment status after a prescription is issued.


// 3. Define the `savePrescription` Method:
//    - Handles HTTP POST requests to save a new prescription for a given appointment.
//    - Accepts a validated `Prescription` object in the request body and a doctor’s token as a path variable.
//    - Validates the token for the `"doctor"` role.
//    - If the token is valid, updates the status of the corresponding appointment to reflect that a prescription has been added.
//    - Delegates the saving logic to `PrescriptionService` and returns a response indicating success or failure.


// 4. Define the `getPrescription` Method:
//    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
//    - Accepts the appointment ID and a doctor’s token as path variables.
//    - Validates the token for the `"doctor"` role using the shared service.
//    - If the token is valid, fetches the prescription using the `PrescriptionService`.
//    - Returns the prescription details or an appropriate error message if validation fails.


}
