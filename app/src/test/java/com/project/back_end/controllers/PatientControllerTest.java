package com.project.back_end.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project.back_end.services.AdminService;
import com.project.back_end.services.PatientService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PatientControllerTest {
    private PatientService patientService;
    private AdminService adminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        patientService = mock(PatientService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        adminService = mock(AdminService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        mockMvc =
                MockMvcBuilders.standaloneSetup(new PatientController(patientService, adminService))
                        .build();
    }

    @Test
    void profileIsAvailableAtTheTopLevelForBooking() throws Exception {
        when(adminService.validateToken("valid-token", "patient")).thenReturn(true);
        when(patientService.getPatientDetails("valid-token"))
                .thenReturn(
                        ResponseEntity.ok(
                                Map.of("patient", Map.of("id", 7L, "name", "Test Patient"))));

        mockMvc.perform(get("/patient/valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.id").value(7))
                .andExpect(jsonPath("$.patient.name").value("Test Patient"))
                .andExpect(jsonPath("$.body").doesNotExist());
    }

    @Test
    void invalidTokenIsRejectedBeforeLoadingProfile() throws Exception {
        mockMvc.perform(get("/patient/invalid-token")).andExpect(status().isUnauthorized());

        verifyNoInteractions(patientService);
    }

    @Test
    void profileLookupFailurePreservesServiceStatus() throws Exception {
        when(adminService.validateToken("valid-token", "patient")).thenReturn(true);
        when(patientService.getPatientDetails("valid-token"))
                .thenReturn(
                        ResponseEntity.status(401).body(Map.of("error", "Unauthorized access")));

        mockMvc.perform(get("/patient/valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized access"));
    }
}
