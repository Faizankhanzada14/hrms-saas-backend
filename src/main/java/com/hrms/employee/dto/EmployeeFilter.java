package com.hrms.employee.dto;

import com.hrms.employee.entity.Employee;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeFilter {
    private String search;
    private Long departmentId;
    private Long designationId;
    private Long postingUnitId;
    private Employee.EmploymentStatus employmentStatus;
    private Employee.Gender gender;
    private Boolean active;
}
