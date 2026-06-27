-- V3__create_employees.sql
-- Departments, designations, posting units, and the core employees table.

CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uq_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE designations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uq_designations_title UNIQUE (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE posting_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    unit_type VARCHAR(30) NOT NULL,
    branch_name VARCHAR(100),
    created_at DATETIME NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cnic VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    marital_status VARCHAR(20),
    blood_group VARCHAR(10),
    mobile_number VARCHAR(20),
    email VARCHAR(150),
    emergency_contact VARCHAR(20),
    address VARCHAR(500),
    joining_date DATE,
    confirmation_date DATE,
    department_id BIGINT,
    designation_id BIGINT,
    posting_unit_id BIGINT,
    manager_id BIGINT,
    salary DECIMAL(15,2) DEFAULT 0,
    bonus DECIMAL(15,2) DEFAULT 0,
    medical_allocation DECIMAL(15,2) DEFAULT 0,
    profile_picture_url VARCHAR(500),
    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    active TINYINT(1) NOT NULL DEFAULT 1,
    user_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT uq_employees_cnic UNIQUE (cnic),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_employees_designation FOREIGN KEY (designation_id) REFERENCES designations(id),
    CONSTRAINT fk_employees_posting_unit FOREIGN KEY (posting_unit_id) REFERENCES posting_units(id),
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees(id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_status ON employees(employment_status);
CREATE INDEX idx_employees_active ON employees(active);
CREATE INDEX idx_employees_name ON employees(first_name, last_name);
