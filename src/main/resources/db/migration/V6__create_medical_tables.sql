-- V6__create_medical_tables.sql
-- Medical claims with the Employee -> HR -> Finance approval workflow.

CREATE TABLE medical_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    claim_date DATE NOT NULL,
    attachment_path VARCHAR(500),
    attachment_original_name VARCHAR(255),
    attachment_content_type VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    hr_remarks VARCHAR(1000),
    hr_decided_by_user_id BIGINT,
    hr_decided_at DATETIME,
    finance_remarks VARCHAR(1000),
    finance_decided_by_user_id BIGINT,
    finance_decided_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT fk_medical_claim_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_claim_hr_decided_by FOREIGN KEY (hr_decided_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_medical_claim_finance_decided_by FOREIGN KEY (finance_decided_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_medical_claim_employee ON medical_claims(employee_id);
CREATE INDEX idx_medical_claim_status ON medical_claims(status);
CREATE INDEX idx_medical_claim_date ON medical_claims(claim_date);
