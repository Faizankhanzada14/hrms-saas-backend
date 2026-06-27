package com.hrms.medical.dto;

import com.hrms.medical.entity.ClaimStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MedicalClaimResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String description;
    private BigDecimal amount;
    private LocalDate claimDate;
    private String attachmentOriginalName;
    private boolean hasAttachment;
    private ClaimStatus status;
    private String hrRemarks;
    private String hrDecidedByUsername;
    private LocalDateTime hrDecidedAt;
    private String financeRemarks;
    private String financeDecidedByUsername;
    private LocalDateTime financeDecidedAt;
    private LocalDateTime createdAt;
}
