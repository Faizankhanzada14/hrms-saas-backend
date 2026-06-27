package com.hrms.leave.entity;

import com.hrms.audit.BaseAuditableEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_encashments")
@Getter
@Setter
public class LeaveEncashment extends BaseAuditableEntity {

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

    @Column(name = "days", nullable = false, precision = 6, scale = 1)
    private BigDecimal days;

    /** Daily rate used at the time of calculation (salary / working days per month). */
    @Column(name = "daily_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private com.hrms.user.entity.User decidedBy;

    @Column(name = "decision_remarks", length = 1000)
    private String decisionRemarks;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
