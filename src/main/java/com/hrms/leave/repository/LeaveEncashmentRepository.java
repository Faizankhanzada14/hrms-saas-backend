package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveEncashment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveEncashmentRepository extends JpaRepository<LeaveEncashment, Long> {

    Page<LeaveEncashment> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);
}
