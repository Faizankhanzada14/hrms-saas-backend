package com.hrms.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single category -> value pair. Deliberately shape-identical for bar, pie, and donut
 * charts — those three differ only in how the frontend renders the same data, not in what
 * data they need. One DTO, three chart types.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPoint {
    private String label;
    private BigDecimal value;

    public static ChartDataPoint of(String label, long value) {
        return new ChartDataPoint(label, BigDecimal.valueOf(value));
    }

    public static ChartDataPoint of(String label, BigDecimal value) {
        return new ChartDataPoint(label, value == null ? BigDecimal.ZERO : value);
    }
}
