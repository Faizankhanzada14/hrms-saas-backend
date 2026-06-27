package com.hrms.employee.repository;

import com.hrms.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByIdAndDeletedFalse(Long id);

    Optional<Employee> findByUser_UsernameAndDeletedFalse(String username);

    Optional<Employee> findByUser_IdAndDeletedFalse(Long userId);

    Optional<Employee> findByEmployeeCodeAndDeletedFalse(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByCnic(String cnic);

    List<Employee> findByManagerIdAndDeletedFalse(Long managerId);

    long countByDeletedFalseAndActiveTrue();

    long countByDeletedFalse();
}
