package com.hrms.dashboard.controller;

import com.hrms.dashboard.dto.ChartDataPoint;
import com.hrms.dashboard.dto.EmployeeSummaryResponse;
import com.hrms.dashboard.dto.StatisticsResponses.LeaveStatisticsResponse;
import com.hrms.dashboard.dto.StatisticsResponses.MedicalStatisticsResponse;
import com.hrms.dashboard.dto.StatisticsResponses.PfStatisticsResponse;
import com.hrms.dashboard.dto.TimeSeriesPoint;
import com.hrms.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

/**
 * Every list-returning endpoint here is intentionally chart-shape-agnostic: a department
 * breakdown is the same {@code List<ChartDataPoint>} whether the frontend renders it as a
 * bar, pie, or donut chart, and a monthly trend is the same {@code List<TimeSeriesPoint>}
 * whether it's drawn as a line or an area chart. The chart type is a frontend rendering
 * choice, not a different backend shape.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER', 'FINANCE')")
@Tag(name = "Dashboard", description = "Chart-ready aggregated statistics for employees, leave, medical claims, and PF")
public class DashboardController {

    private final DashboardService dashboardService;

    // ---------- Employee charts ----------

    @GetMapping("/employees/summary")
    @Operation(summary = "Headline counters: total / active / inactive employees")
    public ResponseEntity<EmployeeSummaryResponse> employeeSummary() {
        return ResponseEntity.ok(dashboardService.getEmployeeSummary());
    }

    @GetMapping("/employees/by-department")
    @Operation(summary = "Employees by department (bar/pie/donut chart data)")
    public ResponseEntity<List<ChartDataPoint>> employeesByDepartment() {
        return ResponseEntity.ok(dashboardService.getEmployeesByDepartment());
    }

    @GetMapping("/employees/by-gender")
    @Operation(summary = "Employees by gender (bar/pie/donut chart data)")
    public ResponseEntity<List<ChartDataPoint>> employeesByGender() {
        return ResponseEntity.ok(dashboardService.getEmployeesByGender());
    }

    @GetMapping("/employees/by-branch")
    @Operation(summary = "Employees by branch (bar/pie/donut chart data)")
    public ResponseEntity<List<ChartDataPoint>> employeesByBranch() {
        return ResponseEntity.ok(dashboardService.getEmployeesByBranch());
    }

    @GetMapping("/employees/by-status")
    @Operation(summary = "Employees by employment status (bar/pie/donut chart data)")
    public ResponseEntity<List<ChartDataPoint>> employeesByStatus() {
        return ResponseEntity.ok(dashboardService.getEmployeesByStatus());
    }

    @GetMapping("/employees/joining-trend")
    @Operation(summary = "Monthly joining trend for a given year (line/area chart data)")
    public ResponseEntity<List<TimeSeriesPoint>> joiningTrend(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getMonthlyJoiningTrend(year != null ? year : Year.now().getValue()));
    }

    @GetMapping("/employees/resignation-trend")
    @Operation(summary = "Monthly resignation trend for a given year (line/area chart data)")
    public ResponseEntity<List<TimeSeriesPoint>> resignationTrend(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getMonthlyResignationTrend(year != null ? year : Year.now().getValue()));
    }

    // ---------- Leave dashboard ----------

    @GetMapping("/leave/statistics")
    @Operation(summary = "Leave statistics: requests by status, approved days by type, pending count")
    public ResponseEntity<LeaveStatisticsResponse> leaveStatistics(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getLeaveStatistics(year));
    }

    // ---------- Medical dashboard ----------

    @GetMapping("/medical/statistics")
    @Operation(summary = "Medical claim statistics: claims by status, total approved amount, pending queues")
    public ResponseEntity<MedicalStatisticsResponse> medicalStatistics(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getMedicalStatistics(year));
    }

    // ---------- PF dashboard ----------

    @GetMapping("/pf/statistics")
    @Operation(summary = "PF statistics: total balance, contributions, withdrawals by status")
    public ResponseEntity<PfStatisticsResponse> pfStatistics() {
        return ResponseEntity.ok(dashboardService.getPfStatistics());
    }
}
