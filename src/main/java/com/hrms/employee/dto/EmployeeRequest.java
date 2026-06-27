package com.hrms.employee.dto;

import com.hrms.employee.entity.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String cnic;

    private LocalDate dateOfBirth;

    private Employee.Gender gender;

    private Employee.MaritalStatus maritalStatus;

    private String bloodGroup;

    private String mobileNumber;

    @Email(message = "Email must be a valid email address")
    private String email;

    private String emergencyContact;

    private String address;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate confirmationDate;

    private LocalDate resignationDate;

    private Long departmentId;

    private Long designationId;

    private Long postingUnitId;

    private Long managerId;

    @PositiveOrZero(message = "Salary cannot be negative")
    private BigDecimal salary;

    @PositiveOrZero(message = "Bonus cannot be negative")
    private BigDecimal bonus;

    @PositiveOrZero(message = "Medical allocation cannot be negative")
    private BigDecimal medicalAllocation;

    private String profilePictureUrl;

    private Employee.EmploymentStatus employmentStatus;
}
