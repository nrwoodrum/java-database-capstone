package com.project.back_end.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.project.back_end.services.AdminService;
import com.project.back_end.services.AppointmentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppointmentControllerTest {
    private final AdminService auth =
            mock(AdminService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private final AppointmentService service =
            mock(AppointmentService.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc =
                MockMvcBuilders.standaloneSetup(new AppointmentController(service, auth))
                        .setControllerAdvice(new ValidationFailed())
                        .build();
        when(auth.validateToken("token", "patient")).thenReturn(true);
        when(auth.validateAppointment(any())).thenReturn(1);
        when(service.bookAppointment(any())).thenReturn(1);
    }

    private String body(LocalDateTime time) {
        return "{\"doctor\":{\"id\":1},\"patient\":{\"id\":2},\"appointmentTime\":\""
                + time
                + "\",\"status\":0}";
    }

    @Test
    void rejectsPastTimeBeforeStartingTransaction() throws Exception {
        mvc.perform(
                        post("/appointments/token")
                                .contentType("application/json")
                                .content(body(LocalDateTime.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Appointment time must be in the future"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingPatientBeforeStartingTransaction() throws Exception {
        mvc.perform(
                        post("/appointments/token")
                                .contentType("application/json")
                                .content(
                                        "{\"doctor\":{\"id\":1},\"appointmentTime\":\""
                                                + LocalDateTime.now().plusDays(1)
                                                + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsCreatedForFutureAppointment() throws Exception {
        mvc.perform(
                        post("/appointments/token")
                                .contentType("application/json")
                                .content(body(LocalDateTime.now().plusDays(1))))
                .andExpect(status().isCreated());
    }

    @Test
    void handlesPersistenceFailureAfterServiceTransactionEnds() throws Exception {
        when(service.bookAppointment(any()))
                .thenThrow(new DataIntegrityViolationException("private database detail"));
        mvc.perform(
                        post("/appointments/token")
                                .contentType("application/json")
                                .content(body(LocalDateTime.now().plusDays(1))))
                .andExpect(status().isConflict())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "private database detail"))));
    }
}
