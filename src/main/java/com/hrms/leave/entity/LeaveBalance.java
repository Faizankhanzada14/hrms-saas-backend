package com.hrms.leave.entity;

import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(
        name = "uq_leave_balance_employee_type_year",
        columnNames = {"employee_id", "leave_type", "year"}
))
@Getter
@Setter
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "allocated", nullable = false, precision = 6, scale = 1)
    private BigDecimal allocated = BigDecimal.ZERO;

    @Column(name = "carried_forward", nullable = false, precision = 6, scale = 1)
    private BigDecimal carriedForward = BigDecimal.ZERO;

    @Column(name = "used", nullable = false, precision = 6, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;

    @Column(name = "encashed", nullable = false, precision = 6, scale = 1)
    private BigDecimal encashed = BigDecimal.ZERO;

    /** Total days available to spend: allocated + carriedForward - used - encashed. */
    public BigDecimal available() {
        return allocated.add(carriedForward).subtract(used).subtract(encashed);
    }
}
