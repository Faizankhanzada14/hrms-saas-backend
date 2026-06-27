package com.hrms.posting;

import com.hrms.audit.AuditService;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.entity.PostingUnit;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.PostingUnitRepository;
import com.hrms.exception.BadRequestException;
import com.hrms.posting.dto.PostingDtos.PostingHistoryResponse;
import com.hrms.posting.dto.PostingDtos.TransferRequest;
import com.hrms.posting.entity.PostingHistory;
import com.hrms.posting.repository.PostingHistoryRepository;
import com.hrms.posting.service.PostingService;
import com.hrms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PostingUnitRepository postingUnitRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PostingHistoryRepository postingHistoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private PostingService postingService;

    private Employee employee;
    private PostingUnit oldUnit;
    private PostingUnit newUnit;

    @BeforeEach
    void setUp() {
        oldUnit = new PostingUnit();
        oldUnit.setId(1L);
        oldUnit.setName("Karachi Factory");

        newUnit = new PostingUnit();
        newUnit.setId(2L);
        newUnit.setName("Lahore Branch");

        employee = new Employee();
        employee.setId(10L);
        employee.setEmployeeCode("EMP-2026-00010");
        employee.setFirstName("Ali");
        employee.setLastName("Khan");
        employee.setPostingUnit(oldUnit);
    }

    @Test
    void transferEmployee_toNewPostingUnit_updatesEmployeeAndRecordsHistory() {
        TransferRequest request = new TransferRequest();
        request.setNewPostingUnitId(2L);
        request.setTransferDate(LocalDate.now());
        request.setReason("Business need");

        when(employeeRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(employee));
        when(postingUnitRepository.findById(2L)).thenReturn(Optional.of(newUnit));
        when(userRepository.findByUsernameAndDeletedFalse("hradmin")).thenReturn(Optional.empty());
        when(postingHistoryRepository.save(any(PostingHistory.class))).thenAnswer(inv -> {
            PostingHistory h = inv.getArgument(0);
            h.setId(99L);
            return h;
        });

        PostingHistoryResponse response = postingService.transferEmployee("hradmin", 10L, request);

        assertThat(employee.getPostingUnit().getId()).isEqualTo(2L);
        assertThat(response.getFromPostingUnitId()).isEqualTo(1L);
        assertThat(response.getToPostingUnitId()).isEqualTo(2L);
    }

    @Test
    void transferEmployee_withNoTargetSpecified_throwsBadRequest() {
        TransferRequest request = new TransferRequest();
        request.setTransferDate(LocalDate.now());

        when(employeeRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> postingService.transferEmployee("hradmin", 10L, request))
                .isInstanceOf(BadRequestException.class);
    }
}
