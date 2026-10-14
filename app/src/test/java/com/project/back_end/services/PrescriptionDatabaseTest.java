package com.project.back_end.services;

import com.project.back_end.repo.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class PrescriptionDatabaseTest {
    @Autowired private PrescriptionRepository prescriptions;

    @Test void configuredApplicationAccountCanReadPrescriptions() {
        assertNotNull(prescriptions.findByAppointmentId(-1L));
    }
}
