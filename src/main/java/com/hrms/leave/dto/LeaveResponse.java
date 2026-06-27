package com.hrms.leave.dto;

import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.entity.LeaveType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LeaveResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String reason;
    private LeaveStatus status;
    private String decidedByUsername;
    private String decisionRemarks;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
