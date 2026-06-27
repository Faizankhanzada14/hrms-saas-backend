package com.hrms.leave.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.leave.config.LeavePolicy;
import com.hrms.leave.dto.LeaveDecisionRequest;
import com.hrms.leave.dto.LeaveEncashmentDtos.EncashmentRequest;
import com.hrms.leave.dto.LeaveEncashmentDtos.EncashmentResponse;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveEncashment;
import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveEncashmentRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class LeaveEncashmentService {

    private final LeaveEncashmentRepository encashmentRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeavePolicy leavePolicy;
    private final LeaveService leaveService;
    private final AuditService auditService;

    @Transactional
    public EncashmentResponse requestEncashment(String username, EncashmentRequest request) {
        Employee employee = employeeRepository.findByUser_UsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to user: " + username));

        int year = Year.now().getValue();
        LeaveBalance balance = leaveService.getOrCreateBalance(employee, request.getLeaveType(), year);

        if (balance.available().compareTo(request.getDays()) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient %s balance to encash: requested %s day(s), available %s day(s)",
                    request.getLeaveType(), request.getDays(), balance.available()));
        }

        BigDecimal dailyRate = (employee.getSalary() == null ? BigDecimal.ZERO : employee.getSalary())
                .divide(BigDecimal.valueOf(leavePolicy.getWorkingDaysPerMonth()), 2, RoundingMode.HALF_UP);
        BigDecimal amount = dailyRate.multiply(request.getDays()).setScale(2, RoundingMode.HALF_UP);

        LeaveEncashment encashment = new LeaveEncashment();
        encashment.setEmployee(employee);
        encashment.setLeaveType(request.getLeaveType());
        encashment.setYear(year);
        encashment.setDays(request.getDays());
        encashment.setDailyRate(dailyRate);
        encashment.setAmount(amount);
        encashment.setStatus(LeaveStatus.PENDING);

        LeaveEncashment saved = encashmentRepository.save(encashment);
        auditService.logSuccess("LEAVE_ENCASHMENT_REQUEST", "LeaveEncashment", saved.getId().toString(),
                employee.getEmployeeCode() + " requested encashment of " + request.getDays() + " " + request.getLeaveType());

        return toResponse(saved);
    }

    @Transactional
    public EncashmentResponse decideEncashment(String approverUsername, Long encashmentId,
                                                boolean approve, LeaveDecisionRequest decision) {
        LeaveEncashment encashment = encashmentRepository.findById(encashmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Encashment request not found with id: " + encashmentId));

        if (encashment.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("This encashment request has already been decided");
        }

        User approver = userRepository.findByUsernameAndDeletedFalse(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver user not found"));

        if (approve) {
            LeaveBalance balance = leaveService.getOrCreateBalance(
                    encashment.getEmployee(), encashment.getLeaveType(), encashment.getYear());
            if (balance.available().compareTo(encashment.getDays()) < 0) {
                throw new BadRequestException("Employee no longer has sufficient balance for this encashment");
            }
            balance.setEncashed(balance.getEncashed().add(encashment.getDays()));
            leaveBalanceRepository.save(balance);
            encashment.setStatus(LeaveStatus.APPROVED);
        } else {
            encashment.setStatus(LeaveStatus.REJECTED);
        }

        encashment.setDecidedBy(approver);
        encashment.setDecisionRemarks(decision != null ? decision.getRemarks() : null);
        encashment.setDecidedAt(LocalDateTime.now());

        LeaveEncashment saved = encashmentRepository.save(encashment);
        auditService.logSuccess(approve ? "LEAVE_ENCASHMENT_APPROVE" : "LEAVE_ENCASHMENT_REJECT",
                "LeaveEncashment", saved.getId().toString(), "Decided by " + approverUsername);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EncashmentResponse> getMyEncashmentHistory(String username, Pageable pageable) {
        Employee employee = employeeRepository.findByUser_UsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to user: " + username));
        return encashmentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), pageable)
                .map(this::toResponse);
    }

    private EncashmentResponse toResponse(LeaveEncashment encashment) {
        Employee employee = encashment.getEmployee();
        return EncashmentResponse.builder()
                .id(encashment.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .leaveType(encashment.getLeaveType())
                .year(encashment.getYear())
                .days(encashment.getDays())
                .dailyRate(encashment.getDailyRate())
                .amount(encashment.getAmount())
                .status(encashment.getStatus())
                .decisionRemarks(encashment.getDecisionRemarks())
                .decidedAt(encashment.getDecidedAt())
                .createdAt(encashment.getCreatedAt())
                .build();
    }
}
