package com.administrapochi.controllers;

import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.services.AdministrationFeeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/administration-fee-rules")
@RequiredArgsConstructor
public class AdministrationFeeRuleController {

    private final AdministrationFeeRuleService ruleService;

    @PostMapping
    public ResponseEntity<AdministrationFeeRuleResponse> create(
            @Valid @RequestBody AdministrationFeeRuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdministrationFeeRuleResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody AdministrationFeeRuleUpdateRequest request) {
        return ResponseEntity.ok(ruleService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrationFeeRuleResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ruleService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<AdministrationFeeRuleResponse>> findAll(
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) Boolean global,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) LocalDate validOn) {
        return ResponseEntity.ok(ruleService.findAll(pochiId, global, active, validOn));
    }

    @GetMapping("/applicable")
    public ResponseEntity<AdministrationFeeRuleResponse> findApplicableRule(
            @RequestParam Integer pochiId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ruleService.findApplicableRule(pochiId, date));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AdministrationFeeRuleResponse> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(ruleService.deactivate(id));
    }
}
