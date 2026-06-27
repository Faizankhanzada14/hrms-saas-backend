package com.hrms.pf.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Default PF contribution rates applied when an account is opened (or auto-opened by the
 * monthly accrual job) without explicit rates. Individual accounts may override these.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "hrms.pf-policy")
public class PfPolicy {

    /** Employee contribution rate as a percentage of monthly salary, e.g. 10.00 = 10%. */
    private BigDecimal defaultEmployeeContributionRate = BigDecimal.valueOf(10);

    /** Employer (employer-matched) contribution rate as a percentage of monthly salary. */
    private BigDecimal defaultEmployerContributionRate = BigDecimal.valueOf(10);

    /** Auto-open a PF account for active employees who don't have one yet during accrual. */
    private boolean autoOpenAccounts = true;
}
