package com.administrapochi.controllers;

import com.administrapochi.models.dtos.settlements.SettlementCreateRequest;
import com.administrapochi.models.dtos.settlements.SettlementResponse;
import com.administrapochi.models.dtos.settlements.SettlementUpdateRequest;
import com.administrapochi.services.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResponse> create(
            @Valid @RequestBody SettlementCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SettlementResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(settlementService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SettlementResponse>> findAll(
            @RequestParam(required = false) Integer driverId,
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) LocalDate workDate,
            @RequestParam(required = false) LocalDate workDateFrom,
            @RequestParam(required = false) LocalDate workDateTo,
            @PageableDefault(size = 20, sort = "workDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(settlementService.findAll(
                driverId, pochiId, workDate, workDateFrom, workDateTo, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SettlementResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody SettlementUpdateRequest request) {
        return ResponseEntity.ok(settlementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        settlementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
