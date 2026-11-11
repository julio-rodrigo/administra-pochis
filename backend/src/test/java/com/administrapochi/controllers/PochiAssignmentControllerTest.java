package com.administrapochi.controllers;

import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.exceptions.PochiAssignmentConflictException;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.services.PochiAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PochiAssignmentControllerTest {

    @Mock
    private PochiAssignmentService assignmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PochiAssignmentController(assignmentService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createReturnsBothFullDayAssignmentsAndIgnoresClientAuditFields() throws Exception {
        when(assignmentService.create(any())).thenReturn(List.of(
                PochiAssignmentResponse.builder().id(1).shift("MORNING").build(),
                PochiAssignmentResponse.builder().id(2).shift("AFTERNOON").build()));

        mockMvc.perform(post("/api/pochi-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentDate": "2026-11-10",
                                  "pochiId": 82,
                                  "driverId": 3,
                                  "shift": "FULL_DAY",
                                  "createdAt": "2000-01-01T00:00:00",
                                  "createdById": 999
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].shift").value("MORNING"))
                .andExpect(jsonPath("$[1].shift").value("AFTERNOON"));

        ArgumentCaptor<com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest> captor =
                ArgumentCaptor.forClass(
                        com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest.class);
        verify(assignmentService).create(captor.capture());
        assertNull(captor.getValue().getNotes());
    }

    @Test
    void createValidatesRequiredFieldsBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/pochi-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shift\":\"MORNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.assignmentDate").exists())
                .andExpect(jsonPath("$.errors.pochiId").exists())
                .andExpect(jsonPath("$.errors.driverId").exists());

        verifyNoInteractions(assignmentService);
    }

    @Test
    void schedulingConflictUsesCentralizedConflictResponse() throws Exception {
        when(assignmentService.create(any()))
                .thenThrow(new PochiAssignmentConflictException("The selected shift is unavailable."));

        mockMvc.perform(post("/api/pochi-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignmentDate": "2026-11-10",
                                  "pochiId": 82,
                                  "driverId": 3,
                                  "shift": "MORNING"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The selected shift is unavailable."));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/pochi-assignments/1"))
                .andExpect(status().isNoContent());

        verify(assignmentService).delete(1);
    }
}
