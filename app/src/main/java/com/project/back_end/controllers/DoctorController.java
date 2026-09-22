package com.project.back_end.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

    @Autowired
    private final DoctorService doctorService;
    private final Service service;

    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(@PathVariable String user, @PathVariable Long doctorId,
                                                   @PathVariable String date, @PathVariable String token) {
        String error = service.validateToken(token, user);
        if (error == null) {
            List<LocalTime> availableTimes = doctorService.getDoctorAvailability(doctorId, LocalDate.parse(date));
            return ResponseEntity.ok(availableTimes);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/getDoctors")
    public ResponseEntity<?> getDoctor() {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorService.getAllDoctors();
        response.put("doctors", doctors);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{token}")

    public ResponseEntity<?> saveDoctor(@RequestBody @Valid Doctor doctor, @PathVariable String token) {
        String error = service.validateToken(token, "admin");
        if (error == null) {
            if (doctorService.isDoctorExists(doctor.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Doctor already exists");
            } else {
                doctorService.saveDoctor(doctor);
                return ResponseEntity.ok("Doctor registered successfully");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody @Valid Login login) {
        Map<String, String> response = doctorService.validateDoctor(login);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{token}")
    public ResponseEntity<?> updateDoctor(@RequestBody @Valid Doctor doctor, @PathVariable String token) {
        String error = service.validateToken(token, "admin");
        if (error == null) {
            if (doctorService.isDoctorExists(doctor.getId())) {
                doctorService.updateDoctor(doctor);
                return ResponseEntity.ok("Doctor updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id, @PathVariable String token) {
        String error = service.validateToken(token, "admin");
        if (error == null) {
            if (doctorService.isDoctorExists(id)) {
                doctorService.deleteDoctor(id);
                return ResponseEntity.ok("Doctor deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filter(@PathVariable String name, @PathVariable String time, @PathVariable String speciality) {
        List<Doctor> filteredDoctors = service.filterDoctor(name, speciality, time);
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", filteredDoctors);
        return ResponseEntity.ok(response);
    }

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
//    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
//    - This class manages doctor-related functionalities such as registration, login, updates, and availability.


// 2. Autowire Dependencies:
//    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
//    - Inject the shared `Service` class for general-purpose features like token validation and filtering.


// 3. Define the `getDoctorAvailability` Method:
//    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
//    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
//    - First validates the token against the user type.
//    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.


// 4. Define the `getDoctor` Method:
//    - Handles HTTP GET requests to retrieve a list of all doctors.
//    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.


// 5. Define the `saveDoctor` Method:
//    - Handles HTTP POST requests to register a new doctor.
//    - Accepts a validated `Doctor` object in the request body and a token for authorization.
//    - Validates the token for the `"admin"` role before proceeding.
//    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.


// 6. Define the `doctorLogin` Method:
//    - Handles HTTP POST requests for doctor login.
//    - Accepts a validated `Login` DTO containing credentials.
//    - Delegates authentication to the `DoctorService` and returns login status and token information.


// 7. Define the `updateDoctor` Method:
//    - Handles HTTP PUT requests to update an existing doctor's information.
//    - Accepts a validated `Doctor` object and a token for authorization.
//    - Token must belong to an `"admin"`.
//    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.


// 8. Define the `deleteDoctor` Method:
//    - Handles HTTP DELETE requests to remove a doctor by ID.
//    - Requires both doctor ID and an admin token as path variables.
//    - If the doctor exists, deletes the record and returns a success message; otherwise, responds with a not found or error message.


// 9. Define the `filter` Method:
//    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
//    - Accepts `name`, `time`, and `speciality` as path variables.
//    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.


}
