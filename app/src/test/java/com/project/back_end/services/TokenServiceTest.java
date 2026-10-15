package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private final TokenService tokenService =
            new TokenService(
                    mock(AdminRepository.class),
                    mock(DoctorRepository.class),
                    mock(PatientRepository.class));

    @Test
    void generateTokenRejectsBlankEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateToken(null, "admin", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateToken(null, "admin", "   "));
    }

    @Test
    void extractEmailRejectsBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.extractEmail(null));
        assertThrows(IllegalArgumentException.class, () -> tokenService.extractEmail("   "));
    }

    @Test
    void patientTokenCannotActAsDoctorWithTheSameEmail() {
        DoctorRepository doctors = mock(DoctorRepository.class);
        PatientRepository patients = mock(PatientRepository.class);
        TokenService service = new TokenService(mock(AdminRepository.class), doctors, patients);
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "jwtSecret", "a-test-secret-that-is-long-enough-for-hmac-sha256");
        org.mockito.Mockito.when(doctors.findByEmail("shared@example.com"))
                .thenReturn(new com.project.back_end.models.Doctor());
        org.mockito.Mockito.when(patients.findByEmail("shared@example.com"))
                .thenReturn(new com.project.back_end.models.Patient());
        String token = service.generateToken(1L, "patient", "shared@example.com");
        org.junit.jupiter.api.Assertions.assertTrue(service.validateToken(token, "patient"));
        org.junit.jupiter.api.Assertions.assertFalse(service.validateToken(token, "doctor"));
    }
}
