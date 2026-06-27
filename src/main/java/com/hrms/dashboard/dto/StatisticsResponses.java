package com.hrms.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class StatisticsResponses {

    @Getter
    @Builder
    public static class LeaveStatisticsResponse {
        private int year;
        private List<ChartDataPoint> requestsByStatus;
        private List<ChartDataPoint> daysApprovedByType;
        private long pendingApprovals;
    }

    @Getter
    @Builder
    public static class MedicalStatisticsResponse {
        private int year;
        private List<ChartDataPoint> claimsByStatus;
        private BigDecimal totalApprovedAmount;
        private long pendingHrReview;
        private long pendingFinanceReview;
    }

    @Getter
    @Builder
    public static class PfStatisticsResponse {
        private long totalActiveAccounts;
        private BigDecimal totalBalanceAcrossAccounts;
        private BigDecimal totalEmployeeContributions;
        private BigDecimal totalEmployerContributions;
        private BigDecimal totalWithdrawn;
        private List<ChartDataPoint> withdrawalsByStatus;
    }
}
