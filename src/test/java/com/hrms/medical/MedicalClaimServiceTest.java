package com.hrms.medical;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.medical.dto.MedicalClaimDtos.ClaimRequest;
import com.hrms.medical.dto.MedicalClaimResponse;
import com.hrms.medical.entity.ClaimStatus;
import com.hrms.medical.entity.MedicalClaim;
import com.hrms.medical.repository.MedicalClaimRepository;
import com.hrms.medical.service.FileStorageService;
import com.hrms.medical.service.MedicalClaimService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalClaimServiceTest {

    @Mock private MedicalClaimRepository medicalClaimRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuditService auditService;

    private MedicalClaimService service;

    private Employee employee;
    private User hrUser;
    private User financeUser;

    @BeforeEach
    void setUp() {
        service = new MedicalClaimService(medicalClaimRepository, employeeRepository, userRepository,
                fileStorageService, auditService);

        User employeeUser = new User();
        employeeUser.setUsername("jdoe");

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("EMP-2026-00001");
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
        employee.setUser(employeeUser);
        employee.setMedicalAllocation(BigDecimal.valueOf(5000));

        hrUser = new User();
        hrUser.setUsername("hradmin");

        financeUser = new User();
        financeUser.setUsername("financeuser");
    }

    @Test
    void createClaim_savesWithSubmittedStatus() {
        ClaimRequest request = new ClaimRequest();
        request.setDescription("Dental checkup");
        request.setAmount(BigDecimal.valueOf(1500));
        request.setClaimDate(LocalDate.now());

        when(employeeRepository.findByUser_UsernameAndDeletedFalse("jdoe")).thenReturn(Optional.of(employee));
        when(medicalClaimRepository.save(any(MedicalClaim.class))).thenAnswer(inv -> {
            MedicalClaim c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });

        MedicalClaimResponse response = service.createClaim("jdoe", request, null);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        verify(auditService).logSuccess(eq("MEDICAL_CLAIM_CREATE"), eq("MedicalClaim"), eq("50"), any());
    }

    @Test
    void financeApprove_withinBudget_succeeds() {
        MedicalClaim claim = buildClaim(ClaimStatus.HR_APPROVED, BigDecimal.valueOf(2000));

        when(medicalClaimRepository.findById(10L)).thenReturn(Optional.of(claim));
        when(medicalClaimRepository.sumApprovedAmountForEmployeeAndYear(eq(1L), any(Integer.class)))
                .thenReturn(BigDecimal.valueOf(1000)); // already used 1000 of 5000
        when(userRepository.findByUsernameAndDeletedFalse("financeuser")).thenReturn(Optional.of(financeUser));
        when(medicalClaimRepository.save(any(MedicalClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicalClaimResponse response = service.financeApprove("financeuser", 10L, null);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.FINANCE_APPROVED);
    }

    @Test
    void financeApprove_exceedingBudget_throwsBadRequest() {
        MedicalClaim claim = buildClaim(ClaimStatus.HR_APPROVED, BigDecimal.valueOf(4500));

        when(medicalClaimRepository.findById(10L)).thenReturn(Optional.of(claim));
        when(medicalClaimRepository.sumApprovedAmountForEmployeeAndYear(eq(1L), any(Integer.class)))
                .thenReturn(BigDecimal.valueOf(1000)); // 1000 used, 4000 remaining of 5000; claim is 4500

        assertThatThrownBy(() -> service.financeApprove("financeuser", 10L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds remaining medical budget");

        verify(medicalClaimRepository, never()).save(any());
    }

    @Test
    void financeApprove_onClaimNotYetHrApproved_throwsBadRequest() {
        MedicalClaim claim = buildClaim(ClaimStatus.SUBMITTED, BigDecimal.valueOf(500));
        when(medicalClaimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.financeApprove("financeuser", 10L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already approved by HR");
    }

    @Test
    void hrApprove_onAlreadyDecidedClaim_throwsBadRequest() {
        MedicalClaim claim = buildClaim(ClaimStatus.HR_REJECTED, BigDecimal.valueOf(500));
        when(medicalClaimRepository.findById(10L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.hrApprove("hradmin", 10L, null))
                .isInstanceOf(BadRequestException.class);
    }

    private MedicalClaim buildClaim(ClaimStatus status, BigDecimal amount) {
        MedicalClaim claim = new MedicalClaim();
        claim.setId(10L);
        claim.setEmployee(employee);
        claim.setDescription("Test claim");
        claim.setAmount(amount);
        claim.setClaimDate(LocalDate.now());
        claim.setStatus(status);
        return claim;
    }
}
