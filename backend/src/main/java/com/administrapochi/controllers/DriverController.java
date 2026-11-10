package com.administrapochi.controllers;

import com.administrapochi.dtos.drivers.DriverCreateRequest;
import com.administrapochi.dtos.drivers.DriverResponse;
import com.administrapochi.dtos.drivers.DriverStatusUpdateRequest;
import com.administrapochi.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.services.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    public ResponseEntity<List<DriverResponse>> findAll() {
        return ResponseEntity.ok(driverService.findAll());
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<DriverResponse>> findDeleted() {
        return ResponseEntity.ok(driverService.findDeleted());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(driverService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriverResponse> update(
            @PathVariable Integer id, @Valid @RequestBody DriverUpdateRequest request) {
        return ResponseEntity.ok(driverService.update(id, request));
    }

    @PatchMapping("/{id}/employment-status")
    public ResponseEntity<DriverResponse> changeEmploymentStatus(
            @PathVariable Integer id, @Valid @RequestBody DriverStatusUpdateRequest request) {
        return ResponseEntity.ok(driverService.changeEmploymentStatus(id, request.getEmploymentStatus()));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<DriverResponse> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(driverService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<DriverResponse> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(driverService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<DriverResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(driverService.restore(id));
    }
}
