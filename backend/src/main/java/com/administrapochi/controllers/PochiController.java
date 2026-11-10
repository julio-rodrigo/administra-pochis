package com.administrapochi.controllers;

import com.administrapochi.models.dtos.pochis.CreatePochiRequest;
import com.administrapochi.models.dtos.pochis.PochiResponse;
import com.administrapochi.models.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.services.PochiService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pochis")
@RequiredArgsConstructor
public class PochiController {

    private final PochiService pochiService;

    @GetMapping
    public ResponseEntity<List<PochiResponse>> findAll() {
        return ResponseEntity.ok(pochiService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PochiResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(pochiService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PochiResponse> create(@Valid @RequestBody CreatePochiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pochiService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PochiResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePochiRequest request) {
        return ResponseEntity.ok(pochiService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PochiResponse> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(pochiService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PochiResponse> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(pochiService.deactivate(id));
    }
}
