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

        // Fetch appointments for the doctor on the given date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59);
        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                        doctorId, startOfDay, endOfDay);

        // Remove booked times from available times
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
            doctorRepository.save(doctor);
            return 1; // Success
        } catch (Exception e) {
            return 0; // Internal error
        }
    }

    public int updateDoctor(Doctor doctor) {
        if (doctorRepository.findById(doctor.getId()).isEmpty()) {
            return -1; // Doctor not found
        }
        try {
            doctorRepository.save(doctor);
            return 1; // Success
        } catch (Exception e) {
            return 0; // Internal error
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    public int deleteDoctor(Long doctorId) {
        if (doctorRepository.findById(doctorId).isEmpty()) {
            return -1; // Doctor not found
        }
        try {
            // Delete all appointments associated with the doctor
            appointmentRepository.deleteAllByDoctorId(doctorId);
            // Delete the doctor
            doctorRepository.deleteById(doctorId);
            return 1; // Success
        } catch (Exception e) {
            return 0; // Internal error
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(login.getEmail());
        if (doctor == null) {
            response.put("error", "Doctor not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
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

    // 1. **Add @Service Annotation**:
    // - This class should be annotated with `@Service` to indicate that it is a
    // service layer class.
    // - The `@Service` annotation marks this class as a Spring-managed bean for
    // business logic.
    // - Instruction: Add `@Service` above the class declaration.

    // 2. **Constructor Injection for Dependencies**:
    // - The `DoctorService` class depends on `DoctorRepository`,
    // `AppointmentRepository`, and `TokenService`.
    // - These dependencies should be injected via the constructor for proper
    // dependency management.
    // - Instruction: Ensure constructor injection is used for injecting
    // dependencies into the service.

    // 3. **Add @Transactional Annotation for Methods that Modify or Fetch Database
    // Data**:
    // - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`,
    // `filterDoctorsBy*` should be annotated with `@Transactional`.
    // - The `@Transactional` annotation ensures that database operations are
    // consistent and wrapped in a single transaction.
    // - Instruction: Add the `@Transactional` annotation above the methods that
    // perform database operations or queries.

    // 4. **getDoctorAvailability Method**:
    // - Retrieves the available time slots for a specific doctor on a particular
    // date and filters out already booked slots.
    // - The method fetches all appointments for the doctor on the given date and
    // calculates the availability by comparing against booked slots.
    // - Instruction: Ensure that the time slots are properly formatted and the
    // available slots are correctly filtered.

    // 5. **saveDoctor Method**:
    // - Used to save a new doctor record in the database after checking if a doctor
    // with the same email already exists.
    // - If a doctor with the same email is found, it returns `-1` to indicate
    // conflict; `1` for success, and `0` for internal errors.
    // - Instruction: Ensure that the method correctly handles conflicts and
    // exceptions when saving a doctor.

    // 6. **updateDoctor Method**:
    // - Updates an existing doctor's details in the database. If the doctor doesn't
    // exist, it returns `-1`.
    // - Instruction: Make sure that the doctor exists before attempting to save the
    // updated record and handle any errors properly.

    // 7. **getDoctors Method**:
    // - Fetches all doctors from the database. It is marked with `@Transactional`
    // to ensure that the collection is properly loaded.
    // - Instruction: Ensure that the collection is eagerly loaded, especially if
    // dealing with lazy-loaded relationships (e.g., available times).

    // 8. **deleteDoctor Method**:
    // - Deletes a doctor from the system along with all appointments associated
    // with that doctor.
    // - It first checks if the doctor exists. If not, it returns `-1`; otherwise,
    // it deletes the doctor and their appointments.
    // - Instruction: Ensure the doctor and their appointments are deleted properly,
    // with error handling for internal issues.

    // 9. **validateDoctor Method**:
    // - Validates a doctor's login by checking if the email and password match an
    // existing doctor record.
    // - It generates a token for the doctor if the login is successful, otherwise
    // returns an error message.
    // - Instruction: Make sure to handle invalid login attempts and password
    // mismatches properly with error responses.

    // 10. **findDoctorByName Method**:
    // - Finds doctors based on partial name matching and returns the list of
    // doctors with their available times.
    // - This method is annotated with `@Transactional` to ensure that the database
    // query and data retrieval are properly managed within a transaction.
    // - Instruction: Ensure that available times are eagerly loaded for the
    // doctors.

    // 11. **filterDoctorsByNameSpecilityandTime Method**:
    // - Filters doctors based on their name, specialty, and availability during a
    // specific time (AM/PM).
    // - The method fetches doctors matching the name and specialty criteria, then
    // filters them based on their availability during the specified time period.
    // - Instruction: Ensure proper filtering based on both the name and specialty
    // as well as the specified time period.

    // 12. **filterDoctorByTime Method**:
    // - Filters a list of doctors based on whether their available times match the
    // specified time period (AM/PM).
    // - This method processes a list of doctors and their available times to return
    // those that fit the time criteria.
    // - Instruction: Ensure that the time filtering logic correctly handles both AM
    // and PM time slots and edge cases.

    // 13. **filterDoctorByNameAndTime Method**:
    // - Filters doctors based on their name and the specified time period (AM/PM).
    // - Fetches doctors based on partial name matching and filters the results to
    // include only those available during the specified time period.
    // - Instruction: Ensure that the method correctly filters doctors based on the
    // given name and time of day (AM/PM).

    // 14. **filterDoctorByNameAndSpecility Method**:
    // - Filters doctors by name and specialty.
    // - It ensures that the resulting list of doctors matches both the name
    // (case-insensitive) and the specified specialty.
    // - Instruction: Ensure that both name and specialty are considered when
    // filtering doctors.

    // 15. **filterDoctorByTimeAndSpecility Method**:
    // - Filters doctors based on their specialty and availability during a specific
    // time period (AM/PM).
    // - Fetches doctors based on the specified specialty and filters them based on
    // their available time slots for AM/PM.
    // - Instruction: Ensure the time filtering is accurately applied based on the
    // given specialty and time period (AM/PM).

    // 16. **filterDoctorBySpecility Method**:
    // - Filters doctors based on their specialty.
    // - This method fetches all doctors matching the specified specialty and
    // returns them.
    // - Instruction: Make sure the filtering logic works for case-insensitive
    // specialty matching.

    // 17. **filterDoctorsByTime Method**:
    // - Filters all doctors based on their availability during a specific time
    // period (AM/PM).
    // - The method checks all doctors' available times and returns those available
    // during the specified time period.
    // - Instruction: Ensure proper filtering logic to handle AM/PM time periods.

}
