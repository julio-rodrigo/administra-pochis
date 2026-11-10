package com.administrapochi.controllers;

import com.administrapochi.dtos.drivers.DriverResponse;
import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.models.BloodType;
import com.administrapochi.models.DriverStatus;
import com.administrapochi.services.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverService driverService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DriverController(driverService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedDriver() throws Exception {
        when(driverService.create(any())).thenReturn(response());

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alex",
                                  "firstLastName": "Driver",
                                  "curp": "ABCD123456EFGHIJKL",
                                  "bloodType": "O_POSITIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Alex Driver"));

        verify(driverService).create(any());
    }

    @Test
    void createRejectsInvalidCurpBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Alex",
                                  "firstLastName": "Driver",
                                  "curp": "TOO-SHORT",
                                  "bloodType": "O_POSITIVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.curp").exists());
    }

    @Test
    void listStatusChangeAndDeleteDelegateToService() throws Exception {
        when(driverService.findAll()).thenReturn(List.of(response()));
        when(driverService.findDeleted()).thenReturn(List.of(response()));
        when(driverService.changeEmploymentStatus(1, DriverStatus.INACTIVE)).thenReturn(response());
        when(driverService.activate(1)).thenReturn(response(DriverStatus.ACTIVE));
        when(driverService.deactivate(1)).thenReturn(response(DriverStatus.INACTIVE));
        when(driverService.restore(1)).thenReturn(response());

        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bloodType").value("O_POSITIVE"));
        mockMvc.perform(get("/api/drivers/deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        mockMvc.perform(patch("/api/drivers/1/employment-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employmentStatus\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/drivers/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
        mockMvc.perform(patch("/api/drivers/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("INACTIVE"));
        mockMvc.perform(patch("/api/drivers/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(delete("/api/drivers/1")).andExpect(status().isNoContent());

        verify(driverService).changeEmploymentStatus(1, DriverStatus.INACTIVE);
        verify(driverService).activate(1);
        verify(driverService).deactivate(1);
        verify(driverService).restore(1);
        verify(driverService).delete(1);
    }

    @Test
    void notFoundUsesCentralizedProblemResponse() throws Exception {
        when(driverService.findById(99)).thenThrow(new DriverNotFoundException(99));

        mockMvc.perform(get("/api/drivers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Driver not found with id: 99"));
    }

    private DriverResponse response() {
        return response(DriverStatus.ACTIVE);
    }

    private DriverResponse response(DriverStatus status) {
        return DriverResponse.builder()
                .id(1)
                .firstName("Alex")
                .firstLastName("Driver")
                .fullName("Alex Driver")
                .curp("ABCD123456EFGHIJKL")
                .bloodType(BloodType.O_POSITIVE)
                .employmentStatus(status)
                .build();
    }
}
