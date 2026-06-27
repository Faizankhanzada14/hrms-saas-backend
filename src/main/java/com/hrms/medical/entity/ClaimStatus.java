package com.hrms.medical.entity;

/**
 * Workflow: SUBMITTED (by employee) -> HR_APPROVED (forwarded to Finance) or HR_REJECTED (final)
 *           -> FINANCE_APPROVED (final, budget consumed) or FINANCE_REJECTED (final).
 */
public enum ClaimStatus {
    SUBMITTED,
    HR_APPROVED,
    HR_REJECTED,
    FINANCE_APPROVED,
    FINANCE_REJECTED
}
