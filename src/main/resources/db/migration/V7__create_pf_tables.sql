-- V7__create_pf_tables.sql
-- Provident Fund accounts, the immutable ledger, and withdrawal requests.

CREATE TABLE pf_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    employee_contribution_rate DECIMAL(5,2) NOT NULL,
    employer_contribution_rate DECIMAL(5,2) NOT NULL,
    opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_employee_contribution DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_employer_contribution DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_withdrawn DECIMAL(15,2) NOT NULL DEFAULT 0,
    opened_date DATE NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    last_contribution_month VARCHAR(7),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_pf_account_employee UNIQUE (employee_id),
    CONSTRAINT fk_pf_account_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pf_account_id BIGINT NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    reference_month VARCHAR(7),
    description VARCHAR(500),
    transaction_date DATETIME NOT NULL,
    CONSTRAINT fk_pf_ledger_account FOREIGN KEY (pf_account_id) REFERENCES pf_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pf_ledger_account ON pf_ledger_entries(pf_account_id);
CREATE INDEX idx_pf_ledger_transaction_date ON pf_ledger_entries(transaction_date);
CREATE INDEX idx_pf_ledger_reference_month ON pf_ledger_entries(reference_month);

CREATE TABLE pf_withdrawals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reason VARCHAR(1000),
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
    CONSTRAINT fk_pf_withdrawal_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_withdrawal_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pf_withdrawal_employee ON pf_withdrawals(employee_id);
CREATE INDEX idx_pf_withdrawal_status ON pf_withdrawals(status);
