package com.administrapochi.controllers;

import com.administrapochi.models.dtos.driverlicenses.DriverLicenseCreateRequest;
import com.administrapochi.models.dtos.driverlicenses.DriverLicenseCurrentRequest;
import com.administrapochi.models.dtos.driverlicenses.DriverLicenseResponse;
import com.administrapochi.services.DriverLicenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers/{driverId}/licenses")
@RequiredArgsConstructor
public class DriverLicenseController {

    private final DriverLicenseService driverLicenseService;

    @PostMapping
    public ResponseEntity<DriverLicenseResponse> register(
            @PathVariable Integer driverId, @Valid @RequestBody DriverLicenseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverLicenseService.register(driverId, request));
    }

    @GetMapping("/current")
    public ResponseEntity<DriverLicenseResponse> findCurrent(@PathVariable Integer driverId) {
        return ResponseEntity.ok(driverLicenseService.findCurrent(driverId));
    }

    @GetMapping
    public ResponseEntity<List<DriverLicenseResponse>> findHistory(@PathVariable Integer driverId) {
        return ResponseEntity.ok(driverLicenseService.findHistory(driverId));
    }

    @PatchMapping("/{licenseId}/current")
    public ResponseEntity<DriverLicenseResponse> markNonCurrent(
            @PathVariable Integer driverId,
            @PathVariable Integer licenseId,
            @Valid @RequestBody DriverLicenseCurrentRequest request) {
        return ResponseEntity.ok(driverLicenseService.markNonCurrent(driverId, licenseId));
    }
}
