package com.hrms.medical.controller;

import com.hrms.medical.dto.MedicalBudgetResponse;
import com.hrms.medical.dto.MedicalClaimDtos.ClaimRequest;
import com.hrms.medical.dto.MedicalClaimDtos.DecisionRequest;
import com.hrms.medical.dto.MedicalClaimResponse;
import com.hrms.medical.service.MedicalClaimService;
import com.hrms.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/medical-claims")
@RequiredArgsConstructor
@Tag(name = "Medical Claim Management", description = "Employee -> HR -> Finance claim workflow, attachments, and budget tracking")
public class MedicalClaimController {

    private final MedicalClaimService medicalClaimService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a new medical claim, optionally with a receipt attachment")
    public ResponseEntity<MedicalClaimResponse> createClaim(
            Authentication authentication,
            @Valid @RequestPart("claim") ClaimRequest request,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        var response = medicalClaimService.createClaim(authentication.getName(), request, attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Attach or replace the receipt/attachment on a still-pending claim")
    public ResponseEntity<MedicalClaimResponse> uploadAttachment(
            Authentication authentication, @PathVariable Long id,
            @RequestPart("attachment") MultipartFile attachment) {
        return ResponseEntity.ok(medicalClaimService.uploadAttachment(authentication.getName(), id, attachment));
    }

    @PostMapping("/{id}/hr-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "HR approves and forwards a claim to Finance")
    public ResponseEntity<MedicalClaimResponse> hrApprove(Authentication authentication, @PathVariable Long id,
                                                            @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(medicalClaimService.hrApprove(authentication.getName(), id, decision));
    }

    @PostMapping("/{id}/hr-reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "HR rejects a claim (final decision)")
    public ResponseEntity<MedicalClaimResponse> hrReject(Authentication authentication, @PathVariable Long id,
                                                           @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(medicalClaimService.hrReject(authentication.getName(), id, decision));
    }

    @PostMapping("/{id}/finance-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE')")
    @Operation(summary = "Finance approves an HR-forwarded claim, subject to remaining medical budget")
    public ResponseEntity<MedicalClaimResponse> financeApprove(Authentication authentication, @PathVariable Long id,
                                                                 @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(medicalClaimService.financeApprove(authentication.getName(), id, decision));
    }

    @PostMapping("/{id}/finance-reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE')")
    @Operation(summary = "Finance rejects an HR-forwarded claim (final decision)")
    public ResponseEntity<MedicalClaimResponse> financeReject(Authentication authentication, @PathVariable Long id,
                                                                @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(medicalClaimService.financeReject(authentication.getName(), id, decision));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single claim by id")
    public ResponseEntity<MedicalClaimResponse> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(medicalClaimService.getClaim(id));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Get the current user's own claim history")
    public ResponseEntity<PageResponse<MedicalClaimResponse>> myHistory(Authentication authentication,
                                                                          @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(medicalClaimService.getMyHistory(authentication.getName(), pageable)));
    }

    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Get claim history for a specific employee")
    public ResponseEntity<PageResponse<MedicalClaimResponse>> employeeHistory(@PathVariable Long employeeId,
                                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(medicalClaimService.getEmployeeHistory(employeeId, pageable)));
    }

    @GetMapping("/pending-hr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "HR dashboard queue: claims awaiting HR review")
    public ResponseEntity<PageResponse<MedicalClaimResponse>> pendingForHr(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(medicalClaimService.getPendingForHr(pageable)));
    }

    @GetMapping("/pending-finance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE')")
    @Operation(summary = "Finance dashboard queue: HR-approved claims awaiting Finance decision")
    public ResponseEntity<PageResponse<MedicalClaimResponse>> pendingForFinance(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(medicalClaimService.getPendingForFinance(pageable)));
    }

    @GetMapping("/budget/my")
    @Operation(summary = "Get the current user's own medical budget summary")
    public ResponseEntity<MedicalBudgetResponse> myBudgetSummary(Authentication authentication,
                                                                    @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(medicalClaimService.getMyBudgetSummary(authentication.getName(), year));
    }

    @GetMapping("/budget/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Get medical budget summary (allocated/used/remaining) for an employee")
    public ResponseEntity<MedicalBudgetResponse> budgetSummary(@PathVariable Long employeeId,
                                                                 @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(medicalClaimService.getBudgetSummary(employeeId, year));
    }
}
