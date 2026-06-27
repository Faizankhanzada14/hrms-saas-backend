package com.hrms.posting.controller;

import com.hrms.posting.dto.PostingDtos.PostingHistoryResponse;
import com.hrms.posting.dto.PostingDtos.TransferRequest;
import com.hrms.posting.service.PostingService;
import com.hrms.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/postings")
@RequiredArgsConstructor
@Tag(name = "Posting Management", description = "Employee transfers and posting history across factory units, branches, and departments")
public class PostingController {

    private final PostingService postingService;

    @PostMapping("/employees/{employeeId}/transfer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Transfer an employee to a new posting unit and/or department")
    public ResponseEntity<PostingHistoryResponse> transferEmployee(
            Authentication authentication, @PathVariable Long employeeId,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(postingService.transferEmployee(authentication.getName(), employeeId, request));
    }

    @GetMapping("/employees/{employeeId}/history")
    @Operation(summary = "Get posting/transfer history for an employee")
    public ResponseEntity<PageResponse<PostingHistoryResponse>> getPostingHistory(
            @PathVariable Long employeeId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(postingService.getPostingHistory(employeeId, pageable)));
    }
}
