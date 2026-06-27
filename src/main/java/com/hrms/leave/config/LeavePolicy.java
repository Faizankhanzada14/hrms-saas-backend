package com.hrms.leave.config;

import com.hrms.leave.entity.LeaveType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Centralizes the business rules around leave entitlement and carry-forward,
 * so policy changes don't require touching service logic.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "hrms.leave-policy")
public class LeavePolicy {

    /** Annual entitlement (days/year) per leave type. */
    private Map<LeaveType, BigDecimal> annualEntitlement = defaultEntitlement();

    /** Maximum days that may be carried forward into the next year, per leave type. */
    private Map<LeaveType, BigDecimal> maxCarryForward = defaultCarryForward();

    private static Map<LeaveType, BigDecimal> defaultEntitlement() {
        Map<LeaveType, BigDecimal> map = new EnumMap<>(LeaveType.class);
        map.put(LeaveType.ANNUAL_LEAVE, BigDecimal.valueOf(20));
        map.put(LeaveType.CASUAL_LEAVE, BigDecimal.valueOf(10));
        map.put(LeaveType.SICK_LEAVE, BigDecimal.valueOf(10));
        return map;
    }

    private static Map<LeaveType, BigDecimal> defaultCarryForward() {
        Map<LeaveType, BigDecimal> map = new EnumMap<>(LeaveType.class);
        map.put(LeaveType.ANNUAL_LEAVE, BigDecimal.valueOf(10));
        map.put(LeaveType.CASUAL_LEAVE, BigDecimal.ZERO);
        map.put(LeaveType.SICK_LEAVE, BigDecimal.ZERO);
        return map;
    }

    public BigDecimal entitlementFor(LeaveType type) {
        return annualEntitlement.getOrDefault(type, BigDecimal.ZERO);
    }

    public BigDecimal maxCarryForwardFor(LeaveType type) {
        return maxCarryForward.getOrDefault(type, BigDecimal.ZERO);
    }

    /** Working days in a month used to derive a daily salary rate for encashment. */
    private int workingDaysPerMonth = 30;
}
