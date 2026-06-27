package com.hrms.employee.dto;

import com.hrms.employee.entity.Employee;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String cnic;
    private LocalDate dateOfBirth;
    private Employee.Gender gender;
    private Employee.MaritalStatus maritalStatus;
    private String bloodGroup;
    private String mobileNumber;
    private String email;
    private String emergencyContact;
    private String address;
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate resignationDate;

    private Long departmentId;
    private String departmentName;

    private Long designationId;
    private String designationTitle;

    private Long postingUnitId;
    private String postingUnitName;

    private Long managerId;
    private String managerName;

    private BigDecimal salary;
    private BigDecimal bonus;
    private BigDecimal medicalAllocation;
    private String profilePictureUrl;
    private Employee.EmploymentStatus employmentStatus;
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
