package com.hrms.dashboard.service;

import com.hrms.dashboard.dto.ChartDataPoint;
import com.hrms.dashboard.dto.EmployeeSummaryResponse;
import com.hrms.dashboard.dto.StatisticsResponses.LeaveStatisticsResponse;
import com.hrms.dashboard.dto.StatisticsResponses.MedicalStatisticsResponse;
import com.hrms.dashboard.dto.StatisticsResponses.PfStatisticsResponse;
import com.hrms.dashboard.dto.TimeSeriesPoint;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.medical.entity.ClaimStatus;
import com.hrms.medical.repository.MedicalClaimRepository;
import com.hrms.pf.entity.PfWithdrawalStatus;
import com.hrms.pf.repository.PfAccountRepository;
import com.hrms.pf.repository.PfWithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregation is done in-memory over the (non-deleted) employee set rather than with
 * per-dimension SQL GROUP BY queries. For typical HRMS org sizes (hundreds to low thousands
 * of employees) this is simpler to read and maintain than a dozen near-identical native
 * queries; if this is ever pointed at a 100k+ employee org, swap these for DB-side aggregation.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MedicalClaimRepository medicalClaimRepository;
    private final PfAccountRepository pfAccountRepository;
    private final PfWithdrawalRepository pfWithdrawalRepository;

    // ---------- Employee dashboard ----------

    @Transactional(readOnly = true)
    public EmployeeSummaryResponse getEmployeeSummary() {
        List<Employee> employees = activeEmployeeUniverse();
        long active = employees.stream().filter(Employee::isActive).count();
        return EmployeeSummaryResponse.builder()
                .totalEmployees(employees.size())
                .activeEmployees(active)
                .inactiveEmployees(employees.size() - active)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getEmployeesByDepartment() {
        return groupCount(activeEmployeeUniverse(),
                e -> e.getDepartment() != null ? e.getDepartment().getName() : "Unassigned");
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getEmployeesByGender() {
        return groupCount(activeEmployeeUniverse(),
                e -> e.getGender() != null ? e.getGender().name() : "UNSPECIFIED");
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getEmployeesByBranch() {
        return groupCount(activeEmployeeUniverse(), e -> {
            if (e.getPostingUnit() == null) {
                return "Unassigned";
            }
            return e.getPostingUnit().getBranchName() != null
                    ? e.getPostingUnit().getBranchName()
                    : e.getPostingUnit().getName();
        });
    }

    @Transactional(readOnly = true)
    public List<ChartDataPoint> getEmployeesByStatus() {
        return groupCount(activeEmployeeUniverse(), e -> e.getEmploymentStatus().name());
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getMonthlyJoiningTrend(int year) {
        return monthlyTrend(activeEmployeeUniverse(), Employee::getJoiningDate, year);
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getMonthlyResignationTrend(int year) {
        return monthlyTrend(activeEmployeeUniverse(), Employee::getResignationDate, year);
    }

    // ---------- Leave dashboard ----------

    @Transactional(readOnly = true)
    public LeaveStatisticsResponse getLeaveStatistics(Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();

        List<ChartDataPoint> byStatus = List.of(LeaveStatus.values()).stream()
                .map(status -> ChartDataPoint.of(status.name(), leaveRequestRepository.countByStatus(status)))
                .toList();

        List<ChartDataPoint> daysByType = leaveRequestRepository.sumApprovedDaysByTypeForYear(targetYear).stream()
                .map(p -> ChartDataPoint.of(p.getLeaveType().name(), p.getTotalDays()))
                .toList();

        return LeaveStatisticsResponse.builder()
                .year(targetYear)
                .requestsByStatus(byStatus)
                .daysApprovedByType(daysByType)
                .pendingApprovals(leaveRequestRepository.countByStatus(LeaveStatus.PENDING))
                .build();
    }

    // ---------- Medical dashboard ----------

    @Transactional(readOnly = true)
    public MedicalStatisticsResponse getMedicalStatistics(Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();

        List<ChartDataPoint> byStatus = List.of(ClaimStatus.values()).stream()
                .map(status -> ChartDataPoint.of(status.name(), medicalClaimRepository.countByStatus(status)))
                .toList();

        return MedicalStatisticsResponse.builder()
                .year(targetYear)
                .claimsByStatus(byStatus)
                .totalApprovedAmount(medicalClaimRepository.sumApprovedAmountForYear(targetYear))
                .pendingHrReview(medicalClaimRepository.countByStatus(ClaimStatus.SUBMITTED))
                .pendingFinanceReview(medicalClaimRepository.countByStatus(ClaimStatus.HR_APPROVED))
                .build();
    }

    // ---------- PF dashboard ----------

    @Transactional(readOnly = true)
    public PfStatisticsResponse getPfStatistics() {
        var accounts = pfAccountRepository.findByActiveTrue();

        BigDecimal totalBalance = accounts.stream().map(a -> a.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployeeContrib = accounts.stream().map(a -> a.getTotalEmployeeContribution())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployerContrib = accounts.stream().map(a -> a.getTotalEmployerContribution())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawn = accounts.stream().map(a -> a.getTotalWithdrawn())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ChartDataPoint> withdrawalsByStatus = List.of(PfWithdrawalStatus.values()).stream()
                .map(status -> ChartDataPoint.of(status.name(), pfWithdrawalRepository.countByStatus(status)))
                .toList();

        return PfStatisticsResponse.builder()
                .totalActiveAccounts(accounts.size())
                .totalBalanceAcrossAccounts(totalBalance)
                .totalEmployeeContributions(totalEmployeeContrib)
                .totalEmployerContributions(totalEmployerContrib)
                .totalWithdrawn(totalWithdrawn)
                .withdrawalsByStatus(withdrawalsByStatus)
                .build();
    }

    // ---------- Helpers ----------

    private List<Employee> activeEmployeeUniverse() {
        return employeeRepository.findAll().stream().filter(e -> !e.isDeleted()).toList();
    }

    private List<ChartDataPoint> groupCount(List<Employee> employees,
                                             java.util.function.Function<Employee, String> classifier) {
        Map<String, Long> counts = employees.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> ChartDataPoint.of(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ChartDataPoint::getLabel))
                .toList();
    }

    private List<TimeSeriesPoint> monthlyTrend(List<Employee> employees,
                                                java.util.function.Function<Employee, LocalDate> dateExtractor,
                                                int year) {
        Map<String, Long> counts = employees.stream()
                .map(dateExtractor)
                .filter(date -> date != null && date.getYear() == year)
                .collect(Collectors.groupingBy(d -> d.format(MONTH_FORMAT), Collectors.counting()));

        List<TimeSeriesPoint> result = new ArrayList<>();
        for (Month month : Month.values()) {
            String key = LocalDate.of(year, month, 1).format(MONTH_FORMAT);
            result.add(TimeSeriesPoint.of(key, counts.getOrDefault(key, 0L)));
        }
        return result;
    }
}
