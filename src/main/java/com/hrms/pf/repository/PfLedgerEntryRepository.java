package com.hrms.pf.repository;

import com.hrms.pf.entity.PfLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PfLedgerEntryRepository extends JpaRepository<PfLedgerEntry, Long> {

    Page<PfLedgerEntry> findByPfAccountIdOrderByTransactionDateDesc(Long pfAccountId, Pageable pageable);

    @Query("select e from PfLedgerEntry e where e.pfAccount.id = :accountId " +
            "and e.transactionDate between :from and :to order by e.transactionDate desc")
    Page<PfLedgerEntry> findStatement(@Param("accountId") Long accountId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       Pageable pageable);

    /** Org-wide ledger view for admin/finance auditing, across all accounts. */
    Page<PfLedgerEntry> findAllByOrderByTransactionDateDesc(Pageable pageable);
}
