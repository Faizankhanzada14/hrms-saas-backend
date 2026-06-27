package com.hrms.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeSummaryResponse {
    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;
}
