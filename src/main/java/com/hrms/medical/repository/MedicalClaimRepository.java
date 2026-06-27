package com.hrms.medical.repository;

import com.hrms.medical.entity.ClaimStatus;
import com.hrms.medical.entity.MedicalClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface MedicalClaimRepository extends JpaRepository<MedicalClaim, Long> {

    Page<MedicalClaim> findByEmployeeIdOrderByClaimDateDesc(Long employeeId, Pageable pageable);

    Page<MedicalClaim> findByStatusOrderByCreatedAtAsc(ClaimStatus status, Pageable pageable);

    /** Total amount already approved by Finance for this employee within the given calendar year. */
    @Query("select coalesce(sum(c.amount), 0) from MedicalClaim c " +
            "where c.employee.id = :employeeId and c.status = 'FINANCE_APPROVED' " +
            "and year(c.claimDate) = :year")
    BigDecimal sumApprovedAmountForEmployeeAndYear(@Param("employeeId") Long employeeId, @Param("year") int year);

    @Query("select coalesce(sum(c.amount), 0) from MedicalClaim c " +
            "where c.status = 'FINANCE_APPROVED' and year(c.claimDate) = :year")
    BigDecimal sumApprovedAmountForYear(@Param("year") int year);

    long countByStatus(ClaimStatus status);
}
