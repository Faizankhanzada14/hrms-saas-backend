package com.hrms.leave.controller;

import com.hrms.leave.dto.LeaveApplyRequest;
import com.hrms.leave.dto.LeaveBalanceResponse;
import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveResponse;
import com.hrms.leave.service.LeaveService;
import com.hrms.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Apply, cancel, approve, reject, history, and balance APIs")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @Operation(summary = "Apply for leave")
    public ResponseEntity<LeaveResponse> applyLeave(Authentication authentication,
                                                      @Valid @RequestBody LeaveApplyRequest request) {
        LeaveResponse response = leaveService.applyLeave(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or approved (future-dated) leave request")
    public ResponseEntity<LeaveResponse> cancelLeave(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancelLeave(authentication.getName(), id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Approve a leave request (manager-of-record or HR/admin only)")
    public ResponseEntity<LeaveResponse> approveLeave(Authentication authentication, @PathVariable Long id,
                                                        @RequestBody(required = false) LeaveDecisionRequest decision) {
        return ResponseEntity.ok(leaveService.approveLeave(authentication.getName(), id, decision));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Reject a leave request (manager-of-record or HR/admin only)")
    public ResponseEntity<LeaveResponse> rejectLeave(Authentication authentication, @PathVariable Long id,
                                                       @RequestBody(required = false) LeaveDecisionRequest decision) {
        return ResponseEntity.ok(leaveService.rejectLeave(authentication.getName(), id, decision));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Get the current user's own leave history")
    public ResponseEntity<PageResponse<LeaveResponse>> getMyHistory(Authentication authentication,
                                                                      @PageableDefault(size = 20) Pageable pageable) {
        var page = leaveService.getMyLeaveHistory(authentication.getName(), pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get leave history for a specific employee (HR/admin/manager)")
    public ResponseEntity<PageResponse<LeaveResponse>> getEmployeeHistory(@PathVariable Long employeeId,
                                                                            @PageableDefault(size = 20) Pageable pageable) {
        var page = leaveService.getEmployeeLeaveHistory(employeeId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get pending leave requests for employees reporting to the current manager")
    public ResponseEntity<PageResponse<LeaveResponse>> getPendingApprovals(Authentication authentication,
                                                                             @PageableDefault(size = 20) Pageable pageable) {
        var page = leaveService.getPendingApprovalsForManager(authentication.getName(), pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/my-balance")
    @Operation(summary = "Get the current user's leave balance for a given year (defaults to current year)")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyBalance(
            Authentication authentication,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveService.getMyLeaveBalance(authentication.getName(), targetYear));
    }

    @GetMapping("/employee/{employeeId}/balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get leave balance for a specific employee")
    public ResponseEntity<List<LeaveBalanceResponse>> getEmployeeBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();
        return ResponseEntity.ok(leaveService.getLeaveBalanceForEmployee(employeeId, targetYear));
    }
}
