package com.hrms.pf.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.pf.config.PfPolicy;
import com.hrms.pf.dto.PfAccountDtos.AccountResponse;
import com.hrms.pf.dto.PfAccountDtos.OpenAccountRequest;
import com.hrms.pf.dto.PfLedgerEntryResponse;
import com.hrms.pf.dto.PfWithdrawalDtos.DecisionRequest;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalRequest;
import com.hrms.pf.dto.PfWithdrawalDtos.WithdrawalResponse;
import com.hrms.pf.entity.*;
import com.hrms.pf.repository.PfAccountRepository;
import com.hrms.pf.repository.PfLedgerEntryRepository;
import com.hrms.pf.repository.PfWithdrawalRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PfService {

    private final PfAccountRepository pfAccountRepository;
    private final PfLedgerEntryRepository pfLedgerEntryRepository;
    private final PfWithdrawalRepository pfWithdrawalRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PfPolicy pfPolicy;
    private final AuditService auditService;

    // ---------- Account lifecycle ----------

    @Transactional
    public AccountResponse openAccount(Long employeeId, OpenAccountRequest request) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (pfAccountRepository.existsByEmployeeId(employeeId)) {
            throw new DuplicateResourceException("This employee already has a PF account");
        }

        PfAccount account = new PfAccount();
        account.setEmployee(employee);
        account.setEmployeeContributionRate(request != null && request.getEmployeeContributionRate() != null
                ? request.getEmployeeContributionRate() : pfPolicy.getDefaultEmployeeContributionRate());
        account.setEmployerContributionRate(request != null && request.getEmployerContributionRate() != null
                ? request.getEmployerContributionRate() : pfPolicy.getDefaultEmployerContributionRate());

        BigDecimal opening = request != null && request.getOpeningBalance() != null
                ? request.getOpeningBalance() : BigDecimal.ZERO;
        account.setOpeningBalance(opening);
        account.setBalance(opening);

        PfAccount saved = pfAccountRepository.save(account);

        if (opening.compareTo(BigDecimal.ZERO) > 0) {
            postLedgerEntry(saved, PfEntryType.OPENING_BALANCE, opening, null, "Opening balance on account creation");
        }

        auditService.logSuccess("PF_ACCOUNT_OPEN", "PfAccount", saved.getId().toString(),
                "PF account opened for " + employee.getEmployeeCode());

        return toAccountResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long employeeId) {
        return toAccountResponse(getAccountOrThrow(employeeId));
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String username) {
        Employee employee = resolveEmployee(username);
        return toAccountResponse(getAccountOrThrow(employee.getId()));
    }

    // ---------- Contributions (used by the monthly scheduler, exposed for manual posting too) ----------

    /**
     * Posts that month's employee + employer contributions for one account. Idempotent per
     * calendar month via {@code lastContributionMonth}, so re-running the job (or calling this
     * twice) for the same month is a no-op rather than a double-credit.
     */
    @Transactional
    public void postMonthlyContribution(PfAccount account, BigDecimal monthlySalary, String yearMonth) {
        if (yearMonth.equals(account.getLastContributionMonth())) {
            return; // already posted for this month
        }

        BigDecimal employeeAmount = monthlySalary
                .multiply(account.getEmployeeContributionRate())
                .divide(BigDecimal.valueOf(100));
        BigDecimal employerAmount = monthlySalary
                .multiply(account.getEmployerContributionRate())
                .divide(BigDecimal.valueOf(100));

        postLedgerEntry(account, PfEntryType.EMPLOYEE_CONTRIBUTION, employeeAmount, yearMonth,
                "Monthly employee contribution for " + yearMonth);
        account.setTotalEmployeeContribution(account.getTotalEmployeeContribution().add(employeeAmount));

        postLedgerEntry(account, PfEntryType.EMPLOYER_CONTRIBUTION, employerAmount, yearMonth,
                "Monthly employer contribution for " + yearMonth);
        account.setTotalEmployerContribution(account.getTotalEmployerContribution().add(employerAmount));

        account.setLastContributionMonth(yearMonth);
        pfAccountRepository.save(account);
    }

    // ---------- Withdrawals ----------

    @Transactional
    public WithdrawalResponse requestWithdrawal(String username, WithdrawalRequest request) {
        Employee employee = resolveEmployee(username);
        PfAccount account = getAccountOrThrow(employee.getId());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException(String.format(
                    "Withdrawal amount %s exceeds available PF balance of %s",
                    request.getAmount(), account.getBalance()));
        }

        PfWithdrawal withdrawal = new PfWithdrawal();
        withdrawal.setEmployee(employee);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setReason(request.getReason());
        withdrawal.setStatus(PfWithdrawalStatus.PENDING);

        PfWithdrawal saved = pfWithdrawalRepository.save(withdrawal);
        auditService.logSuccess("PF_WITHDRAWAL_REQUEST", "PfWithdrawal", saved.getId().toString(),
                employee.getEmployeeCode() + " requested PF withdrawal of " + request.getAmount());

        return toWithdrawalResponse(saved);
    }

    @Transactional
    public WithdrawalResponse decideWithdrawal(String approverUsername, Long withdrawalId,
                                                boolean approve, DecisionRequest decision) {
        PfWithdrawal withdrawal = pfWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("PF withdrawal request not found with id: " + withdrawalId));

        if (withdrawal.getStatus() != PfWithdrawalStatus.PENDING) {
            throw new BadRequestException("This withdrawal request has already been decided");
        }

        User approver = userRepository.findByUsernameAndDeletedFalse(approverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Approver user not found"));

        if (approve) {
            PfAccount account = getAccountOrThrow(withdrawal.getEmployee().getId());
            if (account.getBalance().compareTo(withdrawal.getAmount()) < 0) {
                throw new BadRequestException("Employee no longer has sufficient PF balance for this withdrawal");
            }
            postLedgerEntry(account, PfEntryType.WITHDRAWAL, withdrawal.getAmount().negate(), null,
                    "PF withdrawal approved by " + approverUsername);
            account.setTotalWithdrawn(account.getTotalWithdrawn().add(withdrawal.getAmount()));
            pfAccountRepository.save(account);
            withdrawal.setStatus(PfWithdrawalStatus.APPROVED);
        } else {
            withdrawal.setStatus(PfWithdrawalStatus.REJECTED);
        }

        withdrawal.setDecidedBy(approver);
        withdrawal.setDecisionRemarks(decision != null ? decision.getRemarks() : null);
        withdrawal.setDecidedAt(LocalDateTime.now());

        PfWithdrawal saved = pfWithdrawalRepository.save(withdrawal);
        auditService.logSuccess(approve ? "PF_WITHDRAWAL_APPROVE" : "PF_WITHDRAWAL_REJECT",
                "PfWithdrawal", saved.getId().toString(), "Decided by " + approverUsername);

        return toWithdrawalResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getMyWithdrawalHistory(String username, Pageable pageable) {
        Employee employee = resolveEmployee(username);
        return pfWithdrawalRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), pageable)
                .map(this::toWithdrawalResponse);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getPendingWithdrawals(Pageable pageable) {
        return pfWithdrawalRepository.findByStatusOrderByCreatedAtAsc(PfWithdrawalStatus.PENDING, pageable)
                .map(this::toWithdrawalResponse);
    }

    // ---------- Statement / Ledger ----------

    @Transactional(readOnly = true)
    public Page<PfLedgerEntryResponse> getMyStatement(String username, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Employee employee = resolveEmployee(username);
        PfAccount account = getAccountOrThrow(employee.getId());
        return statementFor(account.getId(), from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PfLedgerEntryResponse> getEmployeeStatement(Long employeeId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        PfAccount account = getAccountOrThrow(employeeId);
        return statementFor(account.getId(), from, to, pageable);
    }

    /** Org-wide ledger across all PF accounts, for Finance auditing / reconciliation. */
    @Transactional(readOnly = true)
    public Page<PfLedgerEntryResponse> getFullLedger(Pageable pageable) {
        return pfLedgerEntryRepository.findAllByOrderByTransactionDateDesc(pageable).map(this::toLedgerResponse);
    }

    private Page<PfLedgerEntryResponse> statementFor(Long accountId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        return pfLedgerEntryRepository.findStatement(accountId, effectiveFrom, effectiveTo, pageable)
                .map(this::toLedgerResponse);
    }

    // ---------- Internal helpers ----------

    private void postLedgerEntry(PfAccount account, PfEntryType type, BigDecimal amount,
                                  String referenceMonth, String description) {
        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        pfAccountRepository.save(account);

        PfLedgerEntry entry = new PfLedgerEntry();
        entry.setPfAccount(account);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(newBalance);
        entry.setReferenceMonth(referenceMonth);
        entry.setDescription(description);
        pfLedgerEntryRepository.save(entry);
    }

    private PfAccount getAccountOrThrow(Long employeeId) {
        return pfAccountRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No PF account exists for employee id: " + employeeId));
    }

    private Employee resolveEmployee(String username) {
        return employeeRepository.findByUser_UsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to user: " + username));
    }

    private AccountResponse toAccountResponse(PfAccount account) {
        Employee employee = account.getEmployee();
        return AccountResponse.builder()
                .id(account.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .employeeContributionRate(account.getEmployeeContributionRate())
                .employerContributionRate(account.getEmployerContributionRate())
                .openingBalance(account.getOpeningBalance())
                .balance(account.getBalance())
                .totalEmployeeContribution(account.getTotalEmployeeContribution())
                .totalEmployerContribution(account.getTotalEmployerContribution())
                .totalWithdrawn(account.getTotalWithdrawn())
                .openedDate(account.getOpenedDate())
                .active(account.isActive())
                .build();
    }

    private WithdrawalResponse toWithdrawalResponse(PfWithdrawal withdrawal) {
        Employee employee = withdrawal.getEmployee();
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .amount(withdrawal.getAmount())
                .reason(withdrawal.getReason())
                .status(withdrawal.getStatus())
                .decidedByUsername(withdrawal.getDecidedBy() != null ? withdrawal.getDecidedBy().getUsername() : null)
                .decisionRemarks(withdrawal.getDecisionRemarks())
                .decidedAt(withdrawal.getDecidedAt())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }

    private PfLedgerEntryResponse toLedgerResponse(PfLedgerEntry entry) {
        return PfLedgerEntryResponse.builder()
                .id(entry.getId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .referenceMonth(entry.getReferenceMonth())
                .description(entry.getDescription())
                .transactionDate(entry.getTransactionDate())
                .build();
    }
}
