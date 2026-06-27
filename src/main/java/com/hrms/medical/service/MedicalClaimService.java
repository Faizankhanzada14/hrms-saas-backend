package com.hrms.medical.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.medical.dto.MedicalBudgetResponse;
import com.hrms.medical.dto.MedicalClaimDtos.ClaimRequest;
import com.hrms.medical.dto.MedicalClaimDtos.DecisionRequest;
import com.hrms.medical.dto.MedicalClaimResponse;
import com.hrms.medical.entity.ClaimStatus;
import com.hrms.medical.entity.MedicalClaim;
import com.hrms.medical.repository.MedicalClaimRepository;
import com.hrms.user.entity.User;
import com.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class MedicalClaimService {

    private final MedicalClaimRepository medicalClaimRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    // ---------- Create / Attachment ----------

    @Transactional
    public MedicalClaimResponse createClaim(String username, ClaimRequest request, MultipartFile attachment) {
        Employee employee = resolveEmployee(username);

        MedicalClaim claim = new MedicalClaim();
        claim.setEmployee(employee);
        claim.setDescription(request.getDescription());
        claim.setAmount(request.getAmount());
        claim.setClaimDate(request.getClaimDate());
        claim.setStatus(ClaimStatus.SUBMITTED);

        if (attachment != null && !attachment.isEmpty()) {
            attach(claim, attachment);
        }

        MedicalClaim saved = medicalClaimRepository.save(claim);
        auditService.logSuccess("MEDICAL_CLAIM_CREATE", "MedicalClaim", saved.getId().toString(),
                employee.getEmployeeCode() + " submitted a claim for " + request.getAmount());

        return toResponse(saved);
    }

    @Transactional
    public MedicalClaimResponse uploadAttachment(String username, Long claimId, MultipartFile attachment) {
        MedicalClaim claim = getClaimOrThrow(claimId);
        Employee employee = resolveEmployee(username);

        if (!claim.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("You can only attach files to your own claims");
        }
        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new BadRequestException("Cannot modify the attachment after the claim has been decided");
        }

        attach(claim, attachment);
        MedicalClaim saved = medicalClaimRepository.save(claim);
        return toResponse(saved);
    }

    private void attach(MedicalClaim claim, MultipartFile attachment) {
        FileStorageService.StoredFile stored = fileStorageService.store(attachment);
        claim.setAttachmentPath(stored.storedPath());
        claim.setAttachmentOriginalName(stored.originalName());
        claim.setAttachmentContentType(stored.contentType());
    }

    // ---------- HR decision ----------

    @Transactional
    public MedicalClaimResponse hrApprove(String hrUsername, Long claimId, DecisionRequest decision) {
        MedicalClaim claim = getClaimOrThrow(claimId);
        ensureStatus(claim, ClaimStatus.SUBMITTED, "HR can only act on newly submitted claims");

        User hrUser = resolveUser(hrUsername);
        claim.setStatus(ClaimStatus.HR_APPROVED);
        claim.setHrRemarks(decision != null ? decision.getRemarks() : null);
        claim.setHrDecidedBy(hrUser);
        claim.setHrDecidedAt(LocalDateTime.now());

        MedicalClaim saved = medicalClaimRepository.save(claim);
        auditService.logSuccess("MEDICAL_CLAIM_HR_APPROVE", "MedicalClaim", saved.getId().toString(),
                "Forwarded to Finance by " + hrUsername);
        return toResponse(saved);
    }

    @Transactional
    public MedicalClaimResponse hrReject(String hrUsername, Long claimId, DecisionRequest decision) {
        MedicalClaim claim = getClaimOrThrow(claimId);
        ensureStatus(claim, ClaimStatus.SUBMITTED, "HR can only act on newly submitted claims");

        User hrUser = resolveUser(hrUsername);
        claim.setStatus(ClaimStatus.HR_REJECTED);
        claim.setHrRemarks(decision != null ? decision.getRemarks() : null);
        claim.setHrDecidedBy(hrUser);
        claim.setHrDecidedAt(LocalDateTime.now());

        MedicalClaim saved = medicalClaimRepository.save(claim);
        auditService.logSuccess("MEDICAL_CLAIM_HR_REJECT", "MedicalClaim", saved.getId().toString(),
                "Rejected by HR: " + hrUsername);
        return toResponse(saved);
    }

    // ---------- Finance decision ----------

    @Transactional
    public MedicalClaimResponse financeApprove(String financeUsername, Long claimId, DecisionRequest decision) {
        MedicalClaim claim = getClaimOrThrow(claimId);
        ensureStatus(claim, ClaimStatus.HR_APPROVED, "Finance can only act on claims already approved by HR");

        Employee employee = claim.getEmployee();
        int year = claim.getClaimDate().getYear();
        BigDecimal allocated = employee.getMedicalAllocation() == null ? BigDecimal.ZERO : employee.getMedicalAllocation();
        BigDecimal alreadyUsed = medicalClaimRepository.sumApprovedAmountForEmployeeAndYear(employee.getId(), year);
        BigDecimal remaining = allocated.subtract(alreadyUsed);

        if (remaining.compareTo(claim.getAmount()) < 0) {
            throw new BadRequestException(String.format(
                    "Claim amount %s exceeds remaining medical budget of %s for %d",
                    claim.getAmount(), remaining, year));
        }

        User financeUser = resolveUser(financeUsername);
        claim.setStatus(ClaimStatus.FINANCE_APPROVED);
        claim.setFinanceRemarks(decision != null ? decision.getRemarks() : null);
        claim.setFinanceDecidedBy(financeUser);
        claim.setFinanceDecidedAt(LocalDateTime.now());

        MedicalClaim saved = medicalClaimRepository.save(claim);
        auditService.logSuccess("MEDICAL_CLAIM_FINANCE_APPROVE", "MedicalClaim", saved.getId().toString(),
                "Approved by Finance: " + financeUsername + " (this status change is immediately visible to HR via the same claim record)");
        return toResponse(saved);
    }

    @Transactional
    public MedicalClaimResponse financeReject(String financeUsername, Long claimId, DecisionRequest decision) {
        MedicalClaim claim = getClaimOrThrow(claimId);
        ensureStatus(claim, ClaimStatus.HR_APPROVED, "Finance can only act on claims already approved by HR");

        User financeUser = resolveUser(financeUsername);
        claim.setStatus(ClaimStatus.FINANCE_REJECTED);
        claim.setFinanceRemarks(decision != null ? decision.getRemarks() : null);
        claim.setFinanceDecidedBy(financeUser);
        claim.setFinanceDecidedAt(LocalDateTime.now());

        MedicalClaim saved = medicalClaimRepository.save(claim);
        auditService.logSuccess("MEDICAL_CLAIM_FINANCE_REJECT", "MedicalClaim", saved.getId().toString(),
                "Rejected by Finance: " + financeUsername);
        return toResponse(saved);
    }

    // ---------- Queries ----------

    @Transactional(readOnly = true)
    public MedicalClaimResponse getClaim(Long claimId) {
        return toResponse(getClaimOrThrow(claimId));
    }

    @Transactional(readOnly = true)
    public Page<MedicalClaimResponse> getMyHistory(String username, Pageable pageable) {
        Employee employee = resolveEmployee(username);
        return medicalClaimRepository.findByEmployeeIdOrderByClaimDateDesc(employee.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MedicalClaimResponse> getEmployeeHistory(Long employeeId, Pageable pageable) {
        return medicalClaimRepository.findByEmployeeIdOrderByClaimDateDesc(employeeId, pageable)
                .map(this::toResponse);
    }

    /** HR dashboard queue: newly submitted claims awaiting HR review. */
    @Transactional(readOnly = true)
    public Page<MedicalClaimResponse> getPendingForHr(Pageable pageable) {
        return medicalClaimRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.SUBMITTED, pageable)
                .map(this::toResponse);
    }

    /** Finance dashboard queue: claims HR has already forwarded. */
    @Transactional(readOnly = true)
    public Page<MedicalClaimResponse> getPendingForFinance(Pageable pageable) {
        return medicalClaimRepository.findByStatusOrderByCreatedAtAsc(ClaimStatus.HR_APPROVED, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicalBudgetResponse getBudgetSummary(Long employeeId, Integer year) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        int targetYear = year != null ? year : Year.now().getValue();

        BigDecimal allocated = employee.getMedicalAllocation() == null ? BigDecimal.ZERO : employee.getMedicalAllocation();
        BigDecimal used = medicalClaimRepository.sumApprovedAmountForEmployeeAndYear(employee.getId(), targetYear);

        return MedicalBudgetResponse.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .year(targetYear)
                .allocated(allocated)
                .used(used)
                .remaining(allocated.subtract(used))
                .build();
    }

    @Transactional(readOnly = true)
    public MedicalBudgetResponse getMyBudgetSummary(String username, Integer year) {
        Employee employee = resolveEmployee(username);
        return getBudgetSummary(employee.getId(), year);
    }

    // ---------- Helpers ----------

    private Employee resolveEmployee(String username) {
        return employeeRepository.findByUser_UsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to user: " + username));
    }

    private User resolveUser(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private MedicalClaim getClaimOrThrow(Long id) {
        return medicalClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical claim not found with id: " + id));
    }

    private void ensureStatus(MedicalClaim claim, ClaimStatus expected, String message) {
        if (claim.getStatus() != expected) {
            throw new BadRequestException(message + " (current status: " + claim.getStatus() + ")");
        }
    }

    private MedicalClaimResponse toResponse(MedicalClaim claim) {
        Employee employee = claim.getEmployee();
        return MedicalClaimResponse.builder()
                .id(claim.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .description(claim.getDescription())
                .amount(claim.getAmount())
                .claimDate(claim.getClaimDate())
                .attachmentOriginalName(claim.getAttachmentOriginalName())
                .hasAttachment(claim.getAttachmentPath() != null)
                .status(claim.getStatus())
                .hrRemarks(claim.getHrRemarks())
                .hrDecidedByUsername(claim.getHrDecidedBy() != null ? claim.getHrDecidedBy().getUsername() : null)
                .hrDecidedAt(claim.getHrDecidedAt())
                .financeRemarks(claim.getFinanceRemarks())
                .financeDecidedByUsername(claim.getFinanceDecidedBy() != null ? claim.getFinanceDecidedBy().getUsername() : null)
                .financeDecidedAt(claim.getFinanceDecidedAt())
                .createdAt(claim.getCreatedAt())
                .build();
    }
}
