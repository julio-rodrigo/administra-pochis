package com.administrapochi.controllers;

import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCommentUpdateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.services.WeeklyClosingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weekly-closings")
@RequiredArgsConstructor
public class WeeklyClosingController {

    private final WeeklyClosingService weeklyClosingService;

    @PostMapping
    public ResponseEntity<WeeklyClosingResponse> create(@Valid @RequestBody WeeklyClosingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(weeklyClosingService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeeklyClosingResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(weeklyClosingService.findById(id));
    }

    @PostMapping("/{id}/admin-close")
    public ResponseEntity<WeeklyClosingResponse> closeByAdmin(@PathVariable Integer id) {
        return ResponseEntity.ok(weeklyClosingService.closeByAdmin(id));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<WeeklyClosingResponse> reopen(
            @PathVariable Integer id, @Valid @RequestBody WeeklyClosingReopenRequest request) {
        return ResponseEntity.ok(weeklyClosingService.reopen(id, request));
    }

    @PostMapping("/{id}/owner-close")
    public ResponseEntity<WeeklyClosingResponse> closeByOwner(@PathVariable Integer id) {
        return ResponseEntity.ok(weeklyClosingService.closeByOwner(id));
    }

    @PatchMapping("/{id}/comment")
    public ResponseEntity<WeeklyClosingResponse> updateComment(
            @PathVariable Integer id, @RequestBody WeeklyClosingCommentUpdateRequest request) {
        return ResponseEntity.ok(weeklyClosingService.updateComment(id, request));
    }

    @GetMapping
    public ResponseEntity<List<WeeklyClosingResponse>> findAll(
            @RequestParam(required = false) Integer ownerId,
            @RequestParam(required = false) Integer weekYear,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) WeeklyClosingStatus status) {
        return ResponseEntity.ok(weeklyClosingService.findAll(ownerId, weekYear, weekNumber, status));
    }
}
