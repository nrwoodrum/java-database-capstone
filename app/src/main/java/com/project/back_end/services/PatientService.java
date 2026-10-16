package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    public int createPatient(Patient patient) {
        try {
            // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
            // through Spring Security PasswordEncoder and verify with matches(), never equals().
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            System.err.println("Error creating patient: " + e.getMessage());
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);
        if (patient == null || !patient.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized access"));
        }

        List<Appointment> appointments = appointmentRepository.findByPatientId(id);
        List<AppointmentDTO> appointmentDTOs =
                appointments.stream()
                        .map(
                                appointment ->
                                        new AppointmentDTO(
                                                appointment.getId(),
                                                appointment.getDoctor().getId(),
                                                appointment.getDoctor().getName(),
                                                appointment.getPatient().getId(),
                                                appointment.getPatient().getName(),
                                                appointment.getPatient().getEmail(),
                                                appointment.getPatient().getPhone(),
                                                appointment.getPatient().getAddress(),
                                                appointment.getAppointmentTime(),
                                                appointment.getStatus()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("appointments", appointmentDTOs));
    }

    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        List<Appointment> appointments;
        if ("past".equalsIgnoreCase(condition)) {
            appointments =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, 1);
        } else if ("future".equalsIgnoreCase(condition)) {
            appointments =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, 0);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid condition"));
        }

        List<AppointmentDTO> appointmentDTOs =
                appointments.stream()
                        .map(
                                appointment ->
                                        new AppointmentDTO(
                                                appointment.getId(),
                                                appointment.getDoctor().getId(),
                                                appointment.getDoctor().getName(),
                                                appointment.getPatient().getId(),
                                                appointment.getPatient().getName(),
                                                appointment.getPatient().getEmail(),
                                                appointment.getPatient().getPhone(),
                                                appointment.getPatient().getAddress(),
                                                appointment.getAppointmentTime(),
                                                appointment.getStatus()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("appointments", appointmentDTOs));
    }

    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        List<Appointment> appointments =
                appointmentRepository.findByPatientId(patientId).stream()
                        .filter(
                                appointment ->
                                        appointment.getDoctor() != null
                                                && appointment.getDoctor().getName() != null
                                                && appointment
                                                        .getDoctor()
                                                        .getName()
                                                        .toLowerCase()
                                                        .contains(name.toLowerCase()))
                        .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No appointments found for doctor: " + name));
        }

        List<AppointmentDTO> appointmentDTOs =
                appointments.stream()
                        .map(
                                appointment ->
                                        new AppointmentDTO(
                                                appointment.getId(),
                                                appointment.getDoctor().getId(),
                                                appointment.getDoctor().getName(),
                                                appointment.getPatient().getId(),
                                                appointment.getPatient().getName(),
                                                appointment.getPatient().getEmail(),
                                                appointment.getPatient().getPhone(),
                                                appointment.getPatient().getAddress(),
                                                appointment.getAppointmentTime(),
                                                appointment.getStatus()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("appointments", appointmentDTOs));
    }

    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(
            String condition, String name, Long patientId) {
        List<Appointment> appointments =
                appointmentRepository.findByPatientId(patientId).stream()
                        .filter(
                                appointment ->
                                        appointment.getDoctor() != null
                                                && appointment.getDoctor().getName() != null
                                                && appointment
                                                        .getDoctor()
                                                        .getName()
                                                        .toLowerCase()
                                                        .contains(name.toLowerCase()))
                        .filter(
                                appointment -> {
                                    if ("past".equalsIgnoreCase(condition)) {
                                        return appointment.getStatus() == 1;
                                    } else if ("future".equalsIgnoreCase(condition)) {
                                        return appointment.getStatus() == 0;
                                    }
                                    return false;
                                })
                        .collect(Collectors.toList());

        if (appointments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "error",
                                    "No appointments found for doctor: "
                                            + name
                                            + " with condition: "
                                            + condition));
        }

        List<AppointmentDTO> appointmentDTOs =
                appointments.stream()
                        .map(
                                appointment ->
                                        new AppointmentDTO(
                                                appointment.getId(),
                                                appointment.getDoctor().getId(),
                                                appointment.getDoctor().getName(),
                                                appointment.getPatient().getId(),
                                                appointment.getPatient().getName(),
                                                appointment.getPatient().getEmail(),
                                                appointment.getPatient().getPhone(),
                                                appointment.getPatient().getAddress(),
                                                appointment.getAppointmentTime(),
                                                appointment.getStatus()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("appointments", appointmentDTOs));
    }

    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);

        if (patient == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized access"));
        }

        Map<String, Object> patientDetails = new HashMap<>();
        patientDetails.put("id", patient.getId());
        patientDetails.put("name", patient.getName());
        patientDetails.put("email", patient.getEmail());
        patientDetails.put("phone", patient.getPhone());
        patientDetails.put("address", patient.getAddress());

        return ResponseEntity.ok(Map.of("patient", patientDetails));
    }
}
