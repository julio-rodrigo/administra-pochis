package com.administrapochi.controllers;

import com.administrapochi.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.dtos.driverlicenses.DriverLicenseResponse;
import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.services.DriverAbsenceService;
import com.administrapochi.services.DriverLicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverChildControllersTest {

    @Mock
    private DriverLicenseService driverLicenseService;

    @Mock
    private DriverAbsenceService driverAbsenceService;

    private MockMvc licenseMockMvc;
    private MockMvc absenceMockMvc;

    @BeforeEach
    void setUp() {
        licenseMockMvc = MockMvcBuilders.standaloneSetup(new DriverLicenseController(driverLicenseService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        absenceMockMvc = MockMvcBuilders.standaloneSetup(new DriverAbsenceController(driverAbsenceService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void patchSetsLicenseNonCurrentByLicenseIdAndRejectsTrue() throws Exception {
        when(driverLicenseService.markNonCurrent(1, 7))
                .thenReturn(DriverLicenseResponse.builder().id(7).currentLicense(false).build());

        licenseMockMvc.perform(patch("/api/drivers/1/licenses/7/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentLicense\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.currentLicense").value(false));

        licenseMockMvc.perform(patch("/api/drivers/1/licenses/7/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentLicense\":true}"))
                .andExpect(status().isBadRequest());

        verify(driverLicenseService).markNonCurrent(1, 7);
    }

    @Test
    void putUpdatesAbsenceFields() throws Exception {
        when(driverAbsenceService.update(org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(9), org.mockito.ArgumentMatchers.any()))
                .thenReturn(DriverAbsenceResponse.builder()
                        .id(9)
                        .startDate(LocalDate.of(2026, 11, 10))
                        .endDate(LocalDate.of(2026, 11, 12))
                        .reason("Medical")
                        .notes("Updated")
                        .build());

        absenceMockMvc.perform(put("/api/drivers/1/absences/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-11-10",
                                  "endDate": "2026-11-12",
                                  "reason": "Medical",
                                  "notes": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.reason").value("Medical"));

        verify(driverAbsenceService).update(
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(9),
                org.mockito.ArgumentMatchers.any());
    }
}
