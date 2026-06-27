package com.hrms.leave.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.leave.config.LeavePolicy;
import com.hrms.leave.dto.LeaveApplyRequest;
import com.hrms.leave.dto.LeaveBalanceResponse;
import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveResponse;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeavePolicy leavePolicy;
    private final AuditService auditService;

    // ---------- Apply / Cancel ----------

    @Transactional
    public LeaveResponse applyLeave(String username, LeaveApplyRequest request) {
        Employee employee = resolveEmployee(username);

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        BigDecimal days = BigDecimal.valueOf(
                ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);

        int year = request.getStartDate().getYear();
        LeaveBalance balance = getOrCreateBalance(employee, request.getLeaveType(), year);

        if (balance.available().compareTo(days) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient %s balance: requested %s day(s), available %s day(s)",
                    request.getLeaveType(), days, balance.available()));
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(request.getLeaveType());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setDays(days);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        auditService.logSuccess("LEAVE_APPLY", "LeaveRequest", saved.getId().toString(),
                employee.getEmployeeCode() + " applied for " + days + " day(s) of " + request.getLeaveType());

        return toResponse(saved);
    }

    @Transactional
    public LeaveResponse cancelLeave(String username, Long leaveRequestId) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(leaveRequestId);
        Employee employee = resolveEmployee(username);

        boolean isOwner = leaveRequest.getEmployee().getId().equals(employee.getId());
        boolean isPrivileged = hasAnyAuthority(username, "ROLE_SUPER_ADMIN", "ROLE_HR_ADMIN");
        if (!isOwner && !isPrivileged) {
            throw new BadRequestException("You can only cancel your own leave requests");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED || leaveRequest.getStatus() == LeaveStatus.REJECTED) {
            throw new BadRequestException("This leave request is already " + leaveRequest.getStatus());
        }

        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            if (leaveRequest.getStartDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Cannot cancel a leave that has already started");
            }
            LeaveBalance balance = getOrCreateBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType(),
                    leaveRequest.getStartDate().getYear());
            balance.setUsed(balance.getUsed().subtract(leaveRequest.getDays()));
            leaveBalanceRepository.save(balance);
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequest.setCancelledAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        auditService.logSuccess("LEAVE_CANCEL", "LeaveRequest", saved.getId().toString(), "Leave request cancelled");
        return toResponse(saved);
    }

    // ---------- Approve / Reject ----------

    @Transactional
    public LeaveResponse approveLeave(String approverUsername, Long leaveRequestId, LeaveDecisionRequest decision) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(leaveRequestId);
        ensureCanDecide(approverUsername, leaveRequest);
        ensurePending(leaveRequest);

        LeaveBalance balance = getOrCreateBalance(leaveRequest.getEmployee(), leaveRequest.getLeaveType(),
                leaveRequest.getStartDate().getYear());
        if (balance.available().compareTo(leaveRequest.getDays()) < 0) {
            throw new BadRequestException("Employee no longer has sufficient balance to approve this request");
        }
        balance.setUsed(balance.getUsed().add(leaveRequest.getDays()));
        leaveBalanceRepository.save(balance);

        User approver = userRepository.findByUsernameAndDeletedFalse(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver user not found"));

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setDecidedBy(approver);
        leaveRequest.setDecisionRemarks(decision != null ? decision.getRemarks() : null);
        leaveRequest.setDecidedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        auditService.logSuccess("LEAVE_APPROVE", "LeaveRequest", saved.getId().toString(),
                "Approved by " + approverUsername);

        return toResponse(saved);
    }

    @Transactional
    public LeaveResponse rejectLeave(String approverUsername, Long leaveRequestId, LeaveDecisionRequest decision) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(leaveRequestId);
        ensureCanDecide(approverUsername, leaveRequest);
        ensurePending(leaveRequest);

        User approver = userRepository.findByUsernameAndDeletedFalse(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver user not found"));

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setDecidedBy(approver);
        leaveRequest.setDecisionRemarks(decision != null ? decision.getRemarks() : null);
        leaveRequest.setDecidedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        auditService.logSuccess("LEAVE_REJECT", "LeaveRequest", saved.getId().toString(),
                "Rejected by " + approverUsername);

        return toResponse(saved);
    }

    // ---------- History / Balance ----------

    @Transactional(readOnly = true)
    public Page<LeaveResponse> getMyLeaveHistory(String username, Pageable pageable) {
        Employee employee = resolveEmployee(username);
        return leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employee.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> getEmployeeLeaveHistory(Long employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> getPendingApprovalsForManager(String managerUsername, Pageable pageable) {
        Employee manager = resolveEmployee(managerUsername);
        return leaveRequestRepository
                .findByEmployee_Manager_IdAndStatusOrderByCreatedAtAsc(manager.getId(), LeaveStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyLeaveBalance(String username, int year) {
        Employee employee = resolveEmployee(username);
        return getLeaveBalanceForEmployee(employee.getId(), year);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalanceForEmployee(Long employeeId, int year) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        return Arrays.stream(LeaveType.values())
                .map(type -> {
                    LeaveBalance balance = getOrCreateBalance(employee, type, year);
                    return LeaveBalanceResponse.builder()
                            .leaveType(type)
                            .year(year)
                            .allocated(balance.getAllocated())
                            .carriedForward(balance.getCarriedForward())
                            .used(balance.getUsed())
                            .encashed(balance.getEncashed())
                            .available(balance.available())
                            .build();
                })
                .toList();
    }

    // ---------- Internal helpers ----------

    LeaveBalance getOrCreateBalance(Employee employee, LeaveType type, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(employee.getId(), type, year)
                .orElseGet(() -> {
                    LeaveBalance balance = new LeaveBalance();
                    balance.setEmployee(employee);
                    balance.setLeaveType(type);
                    balance.setYear(year);
                    balance.setAllocated(leavePolicy.entitlementFor(type));
                    return leaveBalanceRepository.save(balance);
                });
    }

    private Employee resolveEmployee(String username) {
        return employeeRepository.findByUser_UsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No employee profile is linked to user: " + username));
    }

    private LeaveRequest getLeaveRequestOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private void ensurePending(LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be decided; current status: "
                    + leaveRequest.getStatus());
        }
    }

    /**
     * A leave request may be decided by: SUPER_ADMIN, HR_ADMIN, or the direct manager-of-record
     * of the requesting employee. A MANAGER role alone does not grant blanket approval rights —
     * it must be *this* employee's manager.
     */
    private void ensureCanDecide(String approverUsername, LeaveRequest leaveRequest) {
        if (hasAnyAuthority(approverUsername, "ROLE_SUPER_ADMIN", "ROLE_HR_ADMIN")) {
            return;
        }

        Employee manager = leaveRequest.getEmployee().getManager();
        boolean isDirectManager = manager != null
                && manager.getUser() != null
                && manager.getUser().getUsername().equals(approverUsername);

        if (!isDirectManager) {
            throw new BadRequestException("You are not authorized to approve or reject this leave request");
        }
    }

    private boolean hasAnyAuthority(String username, String... authorities) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(granted -> Arrays.asList(authorities).contains(granted));
    }

    private LeaveResponse toResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        return LeaveResponse.builder()
                .id(leaveRequest.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .leaveType(leaveRequest.getLeaveType())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .days(leaveRequest.getDays())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
                .decidedByUsername(leaveRequest.getDecidedBy() != null ? leaveRequest.getDecidedBy().getUsername() : null)
                .decisionRemarks(leaveRequest.getDecisionRemarks())
                .decidedAt(leaveRequest.getDecidedAt())
                .createdAt(leaveRequest.getCreatedAt())
                .build();
    }
}
