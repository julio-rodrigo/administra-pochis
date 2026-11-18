package com.administrapochi.controllers;

import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.exceptions.InvalidPochiDailyStatusException;
import com.administrapochi.exceptions.PochiDailyStatusConflictException;
import com.administrapochi.exceptions.PochiDailyStatusNotFoundException;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusResponse;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.services.PochiDailyStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PochiDailyStatusControllerTest {

    @Mock private PochiDailyStatusService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PochiDailyStatusController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void createsStatusAndIgnoresClientAuditFields() throws Exception {
        when(service.create(any())).thenReturn(PochiDailyStatusResponse.builder()
                .id(7).pochiId(82).status(PochiDailyStatusType.DAY_OFF).createdBy(2).build());
        mockMvc.perform(post("/api/pochi-daily-statuses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pochiId":82,"statusDate":"2026-10-19","status":"DAY_OFF",
                                 "createdBy":999,"updatedBy":999}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(2))
                .andExpect(jsonPath("$.status").value("DAY_OFF"));
        verify(service).create(any());
    }

    @Test
    void rejectsMissingFieldsAndNonPersistedStatus() throws Exception {
        mockMvc.perform(post("/api/pochi-daily-statuses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DAY_OFF\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pochiId").exists())
                .andExpect(jsonPath("$.errors.statusDate").exists());
        mockMvc.perform(post("/api/pochi-daily-statuses").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pochiId\":82,\"statusDate\":\"2026-10-19\",\"status\":\"WORKED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void translatesBusinessExceptionsAndSupportsDelete() throws Exception {
        when(service.findById(99)).thenThrow(new PochiDailyStatusNotFoundException(99));
        mockMvc.perform(get("/api/pochi-daily-statuses/99")).andExpect(status().isNotFound());
        when(service.create(any())).thenThrow(
                new PochiDailyStatusConflictException("Conflict"),
                new InvalidPochiDailyStatusException("Invalid"));
        String body = "{\"pochiId\":82,\"statusDate\":\"2026-10-19\",\"status\":\"DAY_OFF\"}";
        mockMvc.perform(post("/api/pochi-daily-statuses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Conflict"));
        mockMvc.perform(post("/api/pochi-daily-statuses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Invalid"));
        mockMvc.perform(delete("/api/pochi-daily-statuses/7")).andExpect(status().isNoContent());
        verify(service).delete(7);
    }
}
