package com.hrms.leave.controller;

import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveEncashmentDtos.EncashmentRequest;
import com.hrms.leave.dto.LeaveEncashmentDtos.EncashmentResponse;
import com.hrms.leave.service.LeaveEncashmentService;
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

@RestController
@RequestMapping("/api/v1/leaves/encashment")
@RequiredArgsConstructor
@Tag(name = "Leave Encashment", description = "Salary-based leave encashment requests and approvals")
public class LeaveEncashmentController {

    private final LeaveEncashmentService encashmentService;

    @PostMapping
    @Operation(summary = "Request encashment of unused leave balance")
    public ResponseEntity<EncashmentResponse> requestEncashment(Authentication authentication,
                                                                   @Valid @RequestBody EncashmentRequest request) {
        var response = encashmentService.requestEncashment(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Approve an encashment request")
    public ResponseEntity<EncashmentResponse> approve(Authentication authentication, @PathVariable Long id,
                                                        @RequestBody(required = false) LeaveDecisionRequest decision) {
        return ResponseEntity.ok(encashmentService.decideEncashment(authentication.getName(), id, true, decision));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Reject an encashment request")
    public ResponseEntity<EncashmentResponse> reject(Authentication authentication, @PathVariable Long id,
                                                       @RequestBody(required = false) LeaveDecisionRequest decision) {
        return ResponseEntity.ok(encashmentService.decideEncashment(authentication.getName(), id, false, decision));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Get the current user's own encashment history")
    public ResponseEntity<PageResponse<EncashmentResponse>> myHistory(Authentication authentication,
                                                                         @PageableDefault(size = 20) Pageable pageable) {
        var page = encashmentService.getMyEncashmentHistory(authentication.getName(), pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }
}
