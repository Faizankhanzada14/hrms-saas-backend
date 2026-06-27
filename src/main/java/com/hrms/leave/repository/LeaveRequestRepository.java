package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdAndStatusOrderByStartDateDesc(Long employeeId, LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findByEmployeeIdAndLeaveTypeAndStatus(Long employeeId, LeaveType leaveType, LeaveStatus status);

    Page<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status, Pageable pageable);

    /** Pending leave requests for employees reporting (directly) to the given manager. */
    Page<LeaveRequest> findByEmployee_Manager_IdAndStatusOrderByCreatedAtAsc(Long managerId, LeaveStatus status, Pageable pageable);

    long countByStatus(LeaveStatus status);

    @Query("select lr.leaveType as leaveType, sum(lr.days) as totalDays from LeaveRequest lr " +
            "where lr.status = 'APPROVED' and year(lr.startDate) = :year group by lr.leaveType")
    List<LeaveTypeDaysProjection> sumApprovedDaysByTypeForYear(@org.springframework.data.repository.query.Param("year") int year);

    interface LeaveTypeDaysProjection {
        LeaveType getLeaveType();
        java.math.BigDecimal getTotalDays();
    }
}
