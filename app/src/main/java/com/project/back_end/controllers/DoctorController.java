package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.AdminService;
import com.project.back_end.services.DoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/doctor", "/doctor/"})
public class DoctorController {

    private final DoctorService doctorService;
    private final AdminService adminservice;

    public DoctorController(DoctorService doctorService, AdminService adminservice) {
        this.doctorService = doctorService;
        this.adminservice = adminservice;
    }

    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(@PathVariable String user, @PathVariable Long doctorId,
            @PathVariable String date, @PathVariable String token) {
        if (!adminservice.validateToken(token, user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        }

        List<?> availability = doctorService.getDoctorAvailability(doctorId, LocalDate.parse(date));
        return ResponseEntity.ok(Map.of("availability", availability));
    }

    @GetMapping(value = {"", "/"})
    public ResponseEntity<?> getDoctor() {
        List<Doctor> doctors = doctorService.getDoctors();
        return ResponseEntity.ok(Map.of("doctors", doctors));
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> saveDoctor(@RequestBody @Valid Doctor doctor, @PathVariable String token) {
        if (!adminservice.validateToken(token, "admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        int result = doctorService.saveDoctor(doctor);
        if (result == -1) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Doctor already exists");
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body("Doctor added successfully");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody @Valid Login login) {
    return doctorService.validateDoctor(login);
}

    @PutMapping("/{token}")
public ResponseEntity<?> updateDoctor(@RequestBody @Valid Doctor doctor, @PathVariable String token) {
    if (!adminservice.validateToken(token, "admin")) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
    }

    int result = doctorService.updateDoctor(doctor);

    if (result == 1) {
        return ResponseEntity.ok("Doctor updated successfully");
    } else if (result == -1) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
    } else {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed");
    }
}

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id, @PathVariable String token) {
        if (!adminservice.validateToken(token, "admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        int result = doctorService.deleteDoctor(id);
        if (result == 1) {
            return ResponseEntity.ok("Doctor deleted successfully");
        } else if (result == -1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the doctor");
        }
    }

    @GetMapping("/filter")
public ResponseEntity<?> filterDoctors(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) String time,
                                       @RequestParam(required = false) String specialty) {
    Map<String, Object> filteredDoctors = adminservice.filterDoctor(name, specialty, time);
    return ResponseEntity.ok(filteredDoctors);
}
    // 1. Set Up the Controller Class:
    // - Annotate the class with `@RestController` to define it as a REST controller
    // that serves JSON responses.
    // - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a
    // configurable API path followed by "doctor".
    // - This class manages doctor-related functionalities such as registration,
    // login, updates, and availability.

    // 2. Autowire Dependencies:
    // - Inject `DoctorService` for handling the core logic related to doctors
    // (e.g., CRUD operations, authentication).
    // - Inject the shared `Service` class for general-purpose features like token
    // validation and filtering.

    // 3. Define the `getDoctorAvailability` Method:
    // - Handles HTTP GET requests to check a specific doctor’s availability on a
    // given date.
    // - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
    // - First validates the token against the user type.
    // - If the token is invalid, returns an error response; otherwise, returns the
    // availability status for the doctor.

    // 4. Define the `getDoctor` Method:
    // - Handles HTTP GET requests to retrieve a list of all doctors.
    // - Returns the list within a response map under the key `"doctors"` with HTTP
    // 200 OK status.

    // 5. Define the `saveDoctor` Method:
    // - Handles HTTP POST requests to register a new doctor.
    // - Accepts a validated `Doctor` object in the request body and a token for
    // authorization.
    // - Validates the token for the `"admin"` role before proceeding.
    // - If the doctor already exists, returns a conflict response; otherwise, adds
    // the doctor and returns a success message.

    // 6. Define the `doctorLogin` Method:
    // - Handles HTTP POST requests for doctor login.
    // - Accepts a validated `Login` DTO containing credentials.
    // - Delegates authentication to the `DoctorService` and returns login status
    // and token information.

    // 7. Define the `updateDoctor` Method:
    // - Handles HTTP PUT requests to update an existing doctor's information.
    // - Accepts a validated `Doctor` object and a token for authorization.
    // - Token must belong to an `"admin"`.
    // - If the doctor exists, updates the record and returns success; otherwise,
    // returns not found or error messages.

    // 8. Define the `deleteDoctor` Method:
    // - Handles HTTP DELETE requests to remove a doctor by ID.
    // - Requires both doctor ID and an admin token as path variables.
    // - If the doctor exists, deletes the record and returns a success message;
    // otherwise, responds with a not found or error message.

    // 9. Define the `filter` Method:
    // - Handles HTTP GET requests to filter doctors based on name, time, and
    // speciality.
    // - Accepts `name`, `time`, and `speciality` as path variables.
    // - Calls the shared `Service` to perform filtering logic and returns matching
    // doctors in the response.

}
