package com.hrms.employee.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.dto.EmployeeFilter;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.entity.PostingUnit;
import com.hrms.employee.mapper.EmployeeMapper;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSpecifications;
import com.hrms.employee.repository.PostingUnitRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.util.EmployeeCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final PostingUnitRepository postingUnitRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeCodeGenerator codeGenerator;
    private final AuditService auditService;

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (request.getCnic() != null && employeeRepository.existsByCnic(request.getCnic())) {
            throw new DuplicateResourceException("An employee with this CNIC already exists");
        }

        Employee employee = new Employee();
        applyRequestToEntity(employee, request);

        long currentCount = employeeRepository.countByDeletedFalse();
        employee.setEmployeeCode(codeGenerator.generate(currentCount));

        Employee saved = employeeRepository.save(employee);
        auditService.logSuccess("EMPLOYEE_CREATE", "Employee", saved.getEmployeeCode(),
                "Employee created: " + saved.getFirstName() + " " + saved.getLastName());

        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getCnic() != null && !request.getCnic().equals(employee.getCnic())
                && employeeRepository.existsByCnic(request.getCnic())) {
            throw new DuplicateResourceException("An employee with this CNIC already exists");
        }

        applyRequestToEntity(employee, request);
        Employee saved = employeeRepository.save(employee);

        auditService.logSuccess("EMPLOYEE_UPDATE", "Employee", saved.getEmployeeCode(),
                "Employee updated: " + saved.getFirstName() + " " + saved.getLastName());

        return employeeMapper.toResponse(saved);
    }

    @Transactional
    public void deleteEmployee(Long id, String performedBy) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.markDeleted(performedBy);
        employee.setActive(false);
        employeeRepository.save(employee);

        auditService.logSuccess("EMPLOYEE_DELETE", "Employee", employee.getEmployeeCode(),
                "Employee soft-deleted: " + employee.getFirstName() + " " + employee.getLastName());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCodeAndDeletedFalse(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code: " + employeeCode));
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(EmployeeFilter filter, Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAll(EmployeeSpecifications.withFilter(filter), pageable);
        return employees.map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> searchEmployees(String query, Pageable pageable) {
        EmployeeFilter filter = new EmployeeFilter();
        filter.setSearch(query);
        return getAllEmployees(filter, pageable);
    }

    private void applyRequestToEntity(Employee employee, EmployeeRequest request) {
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setCnic(request.getCnic());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setMaritalStatus(request.getMaritalStatus());
        employee.setBloodGroup(request.getBloodGroup());
        employee.setMobileNumber(request.getMobileNumber());
        employee.setEmail(request.getEmail());
        employee.setEmergencyContact(request.getEmergencyContact());
        employee.setAddress(request.getAddress());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setConfirmationDate(request.getConfirmationDate());
        employee.setResignationDate(request.getResignationDate());
        employee.setSalary(request.getSalary());
        employee.setBonus(request.getBonus());
        employee.setMedicalAllocation(request.getMedicalAllocation());
        employee.setProfilePictureUrl(request.getProfilePictureUrl());

        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(request.getEmploymentStatus());
            if (request.getEmploymentStatus() == Employee.EmploymentStatus.RESIGNED
                    && employee.getResignationDate() == null) {
                employee.setResignationDate(java.time.LocalDate.now());
            }
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation designation = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.getDesignationId()));
            employee.setDesignation(designation);
        } else {
            employee.setDesignation(null);
        }

        if (request.getPostingUnitId() != null) {
            PostingUnit postingUnit = postingUnitRepository.findById(request.getPostingUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Posting unit not found with id: " + request.getPostingUnitId()));
            employee.setPostingUnit(postingUnit);
        } else {
            employee.setPostingUnit(null);
        }

        if (request.getManagerId() != null) {
            if (employee.getId() != null && request.getManagerId().equals(employee.getId())) {
                throw new BadRequestException("An employee cannot be their own manager");
            }
            Employee manager = employeeRepository.findByIdAndDeletedFalse(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }
    }
}
