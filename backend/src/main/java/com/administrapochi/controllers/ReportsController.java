package com.administrapochi.controllers;

import com.administrapochi.models.dtos.reports.WeeklyReportHistoryResponse;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.services.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/weekly")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reports;

    @GetMapping("/{weekYear}/{weekNumber}")
    public WeeklyReportResponse weekly(@PathVariable Integer weekYear, @PathVariable Integer weekNumber,
                                       @RequestParam(required = false) Integer ownerId) {
        return reports.weekly(ownerId, weekYear, weekNumber);
    }

    @GetMapping
    public WeeklyReportResponse weeklyByRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer ownerId) {
        return reports.weeklyByRange(ownerId, startDate, endDate);
    }

    @GetMapping("/history")
    public Page<WeeklyReportHistoryResponse> history(@RequestParam(required = false) Integer ownerId,
            @RequestParam(required = false) Integer weekYear, @RequestParam(required = false) WeeklyClosingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return reports.history(ownerId, weekYear, status, startDate, endDate, pageable);
    }

    @GetMapping("/pending-owner-close")
    public Page<WeeklyReportHistoryResponse> pendingOwnerClose(@RequestParam(required = false) Integer ownerId,
                                                               @PageableDefault(size = 20) Pageable pageable) {
        return reports.pendingOwnerClose(ownerId, pageable);
    }
}
