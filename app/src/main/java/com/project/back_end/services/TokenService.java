package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public TokenService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    public String generateToken(String email) {
        return generateToken(null, null, email);
    }

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String extractEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required.");
        }

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token, String user) {
        if (token == null || token.isBlank() || user == null || user.isBlank()) {
            return false;
        }

        try {
            var claims =
                    Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            String claimedRole = claims.get("userType", String.class);
            if (claimedRole != null && !claimedRole.equals(user)) return false;
            String extracted = claims.getSubject();
            if (user.equals("admin")) {
                Admin admin = adminRepository.findByUsername(extracted);
                if (admin != null) {
                    return true;
                }
            } else if (user.equals("doctor")) {
                Doctor doctor = doctorRepository.findByEmail(extracted);
                if (doctor != null) {
                    return true;
                }
            } else if (user.equals("patient")) {
                Patient patient = patientRepository.findByEmail(extracted);
                if (patient != null) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmailFromToken(String token) {
        return extractEmail(token);
    }

    public String generateToken(Object userId, String userType, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required to generate a token.");
        }

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("userType", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 604800000)) // 7 days
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractDoctorIdFromToken(String token) {
        throw new UnsupportedOperationException("Unimplemented method 'extractDoctorIdFromToken'");
    }
}
