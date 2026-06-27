package com.hrms.leave;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.leave.config.LeavePolicy;
import com.hrms.leave.dto.LeaveApplyRequest;
import com.hrms.leave.dto.LeaveResponse;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.entity.LeaveStatus;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private LeavePolicy leavePolicy;

    @InjectMocks
    private LeaveService leaveService;

    private Employee employee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        leavePolicy = new LeavePolicy();
        leaveService = new LeaveService(leaveRequestRepository, leaveBalanceRepository,
                employeeRepository, userRepository, leavePolicy, auditService);

        User employeeUser = new User();
        employeeUser.setUsername("jdoe");

        User managerUser = new User();
        managerUser.setUsername("mgr1");

        manager = new Employee();
        manager.setId(2L);
        manager.setFirstName("Mark");
        manager.setLastName("Manager");
        manager.setUser(managerUser);

        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setEmployeeCode("EMP-2026-00001");
        employee.setUser(employeeUser);
        employee.setManager(manager);

        SecurityContextHolder.clearContext();
    }

    @Test
    void applyLeave_withSufficientBalance_createsRequest() {
        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.ANNUAL_LEAVE);
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(7)); // 3 days inclusive

        when(employeeRepository.findByUser_UsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(employee));

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(LeaveType.ANNUAL_LEAVE);
        balance.setAllocated(BigDecimal.valueOf(20));

        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(eq(1L), eq(LeaveType.ANNUAL_LEAVE), any(Integer.class)))
                .thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest lr = inv.getArgument(0);
            lr.setId(100L);
            return lr;
        });

        LeaveResponse response = leaveService.applyLeave("jdoe", request);

        assertThat(response.getDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(response.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verify(auditService).logSuccess(eq("LEAVE_APPLY"), eq("LeaveRequest"), eq("100"), any());
    }

    @Test
    void applyLeave_withInsufficientBalance_throwsBadRequest() {
        LeaveApplyRequest request = new LeaveApplyRequest();
        request.setLeaveType(LeaveType.CASUAL_LEAVE);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(20)); // 20 days, exceeds 10-day casual entitlement

        when(employeeRepository.findByUser_UsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(employee));

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(LeaveType.CASUAL_LEAVE);
        balance.setAllocated(BigDecimal.valueOf(10));

        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(eq(1L), eq(LeaveType.CASUAL_LEAVE), any(Integer.class)))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> leaveService.applyLeave("jdoe", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void approveLeave_byDirectManager_succeedsAndDeductsBalance() {
        authenticateAs("mgr1", "ROLE_MANAGER");

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(10L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(LeaveType.ANNUAL_LEAVE);
        leaveRequest.setStartDate(LocalDate.now().plusDays(5));
        leaveRequest.setEndDate(LocalDate.now().plusDays(6));
        leaveRequest.setDays(BigDecimal.valueOf(2));
        leaveRequest.setStatus(LeaveStatus.PENDING);

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setAllocated(BigDecimal.valueOf(20));

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(eq(1L), eq(LeaveType.ANNUAL_LEAVE), any(Integer.class)))
                .thenReturn(Optional.of(balance));
        when(userRepository.findByUsernameAndDeletedFalse("mgr1")).thenReturn(Optional.of(manager.getUser()));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.approveLeave("mgr1", 10L, null);

        assertThat(response.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(balance.getUsed()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    void approveLeave_byUnrelatedManager_throwsBadRequest() {
        authenticateAs("someone-else", "ROLE_MANAGER");

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setId(10L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveService.approveLeave("someone-else", 10L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not authorized");
    }

    private void authenticateAs(String username, String... authorities) {
        var authList = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, authList));
    }
}
