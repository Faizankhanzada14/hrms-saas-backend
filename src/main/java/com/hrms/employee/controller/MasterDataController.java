package com.hrms.employee.controller;

import com.hrms.employee.dto.MasterDataDtos.*;
import com.hrms.employee.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Departments, designations, and posting units (factory/branch/HQ/warehouse)")
public class MasterDataController {

    private final MasterDataService masterDataService;

    // ---------- Departments ----------

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create a department")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createDepartment(request));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update a department")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(masterDataService.updateDepartment(id, request));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Soft-delete a department")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        masterDataService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments")
    @Operation(summary = "List all active departments")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(masterDataService.getAllDepartments());
    }

    // ---------- Designations ----------

    @PostMapping("/designations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create a designation")
    public ResponseEntity<DesignationResponse> createDesignation(@Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createDesignation(request));
    }

    @PutMapping("/designations/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update a designation")
    public ResponseEntity<DesignationResponse> updateDesignation(@PathVariable Long id, @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(masterDataService.updateDesignation(id, request));
    }

    @DeleteMapping("/designations/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Soft-delete a designation")
    public ResponseEntity<Void> deleteDesignation(@PathVariable Long id) {
        masterDataService.deleteDesignation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/designations")
    @Operation(summary = "List all active designations")
    public ResponseEntity<List<DesignationResponse>> getAllDesignations() {
        return ResponseEntity.ok(masterDataService.getAllDesignations());
    }

    // ---------- Posting Units ----------

    @PostMapping("/posting-units")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create a posting unit (factory/branch/head office/warehouse)")
    public ResponseEntity<PostingUnitResponse> createPostingUnit(@Valid @RequestBody PostingUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createPostingUnit(request));
    }

    @PutMapping("/posting-units/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update a posting unit")
    public ResponseEntity<PostingUnitResponse> updatePostingUnit(@PathVariable Long id, @Valid @RequestBody PostingUnitRequest request) {
        return ResponseEntity.ok(masterDataService.updatePostingUnit(id, request));
    }

    @DeleteMapping("/posting-units/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Soft-delete a posting unit")
    public ResponseEntity<Void> deletePostingUnit(@PathVariable Long id) {
        masterDataService.deletePostingUnit(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posting-units")
    @Operation(summary = "List all active posting units")
    public ResponseEntity<List<PostingUnitResponse>> getAllPostingUnits() {
        return ResponseEntity.ok(masterDataService.getAllPostingUnits());
    }
}
