package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AdminService adminservice;

    public PrescriptionController(
            PrescriptionService prescriptionService, AdminService adminservice) {
        this.prescriptionService = prescriptionService;
        this.adminservice = adminservice;
    }

    @PostMapping("/save/{token}")
    public ResponseEntity<?> savePrescription(
            @RequestBody @Valid Prescription prescription, @PathVariable String token) {
        if (!adminservice.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Invalid or expired token"));
        }

        return prescriptionService.savePrescription(prescription, token);
    }

    @GetMapping("/patient/{appointmentId}/{token}")
    public ResponseEntity<?> getPatientPrescription(
            @PathVariable Long appointmentId, @PathVariable String token) {
        if (!adminservice.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Invalid or expired token"));
        }
        return prescriptionService.getPatientPrescription(appointmentId, token);
    }

    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId, @PathVariable String token) {
        if (!adminservice.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Invalid or expired token"));
        }

        return prescriptionService.getPrescription(appointmentId, token);
    }
}
