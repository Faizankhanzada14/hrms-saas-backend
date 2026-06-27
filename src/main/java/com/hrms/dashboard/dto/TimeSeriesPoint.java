package com.hrms.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One point on a line/area trend chart: a calendar period (e.g. "2026-03") and its value. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {
    private String period;
    private BigDecimal value;

    public static TimeSeriesPoint of(String period, long value) {
        return new TimeSeriesPoint(period, BigDecimal.valueOf(value));
    }
}
