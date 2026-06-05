package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {
	private static final DateTimeFormatter SLOT_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private final DoctorRepository doctorRepository;
	private final AppointmentRepository appointmentRepository;
	private final TokenService tokenService;

	public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
			TokenService tokenService) {
		this.doctorRepository = doctorRepository;
		this.appointmentRepository = appointmentRepository;
		this.tokenService = tokenService;
	}

	@Transactional(readOnly = true)
	public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
		Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
		if (doctorOptional.isEmpty()) {
			return List.of();
		}

		Doctor doctor = doctorOptional.get();
		List<String> availableTimes = doctor.getAvailableTimes() != null ? doctor.getAvailableTimes() : List.of();

		LocalDate targetDate = date != null ? date : LocalDate.now();
		LocalDateTime start = targetDate.atStartOfDay();
		LocalDateTime end = targetDate.atTime(LocalTime.MAX);
		Set<String> bookedSlots = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end)
				.stream()
				.map(Appointment::getAppointmentTime)
				.filter(value -> value != null)
				.map(value -> value.toLocalTime().format(SLOT_FORMAT))
				.collect(Collectors.toSet());

		return availableTimes.stream()
				.filter(slot -> !bookedSlots.contains(normalizeSlot(slot)))
				.toList();
	}

	@Transactional
	public int saveDoctor(Doctor doctor) {
		if (doctor == null || doctor.getEmail() == null || doctor.getEmail().isBlank()) {
			return 0;
		}

		try {
			if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
				return -1;
			}
			doctorRepository.save(doctor);
			return 1;
		} catch (RuntimeException exception) {
			return 0;
		}
	}

	@Transactional
	public int updateDoctor(Doctor doctor) {
		if (doctor == null || doctor.getId() == null) {
			return 0;
		}

		Optional<Doctor> existingOptional = doctorRepository.findById(doctor.getId());
		if (existingOptional.isEmpty()) {
			return -1;
		}

		try {
			Doctor existing = existingOptional.get();
			existing.setName(doctor.getName());
			existing.setSpecialty(doctor.getSpecialty());
			existing.setEmail(doctor.getEmail());
			existing.setPassword(doctor.getPassword());
			existing.setPhone(doctor.getPhone());
			existing.setAvailableTimes(doctor.getAvailableTimes());
			doctorRepository.save(existing);
			return 1;
		} catch (RuntimeException exception) {
			return 0;
		}
	}

	@Transactional(readOnly = true)
	public List<Doctor> getDoctors() {
		return doctorRepository.findAll();
	}

	@Transactional
	public int deleteDoctor(long id) {
		if (id <= 0) {
			return 0;
		}

		Optional<Doctor> doctorOptional = doctorRepository.findById(id);
		if (doctorOptional.isEmpty()) {
			return -1;
		}

		try {
			appointmentRepository.deleteAllByDoctorId(id);
			doctorRepository.delete(doctorOptional.get());
			return 1;
		} catch (RuntimeException exception) {
			return 0;
		}
	}

	@Transactional(readOnly = true)
	public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
		if (login == null || login.getIdentifier() == null || login.getIdentifier().isBlank()
				|| login.getPassword() == null || login.getPassword().isBlank()) {
			return buildResponse(HttpStatus.BAD_REQUEST, "message", "Identifier and password are required.");
		}

		Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
		if (doctor == null || !doctor.getPassword().equals(login.getPassword())) {
			return buildResponse(HttpStatus.UNAUTHORIZED, "message", "Invalid credentials.");
		}

		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("message", "Login successful.");
		payload.put("token", "Bearer " + login.getIdentifier());
		payload.put("email", tokenService.extractEmail(payload.get("token")));
		return ResponseEntity.ok(payload);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> findDoctorByName(String name) {
		List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name.trim());
		return buildDoctorListResponse(doctors);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
		List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
				name == null ? "" : name.trim(), specialty == null ? "" : specialty.trim());
		return buildDoctorListResponse(filterDoctorByTime(doctors, amOrPm));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
		List<Doctor> doctors = doctorRepository.findByNameLike(name == null ? "" : name.trim());
		return buildDoctorListResponse(filterDoctorByTime(doctors, amOrPm));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
		List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
				name == null ? "" : name.trim(), specilty == null ? "" : specilty.trim());
		return buildDoctorListResponse(doctors);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
		List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty.trim());
		return buildDoctorListResponse(filterDoctorByTime(doctors, amOrPm));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorBySpecility(String specilty) {
		List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty == null ? "" : specilty.trim());
		return buildDoctorListResponse(doctors);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> filterDoctorsByTime(String amOrPm) {
		return buildDoctorListResponse(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
	}

	private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String key, String value) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put(key, value);
		return ResponseEntity.status(status).body(payload);
	}

	private Map<String, Object> buildDoctorListResponse(List<Doctor> doctors) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("doctors", doctors);
		response.put("count", doctors.size());
		return response;
	}

	private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
		if (doctors == null || doctors.isEmpty()) {
			return List.of();
		}

		if (amOrPm == null || amOrPm.isBlank()) {
			return doctors;
		}

		String meridiem = amOrPm.trim().toUpperCase();
		if (!"AM".equals(meridiem) && !"PM".equals(meridiem)) {
			return doctors;
		}

		return doctors.stream().filter(doctor -> {
			List<String> slots = doctor.getAvailableTimes();
			if (slots == null || slots.isEmpty()) {
				return false;
			}

			return slots.stream().map(this::normalizeSlot).anyMatch(slot -> {
				try {
					LocalTime time = LocalTime.parse(slot, SLOT_FORMAT);
					return "AM".equals(meridiem) ? time.getHour() < 12 : time.getHour() >= 12;
				} catch (DateTimeParseException exception) {
					return false;
				}
			});
		}).toList();
	}

	private String normalizeSlot(String slot) {
		if (slot == null || slot.isBlank()) {
			return "";
		}

		try {
			return LocalTime.parse(slot.trim()).format(SLOT_FORMAT);
		} catch (DateTimeParseException exception) {
			return slot.trim();
		}
	}

}
