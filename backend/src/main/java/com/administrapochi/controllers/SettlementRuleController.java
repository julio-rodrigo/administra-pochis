package com.administrapochi.controllers;

import com.administrapochi.models.dtos.settlementrules.SettlementRuleCreateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleUpdateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleValidToUpdateRequest;
import com.administrapochi.services.SettlementRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/settlement-rules")
@RequiredArgsConstructor
public class SettlementRuleController {

    private final SettlementRuleService settlementRuleService;

    @PostMapping
    public ResponseEntity<SettlementRuleResponse> create(
            @Valid @RequestBody SettlementRuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementRuleService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementRuleResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(settlementRuleService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SettlementRuleResponse>> findAll(
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) Integer shiftCount,
            @RequestParam(required = false) LocalDate validOn,
            @PageableDefault(size = 20, sort = "validFrom", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(settlementRuleService.findAll(pochiId, shiftCount, validOn, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SettlementRuleResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody SettlementRuleUpdateRequest request) {
        return ResponseEntity.ok(settlementRuleService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SettlementRuleResponse> updateValidTo(
            @PathVariable Integer id,
            @Valid @RequestBody SettlementRuleValidToUpdateRequest request) {
        return ResponseEntity.ok(settlementRuleService.updateValidTo(id, request));
    }
}
