package com.hrms.employee.controller;

import com.hrms.employee.dto.EmployeeFilter;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.service.EmployeeService;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "CRUD, search, filter, and directory APIs for employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create a new employee")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update an existing employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Soft-delete an employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id, Authentication authentication) {
        employeeService.deleteEmployee(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by internal id")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/code/{employeeCode}")
    @Operation(summary = "Get an employee by employee code")
    public ResponseEntity<EmployeeResponse> getEmployeeByCode(@PathVariable String employeeCode) {
        return ResponseEntity.ok(employeeService.getEmployeeByCode(employeeCode));
    }

    @GetMapping
    @Operation(summary = "List all employees with optional filtering, sorting, and pagination")
    public ResponseEntity<PageResponse<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long postingUnitId,
            @RequestParam(required = false) Employee.EmploymentStatus employmentStatus,
            @RequestParam(required = false) Employee.Gender gender,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {

        EmployeeFilter filter = new EmployeeFilter();
        filter.setSearch(search);
        filter.setDepartmentId(departmentId);
        filter.setDesignationId(designationId);
        filter.setPostingUnitId(postingUnitId);
        filter.setEmploymentStatus(employmentStatus);
        filter.setGender(gender);
        filter.setActive(active);

        var page = employeeService.getAllEmployees(filter, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/search")
    @Operation(summary = "Search employees by name, code, email, or CNIC")
    public ResponseEntity<PageResponse<EmployeeResponse>> searchEmployees(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = employeeService.searchEmployees(query, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/directory")
    @Operation(summary = "Employee directory - lightweight listing for org-wide lookup")
    public ResponseEntity<PageResponse<EmployeeResponse>> employeeDirectory(
            @PageableDefault(size = 50) Pageable pageable) {
        EmployeeFilter filter = new EmployeeFilter();
        filter.setActive(true);
        var page = employeeService.getAllEmployees(filter, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }
}
