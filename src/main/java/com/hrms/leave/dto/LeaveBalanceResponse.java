package com.hrms.leave.dto;

import com.hrms.leave.entity.LeaveType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class LeaveBalanceResponse {
    private LeaveType leaveType;
    private int year;
    private BigDecimal allocated;
    private BigDecimal carriedForward;
    private BigDecimal used;
    private BigDecimal encashed;
    private BigDecimal available;
}
