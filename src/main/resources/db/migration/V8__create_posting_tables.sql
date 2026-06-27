-- V8__create_posting_tables.sql
-- Posting/transfer history. Departments, designations, and posting_units already exist (V3).

CREATE TABLE posting_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    from_posting_unit_id BIGINT,
    to_posting_unit_id BIGINT,
    from_department_id BIGINT,
    to_department_id BIGINT,
    transfer_date DATE NOT NULL,
    reason VARCHAR(1000),
    transferred_by_user_id BIGINT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_posting_history_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_posting_history_from_unit FOREIGN KEY (from_posting_unit_id) REFERENCES posting_units(id),
    CONSTRAINT fk_posting_history_to_unit FOREIGN KEY (to_posting_unit_id) REFERENCES posting_units(id),
    CONSTRAINT fk_posting_history_from_dept FOREIGN KEY (from_department_id) REFERENCES departments(id),
    CONSTRAINT fk_posting_history_to_dept FOREIGN KEY (to_department_id) REFERENCES departments(id),
    CONSTRAINT fk_posting_history_transferred_by FOREIGN KEY (transferred_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_posting_history_employee ON posting_history(employee_id);
CREATE INDEX idx_posting_history_transfer_date ON posting_history(transfer_date);

-- Seed a baseline permission set across modules so roles have something to attach immediately.
INSERT INTO permissions (name, description, module) VALUES
    ('EMPLOYEE_READ', 'View employee records', 'EMPLOYEE'),
    ('EMPLOYEE_WRITE', 'Create/update/delete employee records', 'EMPLOYEE'),
    ('LEAVE_APPROVE', 'Approve or reject leave requests', 'LEAVE'),
    ('MEDICAL_CLAIM_HR_DECIDE', 'HR approve/reject medical claims', 'MEDICAL'),
    ('MEDICAL_CLAIM_FINANCE_DECIDE', 'Finance approve/reject medical claims', 'MEDICAL'),
    ('PF_WITHDRAWAL_DECIDE', 'Approve or reject PF withdrawals', 'PF'),
    ('ROLE_MANAGE', 'Create/update/delete roles and assign permissions', 'ROLE'),
    ('USER_MANAGE', 'Create users and assign roles', 'USER');
