package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("${api.path}" + "doctor")
public class DoctorController {
	private final DoctorService doctorService;
	private final Service service;

	public DoctorController(DoctorService doctorService, Service service) {
		this.doctorService = doctorService;
		this.service = service;
	}

	@GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
	public ResponseEntity<Map<String, Object>> getDoctorAvailability(@PathVariable String user,
			@PathVariable Long doctorId, @PathVariable String date, @PathVariable String token) {
		ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
		if (!validation.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(validation.getStatusCode())
					.body(Map.of("message", validation.getBody().get("message")));
		}

		LocalDate parsedDate = LocalDate.parse(date);
		List<String> availability = doctorService.getDoctorAvailability(doctorId, parsedDate);
		return ResponseEntity.ok(Map.of("availability", availability));
	}

	@GetMapping
	public ResponseEntity<Map<String, Object>> getDoctor() {
		return ResponseEntity.ok(Map.of("doctors", doctorService.getDoctors()));
	}

	@PostMapping("/{token}")
	public ResponseEntity<Map<String, String>> saveDoctor(@RequestBody Doctor doctor, @PathVariable String token) {
		ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
		if (!validation.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
		}

		int result = doctorService.saveDoctor(doctor);
		if (result == 1) {
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Doctor added to db"));
		} else if (result == -1) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Doctor already exists"));
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Some internal error occurred"));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
		return doctorService.validateDoctor(login);
	}

	@PutMapping("/{token}")
	public ResponseEntity<Map<String, String>> updateDoctor(@RequestBody Doctor doctor, @PathVariable String token) {
		ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
		if (!validation.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
		}

		int result = doctorService.updateDoctor(doctor);
		if (result == 1) {
			return ResponseEntity.ok(Map.of("message", "Doctor updated"));
		} else if (result == -1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Doctor not found"));
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Some internal error occurred"));
		}
	}

	@DeleteMapping("/{id}/{token}")
	public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable Long id, @PathVariable String token) {
		ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
		if (!validation.getStatusCode().is2xxSuccessful()) {
			return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
		}

		int result = doctorService.deleteDoctor(id);
		if (result == 1) {
			return ResponseEntity.ok(Map.of("message", "Doctor deleted successfully"));
		} else if (result == -1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Doctor not found with id"));
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Some internal error occurred"));
		}
	}

	@GetMapping("/filter/{name}/{time}/{speciality}")
	public ResponseEntity<Map<String, Object>> filterDoctor(@PathVariable String name, @PathVariable String time,
			@PathVariable String speciality) {
		return ResponseEntity.ok(service.filterDoctor(name, time, speciality));
	}

}

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
}

