package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AdminService(
            TokenService tokenService,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorService doctorService,
            PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    public boolean validateToken(String token, String role) {
        try {
            return tokenService.validateToken(token, role);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResponseEntity<Map<String, String>> validateAdmin(String username, String password) {
        try {
            Admin admin = adminRepository.findByUsername(username);
            // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
            // through Spring Security PasswordEncoder and verify with matches(), never equals().
            if (admin == null || !admin.getPassword().equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password."));
            }
            String token = tokenService.generateToken(null, "admin", username);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed due to an internal error."));
        }
    }

    public Map<String, Object> filterDoctor(String name, String speciality, String time) {
        Map<String, Object> response = new HashMap<>();

        if (name == null || "null".equalsIgnoreCase(name)) {
            name = "";
        }
        if (speciality == null || "null".equalsIgnoreCase(speciality)) {
            speciality = "";
        }
        if (time == null || "null".equalsIgnoreCase(time)) {
            time = "";
        }

        Map<String, Object> doctors =
                doctorService.filterDoctorsByNameSpecialtyAndTime(name, speciality, time);

        response.put("doctors", doctors.getOrDefault("doctors", List.of()));
        return response;
    }

    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null) {
            return -1;
        }

        Long doctorId = appointment.getDoctor().getId();

        if (doctorRepository.findById(doctorId).isEmpty()) {
            return -1;
        }

        List<LocalTime> availableTimes =
                doctorService.getDoctorAvailability(
                        doctorId, appointment.getAppointmentTime().toLocalDate());

        if (availableTimes.contains(appointment.getAppointmentTime().toLocalTime())) {
            return 1;
        }

        return 0;
    }

    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) != null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(String email, String password) {
        Map<String, String> response = new HashMap<>();

        Patient patient = patientRepository.findByEmail(email);
        if (patient == null) {
            response.put("error", "Patient not found");
            return ResponseEntity.status(401).body(response);
        }

        // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
        // through Spring Security PasswordEncoder and verify with matches(), never equals().
        if (!patient.getPassword().equals(password)) {
            response.put("error", "Invalid password");
            return ResponseEntity.status(401).body(response);
        }

        String token = tokenService.generateToken(patient.getId(), "patient", email);
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> filterPatient(
            String condition, String name, String token) {
        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);

        if (patient == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized access"));
        }

        if (condition != null && !condition.isBlank() && name != null && !name.isBlank()) {
            return patientService.filterByDoctorAndCondition(name, condition, patient.getId());
        }

        if (condition != null && !condition.isBlank()) {
            return patientService.filterByCondition(condition, patient.getId());
        }

        if (name != null && !name.isBlank()) {
            return patientService.filterByDoctor(name, patient.getId());
        }

        return patientService.getPatientAppointment(patient.getId(), token);
    }
}
