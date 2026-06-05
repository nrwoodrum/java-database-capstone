package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class AppointmentService {
	private static final int APPOINTMENT_DURATION_HOURS = 1;

	private final AppointmentRepository appointmentRepository;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;
	private final TokenService tokenService;

	public AppointmentService(AppointmentRepository appointmentRepository, DoctorRepository doctorRepository,
			PatientRepository patientRepository, TokenService tokenService) {
		this.appointmentRepository = appointmentRepository;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
		this.tokenService = tokenService;
	}

	@Transactional
	public int bookAppointment(Appointment appointment) {
		try {
			Appointment hydratedAppointment = hydrateAppointment(appointment);
			if (hydratedAppointment == null || !isAppointmentValid(null, hydratedAppointment)) {
				return 0;
			}
			appointmentRepository.save(hydratedAppointment);
			return 1;
		} catch (RuntimeException exception) {
			return 0;
		}
	}

	@Transactional
	public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
		if (appointment == null || appointment.getId() == null) {
			return buildMessageResponse(HttpStatus.BAD_REQUEST, "message", "Appointment id is required.");
		}

		Optional<Appointment> existingOptional = appointmentRepository.findById(appointment.getId());
		if (existingOptional.isEmpty()) {
			return buildMessageResponse(HttpStatus.NOT_FOUND, "message", "Appointment not found.");
		}

		try {
			Appointment existing = existingOptional.get();
			Appointment hydratedAppointment = hydrateAppointment(appointment);
			if (hydratedAppointment == null) {
				return buildMessageResponse(HttpStatus.BAD_REQUEST, "message", "Doctor or patient not found.");
			}
			if (!existing.getPatient().getId().equals(hydratedAppointment.getPatient().getId())) {
				return buildMessageResponse(HttpStatus.FORBIDDEN, "message", "Patient does not own this appointment.");
			}
			if (!isAppointmentValid(existing.getId(), hydratedAppointment)) {
				return buildMessageResponse(HttpStatus.BAD_REQUEST, "message", "Appointment slot is not available.");
			}

			existing.setDoctor(hydratedAppointment.getDoctor());
			existing.setPatient(hydratedAppointment.getPatient());
			existing.setAppointmentTime(hydratedAppointment.getAppointmentTime());
			existing.setStatus(hydratedAppointment.getStatus());
			appointmentRepository.save(existing);
			return buildMessageResponse(HttpStatus.OK, "message", "Appointment updated successfully.");
		} catch (RuntimeException exception) {
			return buildMessageResponse(HttpStatus.INTERNAL_SERVER_ERROR, "message", "Unable to update appointment.");
		}
	}

	@Transactional
	public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
		Optional<Patient> patientOptional = findPatientByToken(token);
		if (patientOptional.isEmpty()) {
			return buildMessageResponse(HttpStatus.UNAUTHORIZED, "message", "Invalid patient token.");
		}

		Optional<Appointment> existingOptional = appointmentRepository.findById(id);
		if (existingOptional.isEmpty()) {
			return buildMessageResponse(HttpStatus.NOT_FOUND, "message", "Appointment not found.");
		}

		Patient patient = patientOptional.get();
		Appointment appointment = existingOptional.get();
		if (!appointment.getPatient().getId().equals(patient.getId())) {
			return buildMessageResponse(HttpStatus.FORBIDDEN, "message", "Patient does not own this appointment.");
		}

		appointmentRepository.delete(appointment);
		return buildMessageResponse(HttpStatus.OK, "message", "Appointment canceled successfully.");
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
		Map<String, Object> response = new LinkedHashMap<>();
		Optional<Doctor> doctorOptional = findDoctorByToken(token);
		if (doctorOptional.isEmpty()) {
			response.put("message", "Invalid doctor token.");
			response.put("appointments", List.of());
			return response;
		}

		LocalDate targetDate = date != null ? date : LocalDate.now();
		LocalDateTime start = targetDate.atStartOfDay();
		LocalDateTime end = targetDate.atTime(LocalTime.MAX);

		List<Appointment> appointments;
		Long doctorId = doctorOptional.get().getId();
		if (pname != null && !pname.isBlank()) {
			appointments = appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
					doctorId, pname.trim(), start, end);
		} else {
			appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
		}

		response.put("appointments", appointments.stream().map(AppointmentDTO::new).toList());
		return response;
	}

	@Transactional
	public boolean changeStatus(Long appointmentId, int status) {
		Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
		if (appointmentOptional.isEmpty()) {
			return false;
		}

		Appointment appointment = appointmentOptional.get();
		appointment.setStatus(status);
		appointmentRepository.save(appointment);
		return true;
	}

	private Appointment hydrateAppointment(Appointment appointment) {
		if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
				|| appointment.getPatient() == null || appointment.getPatient().getId() == null
				|| appointment.getAppointmentTime() == null) {
			return null;
		}

		Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
		Optional<Patient> patientOptional = patientRepository.findById(appointment.getPatient().getId());
		if (doctorOptional.isEmpty() || patientOptional.isEmpty()) {
			return null;
		}

		Appointment hydratedAppointment = new Appointment();
		hydratedAppointment.setId(appointment.getId());
		hydratedAppointment.setDoctor(doctorOptional.get());
		hydratedAppointment.setPatient(patientOptional.get());
		hydratedAppointment.setAppointmentTime(appointment.getAppointmentTime());
		hydratedAppointment.setStatus(appointment.getStatus());
		return hydratedAppointment;
	}

	private ResponseEntity<Map<String, String>> buildMessageResponse(HttpStatus status, String key, String value) {
		Map<String, String> response = new LinkedHashMap<>();
		response.put(key, value);
		return ResponseEntity.status(status).body(response);
	}

	private Optional<Doctor> findDoctorByToken(String token) {
		String email = tokenService.extractEmail(token);
		if (email == null || email.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(doctorRepository.findByEmail(email));
	}

	private Optional<Patient> findPatientByToken(String token) {
		String email = tokenService.extractEmail(token);
		if (email == null || email.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(patientRepository.findByEmail(email));
	}

	private boolean isAppointmentValid(Long appointmentId, Appointment appointment) {
		return appointment != null
				&& appointment.getDoctor() != null
				&& appointment.getDoctor().getId() != null
				&& appointment.getPatient() != null
				&& appointment.getPatient().getId() != null
				&& appointment.getAppointmentTime() != null
				&& !hasConflict(appointmentId, appointment.getDoctor().getId(), appointment.getAppointmentTime());
	}

	private boolean hasConflict(Long appointmentId, Long doctorId, LocalDateTime appointmentTime) {
		LocalDateTime start = appointmentTime.minusHours(APPOINTMENT_DURATION_HOURS).plusMinutes(1);
		LocalDateTime end = appointmentTime.plusHours(APPOINTMENT_DURATION_HOURS).minusMinutes(1);

		return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end).stream()
				.anyMatch(existing -> appointmentId == null || !existing.getId().equals(appointmentId));
	}

}
