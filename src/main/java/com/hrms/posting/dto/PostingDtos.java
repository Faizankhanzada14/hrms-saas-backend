package com.hrms.posting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PostingDtos {

    @Getter
    @Setter
    public static class TransferRequest {
        private Long newPostingUnitId;
        private Long newDepartmentId;

        @NotNull(message = "Transfer date is required")
        private LocalDate transferDate;

        private String reason;
    }

    @Getter
    @Setter
    @Builder
    public static class PostingHistoryResponse {
        private Long id;
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private Long fromPostingUnitId;
        private String fromPostingUnitName;
        private Long toPostingUnitId;
        private String toPostingUnitName;
        private Long fromDepartmentId;
        private String fromDepartmentName;
        private Long toDepartmentId;
        private String toDepartmentName;
        private LocalDate transferDate;
        private String reason;
        private String transferredByUsername;
        private LocalDateTime createdAt;
    }
}
