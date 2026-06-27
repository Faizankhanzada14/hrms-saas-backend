package com.hrms.pf.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PfAccountDtos {

    @Getter
    @Setter
    public static class OpenAccountRequest {
        @DecimalMin(value = "0", message = "Employee contribution rate cannot be negative")
        private BigDecimal employeeContributionRate;

        @DecimalMin(value = "0", message = "Employer contribution rate cannot be negative")
        private BigDecimal employerContributionRate;

        @PositiveOrZero(message = "Opening balance cannot be negative")
        private BigDecimal openingBalance;
    }

    @Getter
    @Setter
    @Builder
    public static class AccountResponse {
        private Long id;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private BigDecimal employeeContributionRate;
        private BigDecimal employerContributionRate;
        private BigDecimal openingBalance;
        private BigDecimal balance;
        private BigDecimal totalEmployeeContribution;
        private BigDecimal totalEmployerContribution;
        private BigDecimal totalWithdrawn;
        private LocalDate openedDate;
        private boolean active;
    }
}
