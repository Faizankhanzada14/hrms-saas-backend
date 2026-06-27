package com.hrms.medical.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class MedicalBudgetResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private int year;
    private BigDecimal allocated;
    private BigDecimal used;
    private BigDecimal remaining;
}
