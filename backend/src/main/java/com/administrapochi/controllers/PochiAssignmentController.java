package com.administrapochi.controllers;

import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentUpdateRequest;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.services.PochiAssignmentService;
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
import java.util.List;

@RestController
@RequestMapping("/api/pochi-assignments")
@RequiredArgsConstructor
public class PochiAssignmentController {

    private final PochiAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<List<PochiAssignmentResponse>> create(
            @Valid @RequestBody PochiAssignmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PochiAssignmentResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(assignmentService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PochiAssignmentResponse>> findAll(
            @RequestParam(required = false) LocalDate assignmentDate,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer driverId,
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) Shift shift,
            @PageableDefault(size = 20, sort = "assignmentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(assignmentService.findAll(
                assignmentDate, startDate, endDate, driverId, pochiId, shift, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PochiAssignmentResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody PochiAssignmentUpdateRequest request) {
        return ResponseEntity.ok(assignmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
