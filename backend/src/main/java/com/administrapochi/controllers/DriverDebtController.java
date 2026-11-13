package com.administrapochi.controllers;

import com.administrapochi.models.dtos.driverdebts.DriverDebtCancelRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentResponse;
import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.enums.DriverDebtStatus;
import com.administrapochi.services.DriverDebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/driver-debts")
@RequiredArgsConstructor
public class DriverDebtController {

    private final DriverDebtService driverDebtService;

    @PostMapping
    public ResponseEntity<DriverDebtResponse> create(
            @Valid @RequestBody DriverDebtCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverDebtService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverDebtResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(driverDebtService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<DriverDebtResponse>> findAll(
            @RequestParam(required = false) Integer driverId,
            @RequestParam(required = false) Integer settlementId,
            @RequestParam(required = false) DriverDebtStatus status,
            @RequestParam(required = false) LocalDate debtDateFrom,
            @RequestParam(required = false) LocalDate debtDateTo,
            @PageableDefault(size = 20, sort = "debtDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(driverDebtService.findAll(
                driverId, settlementId, status, debtDateFrom, debtDateTo, pageable));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<DriverDebtPaymentResponse> registerPayment(
            @PathVariable Integer id,
            @Valid @RequestBody DriverDebtPaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverDebtService.registerPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<DriverDebtPaymentResponse>> findPayments(@PathVariable Integer id) {
        return ResponseEntity.ok(driverDebtService.findPayments(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<DriverDebtResponse> cancel(
            @PathVariable Integer id,
            @Valid @RequestBody DriverDebtCancelRequest request) {
        return ResponseEntity.ok(driverDebtService.cancel(id, request.getCancellationReason()));
    }
}
