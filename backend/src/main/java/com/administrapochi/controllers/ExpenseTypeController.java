package com.administrapochi.controllers;

import com.administrapochi.models.dtos.expensetypes.ExpenseTypeCreateRequest;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeResponse;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeUpdateRequest;
import com.administrapochi.services.ExpenseTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/expense-types")
@RequiredArgsConstructor
public class ExpenseTypeController {

    private final ExpenseTypeService expenseTypeService;

    @GetMapping
    public ResponseEntity<List<ExpenseTypeResponse>> findAll(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(expenseTypeService.findAll(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseTypeResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(expenseTypeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseTypeResponse> create(
            @Valid @RequestBody ExpenseTypeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseTypeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseTypeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody ExpenseTypeUpdateRequest request) {
        return ResponseEntity.ok(expenseTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        expenseTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
