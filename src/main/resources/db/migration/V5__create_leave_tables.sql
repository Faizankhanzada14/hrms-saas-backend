-- V5__create_leave_tables.sql
-- Leave balances (per employee/type/year), leave requests, and leave encashments.

CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    year INT NOT NULL,
    allocated DECIMAL(6,1) NOT NULL DEFAULT 0,
    carried_forward DECIMAL(6,1) NOT NULL DEFAULT 0,
    used DECIMAL(6,1) NOT NULL DEFAULT 0,
    encashed DECIMAL(6,1) NOT NULL DEFAULT 0,
    CONSTRAINT uq_leave_balance_employee_type_year UNIQUE (employee_id, leave_type, year),
    CONSTRAINT fk_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_leave_balance_employee_year ON leave_balances(employee_id, year);

CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days DECIMAL(6,1) NOT NULL,
    reason VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by_user_id BIGINT,
    decision_remarks VARCHAR(1000),
    decided_at DATETIME,
    cancelled_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_leave_request_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_request_status ON leave_requests(status);
CREATE INDEX idx_leave_request_dates ON leave_requests(start_date, end_date);

CREATE TABLE leave_encashments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    year INT NOT NULL,
    days DECIMAL(6,1) NOT NULL,
    daily_rate DECIMAL(15,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by_user_id BIGINT,
    decision_remarks VARCHAR(1000),
    decided_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT fk_leave_encashment_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_encashment_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_leave_encashment_employee ON leave_encashments(employee_id);
CREATE INDEX idx_leave_encashment_status ON leave_encashments(status);
