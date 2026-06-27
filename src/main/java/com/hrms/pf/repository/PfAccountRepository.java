package com.hrms.pf.repository;

import com.hrms.pf.entity.PfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PfAccountRepository extends JpaRepository<PfAccount, Long> {

    Optional<PfAccount> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);

    List<PfAccount> findByActiveTrue();
}
