package com.hrms.employee.service;

import com.hrms.audit.AuditService;
import com.hrms.employee.dto.MasterDataDtos.*;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import com.hrms.employee.entity.PostingUnit;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.PostingUnitRepository;
import com.hrms.exception.DuplicateResourceException;
import com.hrms.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final PostingUnitRepository postingUnitRepository;
    private final AuditService auditService;

    // ---------- Department ----------

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.findAll().stream()
                .anyMatch(d -> !d.isDeleted() && d.getName().equalsIgnoreCase(request.getName()))) {
            throw new DuplicateResourceException("A department named '" + request.getName() + "' already exists");
        }
        Department department = new Department();
        department.setName(request.getName());
        Department saved = departmentRepository.save(department);
        auditService.logSuccess("DEPARTMENT_CREATE", "Department", saved.getId().toString(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setName(request.getName());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setDeleted(true);
        departmentRepository.save(department);
        auditService.logSuccess("DEPARTMENT_DELETE", "Department", id.toString(), department.getName());
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .map(this::toResponse)
                .toList();
    }

    // ---------- Designation ----------

    @Transactional
    public DesignationResponse createDesignation(DesignationRequest request) {
        if (designationRepository.findAll().stream()
                .anyMatch(d -> !d.isDeleted() && d.getTitle().equalsIgnoreCase(request.getTitle()))) {
            throw new DuplicateResourceException("A designation titled '" + request.getTitle() + "' already exists");
        }
        Designation designation = new Designation();
        designation.setTitle(request.getTitle());
        Designation saved = designationRepository.save(designation);
        auditService.logSuccess("DESIGNATION_CREATE", "Designation", saved.getId().toString(), saved.getTitle());
        return toResponse(saved);
    }

    @Transactional
    public DesignationResponse updateDesignation(Long id, DesignationRequest request) {
        Designation designation = designationRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
        designation.setTitle(request.getTitle());
        return toResponse(designationRepository.save(designation));
    }

    @Transactional
    public void deleteDesignation(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
        designation.setDeleted(true);
        designationRepository.save(designation);
        auditService.logSuccess("DESIGNATION_DELETE", "Designation", id.toString(), designation.getTitle());
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations() {
        return designationRepository.findAll().stream()
                .filter(d -> !d.isDeleted())
                .map(this::toResponse)
                .toList();
    }

    // ---------- Posting Unit (Factory / Branch / Head Office / Warehouse) ----------

    @Transactional
    public PostingUnitResponse createPostingUnit(PostingUnitRequest request) {
        PostingUnit unit = new PostingUnit();
        unit.setName(request.getName());
        unit.setUnitType(request.getUnitType());
        unit.setBranchName(request.getBranchName());
        PostingUnit saved = postingUnitRepository.save(unit);
        auditService.logSuccess("POSTING_UNIT_CREATE", "PostingUnit", saved.getId().toString(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public PostingUnitResponse updatePostingUnit(Long id, PostingUnitRequest request) {
        PostingUnit unit = postingUnitRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Posting unit not found with id: " + id));
        unit.setName(request.getName());
        unit.setUnitType(request.getUnitType());
        unit.setBranchName(request.getBranchName());
        return toResponse(postingUnitRepository.save(unit));
    }

    @Transactional
    public void deletePostingUnit(Long id) {
        PostingUnit unit = postingUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Posting unit not found with id: " + id));
        unit.setDeleted(true);
        postingUnitRepository.save(unit);
        auditService.logSuccess("POSTING_UNIT_DELETE", "PostingUnit", id.toString(), unit.getName());
    }

    @Transactional(readOnly = true)
    public List<PostingUnitResponse> getAllPostingUnits() {
        return postingUnitRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .map(this::toResponse)
                .toList();
    }

    // ---------- Mapping ----------

    private DepartmentResponse toResponse(Department department) {
        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        return response;
    }

    private DesignationResponse toResponse(Designation designation) {
        DesignationResponse response = new DesignationResponse();
        response.setId(designation.getId());
        response.setTitle(designation.getTitle());
        return response;
    }

    private PostingUnitResponse toResponse(PostingUnit unit) {
        PostingUnitResponse response = new PostingUnitResponse();
        response.setId(unit.getId());
        response.setName(unit.getName());
        response.setUnitType(unit.getUnitType());
        response.setBranchName(unit.getBranchName());
        return response;
    }
}
