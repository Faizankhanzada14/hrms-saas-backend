package com.hrms.pf.controller;

import com.hrms.pf.dto.PfAccountDtos.AccountResponse;
import com.hrms.pf.dto.PfAccountDtos.OpenAccountRequest;
import com.hrms.pf.dto.PfLedgerEntryResponse;
import com.hrms.pf.dto.PfWithdrawalDtos.DecisionRequest;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalRequest;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalResponse;
import com.hrms.pf.service.PfService;
import com.hrms.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/pf")
@RequiredArgsConstructor
@Tag(name = "Provident Fund", description = "PF accounts, monthly contributions, withdrawals, statements, and ledger")
public class PfController {

    private final PfService pfService;

    @PostMapping("/accounts/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Open a PF account for an employee")
    public ResponseEntity<AccountResponse> openAccount(@PathVariable Long employeeId,
                                                         @RequestBody(required = false) OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pfService.openAccount(employeeId, request));
    }

    @GetMapping("/accounts/my")
    @Operation(summary = "Get the current user's own PF account")
    public ResponseEntity<AccountResponse> myAccount(Authentication authentication) {
        return ResponseEntity.ok(pfService.getMyAccount(authentication.getName()));
    }

    @GetMapping("/accounts/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Get a specific employee's PF account")
    public ResponseEntity<AccountResponse> employeeAccount(@PathVariable Long employeeId) {
        return ResponseEntity.ok(pfService.getAccount(employeeId));
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Request a PF withdrawal")
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(Authentication authentication,
                                                                   @Valid @RequestBody WithdrawalRequest request) {
        var response = pfService.requestWithdrawal(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Approve a PF withdrawal request")
    public ResponseEntity<WithdrawalResponse> approveWithdrawal(Authentication authentication, @PathVariable Long id,
                                                                   @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(pfService.decideWithdrawal(authentication.getName(), id, true, decision));
    }

    @PostMapping("/withdrawals/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Reject a PF withdrawal request")
    public ResponseEntity<WithdrawalResponse> rejectWithdrawal(Authentication authentication, @PathVariable Long id,
                                                                  @RequestBody(required = false) DecisionRequest decision) {
        return ResponseEntity.ok(pfService.decideWithdrawal(authentication.getName(), id, false, decision));
    }

    @GetMapping("/withdrawals/my-history")
    @Operation(summary = "Get the current user's own PF withdrawal history")
    public ResponseEntity<PageResponse<WithdrawalResponse>> myWithdrawalHistory(
            Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(pfService.getMyWithdrawalHistory(authentication.getName(), pageable)));
    }

    @GetMapping("/withdrawals/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Get all pending PF withdrawal requests")
    public ResponseEntity<PageResponse<WithdrawalResponse>> pendingWithdrawals(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(pfService.getPendingWithdrawals(pageable)));
    }

    @GetMapping("/statement/my")
    @Operation(summary = "Get the current user's own PF statement (ledger entries) for an optional date range")
    public ResponseEntity<PageResponse<PfLedgerEntryResponse>> myStatement(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(pfService.getMyStatement(authentication.getName(), from, to, pageable)));
    }

    @GetMapping("/statement/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'FINANCE')")
    @Operation(summary = "Get a specific employee's PF statement for an optional date range")
    public ResponseEntity<PageResponse<PfLedgerEntryResponse>> employeeStatement(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(pfService.getEmployeeStatement(employeeId, from, to, pageable)));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE')")
    @Operation(summary = "Org-wide PF ledger across all accounts (Finance auditing/reconciliation)")
    public ResponseEntity<PageResponse<PfLedgerEntryResponse>> fullLedger(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(pfService.getFullLedger(pageable)));
    }
}
