package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.project.back_end.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

class JwtConfigurationTest {
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withInitializer(
                            ctx -> {
                                ctx.getEnvironment()
                                        .getPropertySources()
                                        .remove("systemEnvironment");
                                ctx.getEnvironment()
                                        .getPropertySources()
                                        .remove("systemProperties");
                            })
                    .withBean(
                            PropertySourcesPlaceholderConfigurer.class,
                            PropertySourcesPlaceholderConfigurer::new)
                    .withBean(
                            TokenService.class,
                            () ->
                                    new TokenService(
                                            mock(AdminRepository.class),
                                            mock(DoctorRepository.class),
                                            mock(PatientRepository.class)))
                    .withPropertyValues("jwt.secret=${JWT_SECRET}");

    @Test
    void missingSigningKeyPreventsStartup() {
        context.run(ctx -> assertNotNull(ctx.getStartupFailure()));
    }

    @Test
    void providedSigningKeyAllowsTokenSigning() {
        context.withPropertyValues("JWT_SECRET=test-only-key-with-more-than-thirty-two-characters")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            TokenService tokens = ctx.getBean(TokenService.class);
                            assertEquals(
                                    "patient@example.com",
                                    tokens.extractEmail(
                                            tokens.generateToken(
                                                    1L, "patient", "patient@example.com")));
                        });
    }
}
