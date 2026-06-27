package com.hrms.employee.repository;

import com.hrms.employee.entity.PostingUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostingUnitRepository extends JpaRepository<PostingUnit, Long> {
}
