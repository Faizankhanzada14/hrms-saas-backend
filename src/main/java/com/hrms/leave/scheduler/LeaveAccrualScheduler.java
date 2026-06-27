package com.hrms.leave.scheduler;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.config.LeavePolicy;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Handles two recurring leave-policy jobs:
 *  - Year-end carry-forward: rolls a capped portion of unused leave into the new year.
 *  - Monthly accrual is intentionally NOT pro-rated here; entitlement is allocated in full
 *    at the start of each year via {@code LeaveService.getOrCreateBalance}. The carry-forward
 *    job below is what actually needs to run on a schedule.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAccrualScheduler {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeavePolicy leavePolicy;
    private final AuditService auditService;

    /**
     * Runs once a year on January 1st at 01:00 server time. For every active employee and every
     * leave type, carries forward min(previous year's unused balance, policy cap) into the new
     * year's allocation, then seeds the new year's balance row.
     */
    @Scheduled(cron = "0 0 1 1 1 *")
    @Transactional
    public void runYearEndCarryForward() {
        int previousYear = LocalDate.now().getYear() - 1;
        int newYear = LocalDate.now().getYear();

        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> !e.isDeleted() && e.isActive())
                .toList();

        log.info("Starting leave carry-forward job: {} -> {} for {} employee(s)",
                previousYear, newYear, activeEmployees.size());

        for (Employee employee : activeEmployees) {
            for (LeaveType type : LeaveType.values()) {
                leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(employee.getId(), type, previousYear)
                        .ifPresent(previous -> carryForwardOne(employee, type, previous, newYear));
            }
        }

        auditService.logSuccess("LEAVE_CARRY_FORWARD", "LeaveBalance", "ALL",
                "Year-end carry-forward processed for " + activeEmployees.size() + " employee(s)");
        log.info("Leave carry-forward job completed");
    }

    private void carryForwardOne(Employee employee, LeaveType type, LeaveBalance previous, int newYear) {
        BigDecimal unused = previous.available().max(BigDecimal.ZERO);
        BigDecimal cap = leavePolicy.maxCarryForwardFor(type);
        BigDecimal toCarryForward = unused.min(cap).setScale(1, RoundingMode.DOWN);

        LeaveBalance newBalance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeAndYear(employee.getId(), type, newYear)
                .orElseGet(() -> {
                    LeaveBalance balance = new LeaveBalance();
                    balance.setEmployee(employee);
                    balance.setLeaveType(type);
                    balance.setYear(newYear);
                    balance.setAllocated(leavePolicy.entitlementFor(type));
                    return balance;
                });

        newBalance.setCarriedForward(toCarryForward);
        leaveBalanceRepository.save(newBalance);
    }
}
