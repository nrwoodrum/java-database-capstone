package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenService {
	private final AdminRepository adminRepository;
	private final DoctorRepository doctorRepository;
	private final PatientRepository patientRepository;

	@Value("${jwt.secret}")
	private String jwtSecret;

	private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds

	public TokenService(AdminRepository adminRepository, DoctorRepository doctorRepository,
			PatientRepository patientRepository) {
		this.adminRepository = adminRepository;
		this.doctorRepository = doctorRepository;
		this.patientRepository = patientRepository;
	}

	public String generateToken(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			return null;
		}

		try {
			Date now = new Date();
			Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

			return Jwts.builder()
					.setSubject(identifier.trim())
					.setIssuedAt(now)
					.setExpiration(expiryDate)
					.signWith(getSigningKey(), SignatureAlgorithm.HS512)
					.compact();
		} catch (Exception e) {
			return null;
		}
	}

	public boolean validateToken(String token, String user) {
		String identifier = extractIdentifier(token);
		if (identifier == null || identifier.isBlank() || user == null || user.isBlank()) {
			return false;
		}

		switch (user.trim().toLowerCase()) {
			case "admin":
				return adminRepository.findByUsername(identifier) != null;
			case "doctor":
				return doctorRepository.findByEmail(identifier) != null;
			case "patient":
				return patientRepository.findByEmail(identifier) != null;
			default:
				return false;
		}
	}

	public String extractIdentifier(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}

		try {
			String trimmedToken = token.trim();
			if (trimmedToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
				trimmedToken = trimmedToken.substring(7).trim();
			}

			return Jwts.parserBuilder()
					.setSigningKey(getSigningKey())
					.build()
					.parseClaimsJws(trimmedToken)
					.getBody()
					.getSubject();
		} catch (Exception e) {
			return null;
		}
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String extractEmail(String token) {
		return extractIdentifier(token);
	}

}
