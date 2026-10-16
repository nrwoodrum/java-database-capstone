package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.AppointmentService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;
    private final AdminService adminService;

    public AppointmentController(AppointmentService appointmentService, AdminService adminService) {
        this.appointmentService = appointmentService;
        this.adminService = adminService;
    }

    @GetMapping("/{doctorId}/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token) {
        if (!adminService.validateToken(token, "doctor")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        String normalizedPatientName =
                (patientName == null
                                || patientName.isBlank()
                                || "all".equalsIgnoreCase(patientName)
                                || "null".equalsIgnoreCase(patientName))
                        ? null
                        : patientName;

        LocalDate localDate = LocalDate.parse(date);
        List<Appointment> appointments =
                appointmentService.getAppointments(doctorId, localDate, normalizedPatientName);
        return ResponseEntity.ok(appointments);
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @RequestBody @Validated(Appointment.Scheduling.class) Appointment appointment,
            @PathVariable String token) {
        if (!adminService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        try {
            int validationCode = adminService.validateAppointment(appointment);
            if (validationCode == -1) {
                return ResponseEntity.badRequest().body("Doctor not found.");
            } else if (validationCode == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Appointment slot is not available.");
            }
            appointmentService.bookAppointment(appointment, token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Appointment booked successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.warn("Booking could not be saved", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Appointment could not be saved. Please refresh the available appointments and try again.");
        } catch (RuntimeException e) {
            log.error("Error booking appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to book appointment. Please try again later.");
        }
    }

    @PutMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable Long appointmentId,
            @RequestBody Appointment updatedAppointment,
            @PathVariable String token) {
        if (!adminService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        String result =
                appointmentService.updateAppointment(appointmentId, updatedAppointment, token);
        if (result.equals("Appointment updated successfully")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @DeleteMapping("/cancel/{token}/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long appointmentId, @PathVariable String token) {
        if (!adminService.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        String result = appointmentService.cancelAppointment(appointmentId, token);
        return result.equals("Appointment canceled successfully")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }
}
