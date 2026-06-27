package com.hrms.posting.repository;

import com.hrms.posting.entity.PostingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostingHistoryRepository extends JpaRepository<PostingHistory, Long> {

    Page<PostingHistory> findByEmployeeIdOrderByTransferDateDesc(Long employeeId, Pageable pageable);
}
