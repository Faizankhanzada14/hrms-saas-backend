package com.hrms.employee;

import com.hrms.audit.AuditService;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.mapper.EmployeeMapper;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.PostingUnitRepository;
import com.hrms.employee.service.EmployeeService;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.util.EmployeeCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DesignationRepository designationRepository;
    @Mock private PostingUnitRepository postingUnitRepository;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeCodeGenerator codeGenerator;
    @Mock private AuditService auditService;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeRequest request;

    @BeforeEach
    void setUp() {
        request = new EmployeeRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setJoiningDate(LocalDate.of(2024, 1, 15));
        request.setCnic("12345-6789012-3");
    }

    @Test
    void createEmployee_withDuplicateCnic_throwsDuplicateResourceException() {
        when(employeeRepository.existsByCnic("12345-6789012-3")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CNIC");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_withValidRequest_generatesCodeAndSaves() {
        when(employeeRepository.existsByCnic(any())).thenReturn(false);
        when(employeeRepository.countByDeletedFalse()).thenReturn(4L);
        when(codeGenerator.generate(4L)).thenReturn("EMP-2026-00005");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeMapper.toResponse(any(Employee.class))).thenReturn(new EmployeeResponse());

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        verify(employeeRepository).save(argThat(e -> "EMP-2026-00005".equals(e.getEmployeeCode())));
        verify(auditService).logSuccess(eq("EMPLOYEE_CREATE"), eq("Employee"), eq("EMP-2026-00005"), any());
    }

    @Test
    void createEmployee_withUnknownDepartment_throwsResourceNotFoundException() {
        request.setDepartmentId(99L);
        when(employeeRepository.existsByCnic(any())).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department");
    }

    @Test
    void getEmployeeById_whenNotFound_throwsResourceNotFoundException() {
        when(employeeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
