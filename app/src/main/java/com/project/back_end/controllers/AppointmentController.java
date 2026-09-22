package com.project.back_end.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.AppointmentService;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private final AppointmentService appointmentService;
    private final AdminService service;

    public AppointmentController(AppointmentService appointmentService, AdminService service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(@PathVariable String date, @PathVariable String patientName, @PathVariable String token) {
        String error = service.validateToken(token, "doctor");
        if (error == null) {
            List<Appointment> appointments = appointmentService.getAppointmentsByDateAndPatientName(date, patientName);
            return ResponseEntity.ok(appointments);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/book/{token}")
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment, @PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            int validationResult = service.validateAppointment(appointment.getDoctor().getId(), appointment.getAppointmentTime());
            if (validationResult == 1) {
                int result = appointmentService.bookAppointment(appointment);
                if (result == 1) {
                    return ResponseEntity.ok("Appointment booked successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to book appointment");
                }
            } else if (validationResult == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid appointment time");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(@RequestBody Appointment appointment, @PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            int validationResult = service.validateAppointment(appointment.getDoctor().getId(), appointment.getAppointmentTime());
            if (validationResult == 1) {
                int result = appointmentService.updateAppointment(appointment);
                if (result == 1) {
                    return ResponseEntity.ok("Appointment updated successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update appointment");
                }
            } else if (validationResult == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid appointment time");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id, @PathVariable String token) {
        String error = service.validateToken(token, "patient");
        if (error == null) {
            int result = appointmentService.cancelAppointment(id);
            if (result == 1) {
                return ResponseEntity.ok("Appointment canceled successfully");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel appointment");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.


// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.


// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.


}
