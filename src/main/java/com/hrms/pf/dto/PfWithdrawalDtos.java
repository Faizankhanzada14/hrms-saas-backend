package com.hrms.pf.dto;

import com.hrms.pf.entity.PfWithdrawalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PfWithdrawalDtos {

    @Getter
    @Setter
    public static class WithdrawalRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;

        private String reason;
    }

    @Getter
    @Setter
    public static class DecisionRequest {
        private String remarks;
    }

    @Getter
    @Setter
    @Builder
    public static class WithdrawalResponse {
        private Long id;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private BigDecimal amount;
        private String reason;
        private PfWithdrawalStatus status;
        private String decidedByUsername;
        private String decisionRemarks;
        private LocalDateTime decidedAt;
        private LocalDateTime createdAt;
    }
}
