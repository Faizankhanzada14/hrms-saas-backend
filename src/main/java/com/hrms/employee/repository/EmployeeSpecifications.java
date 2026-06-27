package com.hrms.employee.repository;

import com.hrms.employee.dto.EmployeeFilter;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> withFilter(EmployeeFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String like = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate nameMatch = cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("employeeCode")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("cnic")), like)
                );
                predicates.add(nameMatch);
            }

            if (filter.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), filter.getDepartmentId()));
            }

            if (filter.getDesignationId() != null) {
                predicates.add(cb.equal(root.get("designation").get("id"), filter.getDesignationId()));
            }

            if (filter.getPostingUnitId() != null) {
                predicates.add(cb.equal(root.get("postingUnit").get("id"), filter.getPostingUnitId()));
            }

            if (filter.getEmploymentStatus() != null) {
                predicates.add(cb.equal(root.get("employmentStatus"), filter.getEmploymentStatus()));
            }

            if (filter.getGender() != null) {
                predicates.add(cb.equal(root.get("gender"), filter.getGender()));
            }

            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
