package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    private final DoctorRepository doctorRepository;

    private final PatientRepository patientRepository;

    private final TokenService tokenService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.tokenService = tokenService;
    }

    private Patient patientFromToken(String token) {
        String email = tokenService.extractEmail(token);
        var patient = patientRepository.findByEmail(email);
        if (patient == null) throw new IllegalArgumentException("Patient not found.");
        return patient;
    }

    @Transactional
    public int bookAppointment(Appointment appointment, String token) {
        if (appointment.getId() != null) {
            throw new IllegalArgumentException(
                    "A new appointment must not include an appointment ID.");
        }
        if (appointment.getAppointmentTime() == null
                || !appointment.getAppointmentTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment time must be in the future.");
        }
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Doctor ID is required.");
        }
        appointment.setDoctor(
                doctorRepository
                        .findById(appointment.getDoctor().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Doctor not found.")));
        appointment.setPatient(patientFromToken(token));
        // Let persistence failures leave this transaction before the controller handles them.
        appointmentRepository.saveAndFlush(appointment);
        return 1;
    }

    @Transactional
    public String updateAppointment(
            Long appointmentId, Appointment updatedAppointment, String token) {
        Appointment existingAppointment =
                appointmentRepository.findById(appointmentId).orElse(null);
        if (existingAppointment == null) {
            return "Appointment not found";
        }
        if (!existingAppointment.getPatient().getId().equals(patientFromToken(token).getId())) {
            return "Patient ID mismatch";
        }
        if (existingAppointment.getStatus() != 0) {
            return "Cannot update an appointment that is not in pending status";
        }
        if (updatedAppointment.getAppointmentTime() == null
                || !updatedAppointment.getAppointmentTime().isAfter(LocalDateTime.now())) {
            return "Appointment time must be in the future";
        }
        List<Appointment> doctorAppointments =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        updatedAppointment.getDoctor().getId(),
                        updatedAppointment.getAppointmentTime().minusHours(1),
                        updatedAppointment.getAppointmentTime().plusHours(1));
        if (!doctorAppointments.isEmpty()) {
            return "Doctor is not available at the specified time";
        }
        existingAppointment.setAppointmentTime(updatedAppointment.getAppointmentTime());
        existingAppointment.setDoctor(updatedAppointment.getDoctor());
        appointmentRepository.save(existingAppointment);
        return "Appointment updated successfully";
    }

    @Transactional
    public String cancelAppointment(Long appointmentId, String token) {
        Appointment existingAppointment =
                appointmentRepository.findById(appointmentId).orElse(null);
        if (existingAppointment == null) {
            return "Appointment not found";
        }
        if (!existingAppointment.getPatient().getId().equals(patientFromToken(token).getId())) {
            return "Patient ID mismatch";
        }
        try {
            appointmentRepository.delete(existingAppointment);
            return "Appointment canceled successfully";
        } catch (Exception e) {
            return "Error occurred while canceling the appointment";
        }
    }

    @Transactional
    public List<Appointment> getAppointments(Long doctorId, LocalDate date, String patientName) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        String normalizedName =
                patientName == null
                                || patientName.isBlank()
                                || "all".equalsIgnoreCase(patientName)
                                || "null".equalsIgnoreCase(patientName)
                        ? null
                        : patientName.trim();

        if (normalizedName == null) {
            return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    doctorId, startOfDay, endOfDay);
        } else {
            return appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctorId, normalizedName, startOfDay, endOfDay);
        }
    }

    @Transactional
    public String changeStatus(Long appointmentId, int status) {
        Appointment existingAppointment =
                appointmentRepository.findById(appointmentId).orElse(null);
        if (existingAppointment == null) {
            return "Appointment not found";
        }
        existingAppointment.setStatus(status);
        appointmentRepository.save(existingAppointment);
        return "Appointment status updated successfully";
    }
}
