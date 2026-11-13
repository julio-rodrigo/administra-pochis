package com.administrapochi.controllers;

import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.services.AdministrationChargeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdministrationChargeControllerTest {

    @Mock
    private AdministrationChargeService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdministrationChargeController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createValidatesRequestAndReturnsReadOnlyResponse() throws Exception {
        when(service.create(any())).thenReturn(AdministrationChargeResponse.builder()
                .id(10)
                .chargeDate(LocalDate.of(2026, 11, 13))
                .suggestedAmount(new BigDecimal("70.00"))
                .amount(new BigDecimal("60.00"))
                .adjusted(true)
                .note("Partial shift")
                .build());

        mockMvc.perform(post("/api/administration-charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pochiId": 35,
                                  "chargeDate": "2026-11-13",
                                  "amount": 60.00,
                                  "note": "Partial shift",
                                  "suggestedAmount": 999.00,
                                  "shiftCount": 2,
                                  "createdById": 100
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.adjusted").value(true))
                .andExpect(jsonPath("$.suggestedAmount").value(70.0));

        org.mockito.ArgumentCaptor<com.administrapochi.models.dtos.administrationcharges
                .AdministrationChargeCreateRequest> captor = org.mockito.ArgumentCaptor.forClass(
                com.administrapochi.models.dtos.administrationcharges.AdministrationChargeCreateRequest.class);
        verify(service).create(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void validatesRequiredValuesBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/administration-charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pochiId").exists())
                .andExpect(jsonPath("$.errors.chargeDate").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
        verifyNoInteractions(service);
    }

    @Test
    void exposesReadByIdAndPagedListEndpoints() throws Exception {
        AdministrationChargeResponse response = AdministrationChargeResponse.builder().id(10).build();
        when(service.findById(10)).thenReturn(response);
        when(service.findAll(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/administration-charges/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        mockMvc.perform(get("/api/administration-charges")
                        .queryParam("pochiId", "35")
                        .queryParam("adjusted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));

        verify(service).findById(10);
        verify(service).findAll(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void exposesNoUpdateOrDeleteControllerMappings() {
        var mappings = Arrays.stream(AdministrationChargeController.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getAnnotations()))
                .map(annotation -> annotation.annotationType())
                .toList();
        assertFalse(mappings.contains(PutMapping.class));
        assertFalse(mappings.contains(PatchMapping.class));
        assertFalse(mappings.contains(DeleteMapping.class));
    }
}
