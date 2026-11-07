package com.administrapochi.controllers;

import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.services.PochiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PochiControllerTest {

    @Mock
    private PochiService pochiService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PochiController(pochiService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedResponse() throws Exception {
        when(pochiService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/pochis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "economicNumber": "P-01",
                                  "licensePlate": "MX-01",
                                  "unionName": "UNTRAC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.economicNumber").value("P-01"));

        verify(pochiService).create(any());
    }

    @Test
    void createRejectsBlankEconomicNumber() throws Exception {
        mockMvc.perform(post("/api/pochis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "economicNumber": "  "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.economicNumber").exists());
    }

    @Test
    void listAndFindByIdReturnResponses() throws Exception {
        when(pochiService.findAll()).thenReturn(List.of(response()));
        when(pochiService.findById(1)).thenReturn(response());

        mockMvc.perform(get("/api/pochis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].economicNumber").value("P-01"));
        mockMvc.perform(get("/api/pochis/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedResponse() throws Exception {
        when(pochiService.update(any(), any())).thenReturn(response());

        mockMvc.perform(put("/api/pochis/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "economicNumber": "P-01",
                                  "licensePlate": "MX-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.economicNumber").value("P-01"));

        verify(pochiService).update(any(), any());
    }

    @Test
    void activateAndDeactivateReturnUpdatedResponses() throws Exception {
        when(pochiService.activate(1)).thenReturn(response());
        when(pochiService.deactivate(1)).thenReturn(response());

        mockMvc.perform(patch("/api/pochis/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/api/pochis/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(pochiService).activate(1);
        verify(pochiService).deactivate(1);
    }

    @Test
    void notFoundExceptionUsesCentralizedErrorResponse() throws Exception {
        when(pochiService.findById(1)).thenThrow(new PochiNotFoundException(1));

        mockMvc.perform(get("/api/pochis/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pochi not found with id: 1"));
    }

    private PochiResponse response() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return PochiResponse.builder()
                .id(1)
                .economicNumber("P-01")
                .licensePlate("MX-01")
                .unionName("UNTRAC")
                .active(true)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
