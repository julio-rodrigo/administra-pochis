package com.administrapochi.controllers;

import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeCreateRequest;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.services.AdministrationChargeService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/administration-charges")
@RequiredArgsConstructor
public class AdministrationChargeController {

    private final AdministrationChargeService administrationChargeService;

    @PostMapping
    public ResponseEntity<AdministrationChargeResponse> create(
            @Valid @RequestBody AdministrationChargeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(administrationChargeService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministrationChargeResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(administrationChargeService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AdministrationChargeResponse>> findAll(
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) LocalDate chargeDate,
            @RequestParam(required = false) LocalDate chargeDateFrom,
            @RequestParam(required = false) LocalDate chargeDateTo,
            @RequestParam(required = false) Integer createdById,
            @RequestParam(required = false) Boolean adjusted,
            @PageableDefault(size = 20, sort = "chargeDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(administrationChargeService.findAll(
                pochiId, chargeDate, chargeDateFrom, chargeDateTo, createdById, adjusted, pageable));
    }
}
