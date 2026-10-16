package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public List<LocalTime> getDoctorAvailability(Long doctorId, LocalDate date) {
        List<LocalTime> availableTimes = new ArrayList<>();
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getAvailableTimes() == null) {
            return availableTimes;
        }
        for (String slot : doctor.getAvailableTimes()) {
            availableTimes.add(LocalTime.parse(slot.split("-", 2)[0].trim()));
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59);
        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        doctorId, startOfDay, endOfDay);

        for (Appointment appointment : appointments) {
            availableTimes.remove(appointment.getAppointmentTime().toLocalTime());
        }

        return availableTimes;
    }

    public int saveDoctor(Doctor doctor) {
        if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
            return -1; // Doctor with the same email already exists
        }
        try {
            // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
            // through Spring Security PasswordEncoder and verify with matches(), never equals().
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public int updateDoctor(Doctor doctor) {
        if (doctorRepository.findById(doctor.getId()).isEmpty()) {
            return -1;
        }
        try {
            // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
            // through Spring Security PasswordEncoder and verify with matches(), never equals().
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    public int deleteDoctor(Long doctorId) {
        if (doctorRepository.findById(doctorId).isEmpty()) {
            return -1;
        }
        try {
            appointmentRepository.deleteAllByDoctorId(doctorId);
            doctorRepository.deleteById(doctorId);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(login.getEmail());
        if (doctor == null) {
            response.put("error", "Doctor not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        // Plaintext passwords keep the local demo simple. Production must hash with BCrypt
        // through Spring Security PasswordEncoder and verify with matches(), never equals().
        if (!doctor.getPassword().equals(login.getPassword())) {
            response.put("error", "Invalid password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        String token =
                tokenService.generateToken(
                        String.valueOf(doctor.getId()), "doctor", doctor.getEmail());
        response.put("token", token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public Map<String, Object> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : doctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }

    public Map<String, Object> filterDoctorsByNameSpecialtyAndTime(
            String name, String specialty, String amOrPm) {
        String normalizedName =
                (name == null || name.equalsIgnoreCase("null") || name.isBlank())
                        ? ""
                        : name.trim();
        String normalizedSpecialty =
                (specialty == null || specialty.equalsIgnoreCase("null") || specialty.isBlank())
                        ? ""
                        : specialty.trim();
        String normalizedTime =
                (amOrPm == null || amOrPm.equalsIgnoreCase("null") || amOrPm.isBlank())
                        ? ""
                        : amOrPm.trim();

        List<Doctor> allDoctors = doctorRepository.findAll();
        List<Doctor> filteredDoctors = new ArrayList<>();

        for (Doctor doctor : allDoctors) {
            boolean matchesName =
                    normalizedName.isEmpty()
                            || doctor.getName()
                                    .toLowerCase()
                                    .contains(normalizedName.toLowerCase());
            boolean matchesSpecialty =
                    normalizedSpecialty.isEmpty()
                            || doctor.getSpecialty().equalsIgnoreCase(normalizedSpecialty);
            boolean matchesTime =
                    normalizedTime.isEmpty() || hasAvailableTimeInRange(doctor, normalizedTime);

            if (matchesName && matchesSpecialty && matchesTime) {
                filteredDoctors.add(doctor);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("doctors", filteredDoctors);
        return result;
    }

    private boolean hasAvailableTimeInRange(Doctor doctor, String amOrPm) {
        List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
        for (LocalTime time : availableTimes) {
            if ((amOrPm.equalsIgnoreCase("AM") && time.isBefore(LocalTime.NOON))
                    || (amOrPm.equalsIgnoreCase("PM") && !time.isBefore(LocalTime.NOON))) {
                return true;
            }
        }
        return false;
    }

    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        List<Doctor> filteredDoctors = new ArrayList<>();
        for (Doctor doctor : doctors) {
            if (hasAvailableTimeInRange(doctor, amOrPm)) {
                filteredDoctors.add(doctor);
            }
        }
        return filteredDoctors;
    }

    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        List<Doctor> doctors = doctorRepository.findAll();
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : filteredDoctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }

    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : filteredDoctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }

    public Map<String, Object> filterDoctorByNameAndSpecialty(String name, String specialty) {
        List<Doctor> doctors =
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
                        name, specialty);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : doctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }

    public Map<String, Object> filterDoctorByTimeAndSpecialty(String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : filteredDoctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }

    public Map<String, Object> filterDoctorBySpecialty(String specialty) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        Map<String, Object> result = new HashMap<>();
        for (Doctor doctor : doctors) {
            List<LocalTime> availableTimes = getDoctorAvailability(doctor.getId(), LocalDate.now());
            result.put(doctor.getName(), availableTimes);
        }
        return result;
    }
}
