package com.hrms.dashboard;

import com.hrms.dashboard.dto.ChartDataPoint;
import com.hrms.dashboard.dto.EmployeeSummaryResponse;
import com.hrms.dashboard.dto.TimeSeriesPoint;
import com.hrms.dashboard.service.DashboardService;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.medical.repository.MedicalClaimRepository;
import com.hrms.pf.repository.PfAccountRepository;
import com.hrms.pf.repository.PfWithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private MedicalClaimRepository medicalClaimRepository;
    @Mock private PfAccountRepository pfAccountRepository;
    @Mock private PfWithdrawalRepository pfWithdrawalRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Employee makeEmployee(boolean active, boolean deleted, String deptName, LocalDate joiningDate) {
        Employee e = new Employee();
        e.setActive(active);
        e.setDeleted(deleted);
        if (deptName != null) {
            Department dept = new Department();
            dept.setName(deptName);
            e.setDepartment(dept);
        }
        e.setJoiningDate(joiningDate);
        e.setEmploymentStatus(Employee.EmploymentStatus.ACTIVE);
        return e;
    }

    @Test
    void employeeSummary_excludesDeletedAndCountsActiveInactive() {
        List<Employee> employees = List.of(
                makeEmployee(true, false, "Engineering", LocalDate.of(2026, 1, 10)),
                makeEmployee(true, false, "Engineering", LocalDate.of(2026, 2, 5)),
                makeEmployee(false, false, "Sales", LocalDate.of(2025, 6, 1)),
                makeEmployee(true, true, "Sales", LocalDate.of(2025, 7, 1)) // deleted, should be excluded
        );
        when(employeeRepository.findAll()).thenReturn(employees);

        EmployeeSummaryResponse summary = dashboardService.getEmployeeSummary();

        assertThat(summary.getTotalEmployees()).isEqualTo(3); // 4 minus 1 deleted
        assertThat(summary.getActiveEmployees()).isEqualTo(2);
        assertThat(summary.getInactiveEmployees()).isEqualTo(1);
    }

    @Test
    void employeesByDepartment_groupsCorrectly() {
        List<Employee> employees = List.of(
                makeEmployee(true, false, "Engineering", LocalDate.now()),
                makeEmployee(true, false, "Engineering", LocalDate.now()),
                makeEmployee(true, false, "Sales", LocalDate.now())
        );
        when(employeeRepository.findAll()).thenReturn(employees);

        List<ChartDataPoint> result = dashboardService.getEmployeesByDepartment();

        assertThat(result).hasSize(2);
        ChartDataPoint engineering = result.stream().filter(c -> c.getLabel().equals("Engineering")).findFirst().orElseThrow();
        assertThat(engineering.getValue().intValue()).isEqualTo(2);
    }

    @Test
    void monthlyJoiningTrend_zeroFillsMonthsWithNoHires() {
        List<Employee> employees = List.of(
                makeEmployee(true, false, "Engineering", LocalDate.of(2026, 3, 15)),
                makeEmployee(true, false, "Engineering", LocalDate.of(2026, 3, 20)),
                makeEmployee(true, false, "Sales", LocalDate.of(2025, 12, 1)) // different year, excluded
        );
        when(employeeRepository.findAll()).thenReturn(employees);

        List<TimeSeriesPoint> trend = dashboardService.getMonthlyJoiningTrend(2026);

        assertThat(trend).hasSize(12); // always 12 months, zero-filled
        TimeSeriesPoint march = trend.stream().filter(p -> p.getPeriod().equals("2026-03")).findFirst().orElseThrow();
        assertThat(march.getValue().intValue()).isEqualTo(2);
        TimeSeriesPoint january = trend.stream().filter(p -> p.getPeriod().equals("2026-01")).findFirst().orElseThrow();
        assertThat(january.getValue().intValue()).isZero();
    }
}
