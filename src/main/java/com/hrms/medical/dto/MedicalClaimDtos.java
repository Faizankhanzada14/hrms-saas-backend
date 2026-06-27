package com.hrms.medical.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MedicalClaimDtos {

    @Getter
    @Setter
    public static class ClaimRequest {
        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;

        @NotNull(message = "Claim date is required")
        @PastOrPresent(message = "Claim date cannot be in the future")
        private LocalDate claimDate;
    }

    @Getter
    @Setter
    public static class DecisionRequest {
        private String remarks;
    }
}
