package com.hrms.pf.scheduler;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.pf.config.PfPolicy;
import com.hrms.pf.entity.PfAccount;
import com.hrms.pf.repository.PfAccountRepository;
import com.hrms.pf.service.PfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Posts that month's employee + employer PF contributions for every active employee with
 * (or eligible for) a PF account. Runs on the 1st of every month at 02:00 server time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PfContributionScheduler {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeRepository employeeRepository;
    private final PfAccountRepository pfAccountRepository;
    private final PfService pfService;
    private final PfPolicy pfPolicy;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 2 1 * *")
    @Transactional
    public void runMonthlyContributions() {
        String yearMonth = LocalDate.now().minusMonths(1).format(MONTH_FORMAT); // contribution for the month just closed
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> !e.isDeleted() && e.isActive() && e.getSalary() != null
                        && e.getSalary().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        log.info("Starting PF monthly contribution job for {} ({} active employee(s))",
                yearMonth, activeEmployees.size());

        int posted = 0;
        for (Employee employee : activeEmployees) {
            PfAccount account = pfAccountRepository.findByEmployeeId(employee.getId()).orElse(null);

            if (account == null) {
                if (!pfPolicy.isAutoOpenAccounts()) {
                    continue;
                }
                account = openDefaultAccount(employee);
            }

            if (!account.isActive()) {
                continue;
            }

            pfService.postMonthlyContribution(account, employee.getSalary(), yearMonth);
            posted++;
        }

        auditService.logSuccess("PF_MONTHLY_CONTRIBUTION", "PfAccount", yearMonth,
                "Posted monthly PF contributions for " + posted + " account(s)");
        log.info("PF monthly contribution job completed: {} account(s) processed", posted);
    }

    private PfAccount openDefaultAccount(Employee employee) {
        PfAccount account = new PfAccount();
        account.setEmployee(employee);
        account.setEmployeeContributionRate(pfPolicy.getDefaultEmployeeContributionRate());
        account.setEmployerContributionRate(pfPolicy.getDefaultEmployerContributionRate());
        return pfAccountRepository.save(account);
    }
}
