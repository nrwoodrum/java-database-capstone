package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Test
    void generateTokenRejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.generateToken(null, "admin", null));
        assertThrows(IllegalArgumentException.class, () -> tokenService.generateToken(null, "admin", "   "));
    }

    @Test
    void extractEmailRejectsBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.extractEmail(null));
        assertThrows(IllegalArgumentException.class, () -> tokenService.extractEmail("   "));
    }
}
