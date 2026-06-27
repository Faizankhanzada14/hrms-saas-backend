package com.hrms.pf.dto;

import com.hrms.pf.entity.PfEntryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PfLedgerEntryResponse {
    private Long id;
    private PfEntryType entryType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceMonth;
    private String description;
    private LocalDateTime transactionDate;
}
