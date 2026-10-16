package com.project.back_end.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.MockMakers;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PrescriptionControllerTest {
    private final PrescriptionService service =
            mock(PrescriptionService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AdminService auth =
            mock(AdminService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(new PrescriptionController(service, auth))
                        .setControllerAdvice(new ValidationFailed())
                        .build();
        when(auth.validateToken("token", "doctor")).thenReturn(true);
    }

    private String payload(String name, String medication, String dosage, long id) {
        return "{\"patientName\":\""
                + name
                + "\",\"medication\":\""
                + medication
                + "\",\"dosage\":\""
                + dosage
                + "\",\"appointmentId\":"
                + id
                + "}";
    }

    @Test
    void saveRouteReturnsCreated() throws Exception {
        when(service.savePrescription(any(Prescription.class), eq("token")))
                .thenReturn(
                        ResponseEntity.status(201)
                                .body(Map.of("message", "Prescription saved successfully")));
        mvc.perform(
                        post("/prescription/save/token")
                                .contentType("application/json")
                                .content(payload("Patient", "Medicine", "Daily", 7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Prescription saved successfully"));
    }

    @Test
    void rejectsBlankFieldsAndNonPositiveAppointmentIds() throws Exception {
        for (String body :
                new String[] {
                    payload("   ", "Medicine", "Daily", 7),
                    payload("Patient", "   ", "Daily", 7),
                    payload("Patient", "Medicine", "", 7),
                    payload("Patient", "Medicine", "Daily", 0)
                }) {
            mvc.perform(
                            post("/prescription/save/token")
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsInvalidTokenWithJsonMessage() throws Exception {
        mvc.perform(
                        post("/prescription/save/invalid")
                                .contentType("application/json")
                                .content(payload("Patient", "Medicine", "Daily", 7)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        mvc.perform(get("/prescription/7/invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void patientRouteAllowsReadAndRejectsWrite() throws Exception {
        when(auth.validateToken("patient-token", "patient")).thenReturn(true);
        when(service.getPatientPrescription(7L, "patient-token"))
                .thenReturn(
                        ResponseEntity.ok(
                                Map.of(
                                        "prescriptions",
                                        java.util.List.of(Map.of("dosage", "Once daily")))));
        mvc.perform(get("/prescription/patient/7/patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptions[0].dosage").value("Once daily"));
        mvc.perform(
                        post("/prescription/save/patient-token")
                                .contentType("application/json")
                                .content(payload("Patient", "Medicine", "Daily", 7)))
                .andExpect(status().isUnauthorized());
        verify(service, never()).savePrescription(any(), any());
    }

    @Test
    void patientRouteRejectsInvalidToken() throws Exception {
        mvc.perform(get("/prescription/patient/7/invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
