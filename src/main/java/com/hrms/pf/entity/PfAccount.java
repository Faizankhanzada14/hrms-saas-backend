package com.hrms.pf.entity;

import com.hrms.audit.BaseAuditableEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One PF account per employee. {@code balance} is a cached running total — the {@code PfLedgerEntry}
 * rows are the source of truth, and balance is only ever updated in the same transaction as the
 * ledger entry that changes it, so the two can never drift.
 */
@Entity
@Table(name = "pf_accounts")
@Getter
@Setter
public class PfAccount extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "employee_contribution_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal employeeContributionRate;

    @Column(name = "employer_contribution_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal employerContributionRate;

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_employee_contribution", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEmployeeContribution = BigDecimal.ZERO;

    @Column(name = "total_employer_contribution", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEmployerContribution = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Column(name = "opened_date", nullable = false)
    private LocalDate openedDate = LocalDate.now();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Last calendar month (YYYY-MM) for which monthly contributions were posted, to make the job idempotent. */
    @Column(name = "last_contribution_month", length = 7)
    private String lastContributionMonth;
}
