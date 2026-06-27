package com.hrms.leave.dto;

import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.entity.LeaveType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LeaveEncashmentDtos {

    @Getter
    @Setter
    public static class EncashmentRequest {
        @NotNull(message = "Leave type is required")
        private LeaveType leaveType;

        @NotNull(message = "Number of days is required")
        @DecimalMin(value = "0.5", message = "Days to encash must be at least 0.5")
        private BigDecimal days;
    }

    @Getter
    @Setter
    @Builder
    public static class EncashmentResponse {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private LeaveType leaveType;
        private int year;
        private BigDecimal days;
        private BigDecimal dailyRate;
        private BigDecimal amount;
        private LeaveStatus status;
        private String decisionRemarks;
        private LocalDateTime decidedAt;
        private LocalDateTime createdAt;
    }
}
