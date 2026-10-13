package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.List;

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

    private final AppointmentService appointmentService;
    private final AdminService adminService;

    
    public AppointmentController(AppointmentService appointmentService, AdminService adminService) {
        this.appointmentService = appointmentService;
        this.adminService = adminService;
    }

  @GetMapping("/{doctorId}/{date}/{patientName}/{token}")
public ResponseEntity<?> getAppointments(@PathVariable Long doctorId,
                                         @PathVariable String date,
                                         @PathVariable String patientName,
                                         @PathVariable String token) {
    if (!adminService.validateToken(token, "doctor")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
    }

    String normalizedPatientName = (patientName == null || patientName.isBlank() || "all".equalsIgnoreCase(patientName)
            || "null".equalsIgnoreCase(patientName)) ? null : patientName;

    LocalDate localDate = LocalDate.parse(date);
    List<Appointment> appointments = appointmentService.getAppointments(doctorId, localDate, normalizedPatientName);
    return ResponseEntity.ok(appointments);
}

@PostMapping("/{token}")
public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment, @PathVariable String token) {
    if (!adminService.validateToken(token, "patient")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
    }

     int validationCode = adminService.validateAppointment(appointment);

        if (validationCode == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Doctor not found.");
        } else if (validationCode == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Appointment slot is not available.");
        }


    int result = appointmentService.bookAppointment(appointment);
    if (result == 1) {
        return ResponseEntity.status(HttpStatus.CREATED).body("Appointment booked successfully");
    } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to book appointment");
    }
}

@PutMapping("/{appointmentId}/{token}")
public ResponseEntity<?> updateAppointment(@PathVariable Long appointmentId,
                                           @RequestBody Appointment updatedAppointment,
                                           @PathVariable String token) {
    if (!adminService.validateToken(token, "patient")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
    }

    String result = appointmentService.updateAppointment(appointmentId, updatedAppointment, updatedAppointment.getPatient().getId());
    if (result.equals("Appointment updated successfully")) {
        return ResponseEntity.ok(result);
    } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

}

@DeleteMapping ("/cancel/{token}/{appointmentId}/{patientId}")
public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId, @PathVariable String token, @PathVariable Long patientId) {
    if (!adminService.validateToken(token, "patient")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
    }

    String result = appointmentService.cancelAppointment(appointmentId, patientId);
        return result.equals("Appointment canceled successfully")
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
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


