package com.hrms.pf;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.pf.config.PfPolicy;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalRequest;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalResponse;
import com.hrms.pf.entity.PfAccount;
import com.hrms.pf.entity.PfWithdrawal;
import com.hrms.pf.entity.PfWithdrawalStatus;
import com.hrms.pf.repository.PfAccountRepository;
import com.hrms.pf.repository.PfLedgerEntryRepository;
import com.hrms.pf.repository.PfWithdrawalRepository;
import com.hrms.pf.service.PfService;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PfServiceTest {

    @Mock private PfAccountRepository pfAccountRepository;
    @Mock private PfLedgerEntryRepository pfLedgerEntryRepository;
    @Mock private PfWithdrawalRepository pfWithdrawalRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private PfPolicy pfPolicy;
    private PfService pfService;

    private Employee employee;
    private PfAccount account;

    @BeforeEach
    void setUp() {
        pfPolicy = new PfPolicy();
        pfService = new PfService(pfAccountRepository, pfLedgerEntryRepository, pfWithdrawalRepository,
                employeeRepository, userRepository, pfPolicy, auditService);

        User employeeUser = new User();
        employeeUser.setUsername("jdoe");

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP-2026-00001");
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setUser(employeeUser);
        employee.setSalary(BigDecimal.valueOf(100000));

        account = new PfAccount();
        account.setId(1L);
        account.setEmployee(employee);
        account.setEmployeeContributionRate(BigDecimal.valueOf(10));
        account.setEmployerContributionRate(BigDecimal.valueOf(10));
        account.setBalance(BigDecimal.valueOf(5000));
        account.setOpenedDate(LocalDate.now());
        account.setActive(true);

        when(pfAccountRepository.save(any(PfAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void postMonthlyContribution_creditsBothEmployeeAndEmployerAmounts() {
        pfService.postMonthlyContribution(account, BigDecimal.valueOf(100000), "2026-01");

        // 10% of 100000 = 10000 each side, balance starts at 5000 -> 5000 + 10000 + 10000 = 25000
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(account.getTotalEmployeeContribution()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(account.getTotalEmployerContribution()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(account.getLastContributionMonth()).isEqualTo("2026-01");
        verify(pfLedgerEntryRepository, times(2)).save(any());
    }

    @Test
    void postMonthlyContribution_isIdempotentForSameMonth() {
        account.setLastContributionMonth("2026-01");

        pfService.postMonthlyContribution(account, BigDecimal.valueOf(100000), "2026-01");

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5000)); // unchanged
        verify(pfLedgerEntryRepository, never()).save(any());
    }

    @Test
    void requestWithdrawal_exceedingBalance_throwsBadRequest() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(BigDecimal.valueOf(10000)); // balance is only 5000

        when(employeeRepository.findByUser_UsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(employee));
        when(pfAccountRepository.findByEmployeeId(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> pfService.requestWithdrawal("jdoe", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds available PF balance");

        verify(pfWithdrawalRepository, never()).save(any());
    }

    @Test
    void decideWithdrawal_approve_deductsBalanceAndPostsLedgerEntry() {
        PfWithdrawal withdrawal = new PfWithdrawal();
        withdrawal.setId(20L);
        withdrawal.setEmployee(employee);
        withdrawal.setAmount(BigDecimal.valueOf(2000));
        withdrawal.setStatus(PfWithdrawalStatus.PENDING);

        User approver = new User();
        approver.setUsername("financeuser");

        when(pfWithdrawalRepository.findById(20L)).thenReturn(Optional.of(withdrawal));
        when(userRepository.findByUsernameAndDeletedFalse("financeuser")).thenReturn(Optional.of(approver));
        when(pfAccountRepository.findByEmployeeId(1L)).thenReturn(Optional.of(account));
        when(pfWithdrawalRepository.save(any(PfWithdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

        WithdrawalResponse response = pfService.decideWithdrawal("financeuser", 20L, true, null);

        assertThat(response.getStatus()).isEqualTo(PfWithdrawalStatus.APPROVED);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(3000)); // 5000 - 2000
        assertThat(account.getTotalWithdrawn()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }
}
