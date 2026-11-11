package com.administrapochi.controllers;

import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleValidToUpdateRequest;
import com.administrapochi.services.SettlementRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SettlementRuleControllerTest {

    @Mock
    private SettlementRuleService settlementRuleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SettlementRuleController(settlementRuleService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void patchUpdatesValidToAndReturnsRule() throws Exception {
        LocalDate validTo = LocalDate.of(2026, 11, 30);
        when(settlementRuleService.updateValidTo(eq(12), any()))
                .thenReturn(SettlementRuleResponse.builder()
                        .id(12)
                        .shiftCount(2)
                        .validTo(validTo)
                        .build());

        mockMvc.perform(patch("/api/settlement-rules/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "validTo": "2026-11-30"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.validTo").value("2026-11-30"))
                .andExpect(jsonPath("$.shiftCount").value(2));

        ArgumentCaptor<SettlementRuleValidToUpdateRequest> captor =
                ArgumentCaptor.forClass(SettlementRuleValidToUpdateRequest.class);
        verify(settlementRuleService).updateValidTo(eq(12), captor.capture());
        assertEquals(validTo, captor.getValue().getValidTo());
    }

    @Test
    void patchRequiresValidTo() throws Exception {
        mockMvc.perform(patch("/api/settlement-rules/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.validTo").exists());

        verifyNoInteractions(settlementRuleService);
    }
}
