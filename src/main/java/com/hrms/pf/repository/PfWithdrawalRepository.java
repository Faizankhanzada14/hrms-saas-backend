package com.hrms.pf.repository;

import com.hrms.pf.entity.PfWithdrawal;
import com.hrms.pf.entity.PfWithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PfWithdrawalRepository extends JpaRepository<PfWithdrawal, Long> {

    Page<PfWithdrawal> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);

    Page<PfWithdrawal> findByStatusOrderByCreatedAtAsc(PfWithdrawalStatus status, Pageable pageable);

    long countByStatus(PfWithdrawalStatus status);
}
