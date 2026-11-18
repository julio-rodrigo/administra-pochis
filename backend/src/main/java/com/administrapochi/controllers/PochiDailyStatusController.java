package com.administrapochi.controllers;

import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusCreateRequest;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusResponse;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusUpdateRequest;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.services.PochiDailyStatusService;
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
@RequestMapping("/api/pochi-daily-statuses")
@RequiredArgsConstructor
public class PochiDailyStatusController {

    private final PochiDailyStatusService dailyStatusService;

    @PostMapping
    public ResponseEntity<PochiDailyStatusResponse> create(@Valid @RequestBody PochiDailyStatusCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyStatusService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PochiDailyStatusResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(dailyStatusService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PochiDailyStatusResponse>> findAll(
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) PochiDailyStatusType status,
            @RequestParam(required = false) LocalDate statusDate,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @PageableDefault(size = 20, sort = {"statusDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(dailyStatusService.findAll(pochiId, status, statusDate, dateFrom, dateTo, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PochiDailyStatusResponse> update(
            @PathVariable Integer id, @Valid @RequestBody PochiDailyStatusUpdateRequest request) {
        return ResponseEntity.ok(dailyStatusService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dailyStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
