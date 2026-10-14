package com.project.back_end.services;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Prescription;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PrescriptionRepository;

@Service
public class PrescriptionService {
    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);
    private final PrescriptionRepository prescriptions;
    private final AppointmentRepository appointments;
    private final TokenService tokens;
    private final AppointmentService appointmentService;

    public PrescriptionService(PrescriptionRepository prescriptions, AppointmentRepository appointments,
                               TokenService tokens, AppointmentService appointmentService) {
        this.prescriptions = prescriptions;
        this.appointments = appointments;
        this.tokens = tokens;
        this.appointmentService = appointmentService;
    }

    private Appointment authorizedAppointment(Long id, String token) {
        Appointment appointment = appointments.findById(id).orElse(null);
        if (appointment == null) return null;
        if (appointment.getDoctor() == null ||
                !tokens.extractEmail(token).equals(appointment.getDoctor().getEmail())) {
            throw new SecurityException("Appointment belongs to another doctor");
        }
        return appointment;
    }

    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription, String token) {
        boolean inserted = false;
        try {
            Appointment appointment = authorizedAppointment(prescription.getAppointmentId(), token);
            if (appointment == null) return ResponseEntity.status(404).body(Map.of("message", "Appointment not found"));
            if (!prescriptions.findByAppointmentId(appointment.getId()).isEmpty()) {
                return ResponseEntity.status(409).body(Map.of("message", "Prescription already exists for this appointment"));
            }
            if (appointment.getStatus() != 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Appointment is already completed"));
            }
            // Mongo's unique _id makes simultaneous creates conflict without a new index migration.
            // Never accept a caller's document ID or patient name for a new prescription.
            prescription.setId("appointment-" + appointment.getId());
            prescription.setPatientName(appointment.getPatient().getName());
            try {
                prescriptions.insert(prescription);
            } catch (DuplicateKeyException e) {
                return ResponseEntity.status(409).body(Map.of("message", "Prescription already exists for this appointment"));
            }
            inserted = true;
            String statusResult = appointmentService.changeStatus(appointment.getId(), 1);
            if (!"Appointment status updated successfully".equals(statusResult)) {
                throw new IllegalStateException(statusResult);
            }
            return ResponseEntity.status(201).body(Map.of("message", "Prescription saved successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", "You cannot access another doctor's appointment"));
        } catch (Exception e) {
            // MongoDB and MySQL do not share a transaction. Undo our insert if completion fails.
            if (inserted) {
                try { prescriptions.deleteById(prescription.getId()); }
                catch (Exception cleanupError) { log.error("Unable to undo prescription insert", cleanupError); }
            }
            log.error("Error saving prescription", e);
            return ResponseEntity.status(500).body(Map.of("message", "Unable to save prescription. Please try again."));
        }
    }

    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId, String token) {
        try {
            if (authorizedAppointment(appointmentId, token) == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Appointment not found"));
            }
            return ResponseEntity.ok(Map.of("prescriptions", prescriptions.findByAppointmentId(appointmentId)));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", "You cannot access another doctor's appointment"));
        } catch (Exception e) {
            log.error("Error fetching prescription", e);
            return ResponseEntity.status(500).body(Map.of("message", "Unable to fetch prescription. Please try again."));
        }
    }
}
