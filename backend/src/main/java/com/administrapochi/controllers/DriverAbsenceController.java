package com.administrapochi.controllers;

import com.administrapochi.models.dtos.driverabsences.DriverAbsenceCreateRequest;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceUpdateRequest;
import com.administrapochi.services.DriverAbsenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/drivers/{driverId}/absences")
@RequiredArgsConstructor
public class DriverAbsenceController {

    private final DriverAbsenceService driverAbsenceService;

    @PostMapping
    public ResponseEntity<DriverAbsenceResponse> register(
            @PathVariable Integer driverId, @Valid @RequestBody DriverAbsenceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverAbsenceService.register(driverId, request));
    }

    @PutMapping("/{absenceId}")
    public ResponseEntity<DriverAbsenceResponse> update(
            @PathVariable Integer driverId,
            @PathVariable Integer absenceId,
            @Valid @RequestBody DriverAbsenceUpdateRequest request) {
        return ResponseEntity.ok(driverAbsenceService.update(driverId, absenceId, request));
    }

    @GetMapping
    public ResponseEntity<List<DriverAbsenceResponse>> find(
            @PathVariable Integer driverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            return ResponseEntity.ok(driverAbsenceService.findByDriver(driverId));
        }
        return ResponseEntity.ok(driverAbsenceService.findCoveringDate(driverId, date));
    }
}
