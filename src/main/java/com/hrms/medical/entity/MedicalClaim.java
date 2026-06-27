package com.hrms.medical.entity;

import com.hrms.audit.BaseAuditableEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_claims")
@Getter
@Setter
public class MedicalClaim extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(name = "hr_remarks", length = 1000)
    private String hrRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_decided_by_user_id")
    private User hrDecidedBy;

    @Column(name = "hr_decided_at")
    private LocalDateTime hrDecidedAt;

    @Column(name = "finance_remarks", length = 1000)
    private String financeRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_decided_by_user_id")
    private User financeDecidedBy;

    @Column(name = "finance_decided_at")
    private LocalDateTime financeDecidedAt;
}
