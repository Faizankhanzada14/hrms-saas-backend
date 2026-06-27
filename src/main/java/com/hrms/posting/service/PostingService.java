package com.hrms.posting.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.entity.PostingUnit;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.PostingUnitRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.posting.dto.PostingDtos.PostingHistoryResponse;
import com.hrms.posting.dto.PostingDtos.TransferRequest;
import com.hrms.posting.entity.PostingHistory;
import com.hrms.posting.repository.PostingHistoryRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostingService {

    private final EmployeeRepository employeeRepository;
    private final PostingUnitRepository postingUnitRepository;
    private final DepartmentRepository departmentRepository;
    private final PostingHistoryRepository postingHistoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public PostingHistoryResponse transferEmployee(String performedByUsername, Long employeeId, TransferRequest request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (request.getNewPostingUnitId() == null && request.getNewDepartmentId() == null) {
            throw new BadRequestException("At least one of newPostingUnitId or newDepartmentId must be provided");
        }

        PostingUnit fromUnit = employee.getPostingUnit();
        Department fromDept = employee.getDepartment();

        PostingHistory history = new PostingHistory();
        history.setEmployee(employee);
        history.setFromPostingUnit(fromUnit);
        history.setFromDepartment(fromDept);
        history.setTransferDate(request.getTransferDate());
        history.setReason(request.getReason());

        if (request.getNewPostingUnitId() != null) {
            PostingUnit newUnit = postingUnitRepository.findById(request.getNewPostingUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Posting unit not found with id: " + request.getNewPostingUnitId()));
            employee.setPostingUnit(newUnit);
            history.setToPostingUnit(newUnit);
        } else {
            history.setToPostingUnit(fromUnit);
        }

        if (request.getNewDepartmentId() != null) {
            Department newDept = departmentRepository.findById(request.getNewDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Department not found with id: " + request.getNewDepartmentId()));
            employee.setDepartment(newDept);
            history.setToDepartment(newDept);
        } else {
            history.setToDepartment(fromDept);
        }

        User performedBy = userRepository.findByUsernameAndDeletedFalse(performedByUsername).orElse(null);
        history.setTransferredBy(performedBy);

        employeeRepository.save(employee);
        PostingHistory saved = postingHistoryRepository.save(history);

        auditService.logSuccess("EMPLOYEE_TRANSFER", "Employee", employee.getEmployeeCode(),
                "Transferred by " + performedByUsername);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PostingHistoryResponse> getPostingHistory(Long employeeId, Pageable pageable) {
        return postingHistoryRepository.findByEmployeeIdOrderByTransferDateDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    private PostingHistoryResponse toResponse(PostingHistory history) {
        Employee employee = history.getEmployee();
        return PostingHistoryResponse.builder()
                .id(history.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .fromPostingUnitId(history.getFromPostingUnit() != null ? history.getFromPostingUnit().getId() : null)
                .fromPostingUnitName(history.getFromPostingUnit() != null ? history.getFromPostingUnit().getName() : null)
                .toPostingUnitId(history.getToPostingUnit() != null ? history.getToPostingUnit().getId() : null)
                .toPostingUnitName(history.getToPostingUnit() != null ? history.getToPostingUnit().getName() : null)
                .fromDepartmentId(history.getFromDepartment() != null ? history.getFromDepartment().getId() : null)
                .fromDepartmentName(history.getFromDepartment() != null ? history.getFromDepartment().getName() : null)
                .toDepartmentId(history.getToDepartment() != null ? history.getToDepartment().getId() : null)
                .toDepartmentName(history.getToDepartment() != null ? history.getToDepartment().getName() : null)
                .transferDate(history.getTransferDate())
                .reason(history.getReason())
                .transferredByUsername(history.getTransferredBy() != null ? history.getTransferredBy().getUsername() : null)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
