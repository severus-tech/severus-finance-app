-- V1.0.0__Initial_schema.sql
-- Severus Finance Initial Database Migration

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

CREATE TABLE app_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monthly_salary NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    salary_credit_day INT NOT NULL DEFAULT 1,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    display_name VARCHAR(100) NOT NULL DEFAULT 'Severus',
    theme_preference VARCHAR(20) NOT NULL DEFAULT 'DARK',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_settings_user UNIQUE(user_id)
);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    lender_or_bank VARCHAR(150) NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    masked_account_number VARCHAR(50),
    original_principal NUMERIC(19,2) NOT NULL,
    current_outstanding_principal NUMERIC(19,2) NOT NULL,
    annual_interest_rate NUMERIC(10,4) NOT NULL,
    start_date DATE NOT NULL,
    first_emi_date DATE NOT NULL,
    emi_due_day INT NOT NULL,
    emi_amount NUMERIC(19,2) NOT NULL,
    original_tenure_months INT NOT NULL,
    total_emi_count INT NOT NULL,
    calculation_mode VARCHAR(30) NOT NULL DEFAULT 'REDUCING_BALANCE',
    outstanding_principal_manual_override BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_loans_user_status ON loans(user_id, status);

CREATE TABLE loan_installments (
    id UUID PRIMARY KEY,
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    scheduled_amount NUMERIC(19,2) NOT NULL,
    principal_component NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    interest_component NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    payment_state VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_date DATE,
    actual_paid_amount NUMERIC(19,2),
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_loan_installment_number UNIQUE(loan_id, installment_number)
);

CREATE INDEX idx_installments_loan_due ON loan_installments(loan_id, due_date);
CREATE INDEX idx_installments_payment_state ON loan_installments(payment_state);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    expense_date DATE NOT NULL,
    monthly_due_day INT,
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    payment_state VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_date DATE,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expenses_user_date ON expenses(user_id, expense_date);
CREATE INDEX idx_expenses_user_type ON expenses(user_id, type);
CREATE INDEX idx_expenses_payment_state ON expenses(payment_state);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    person_name VARCHAR(150) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    original_amount NUMERIC(19,2) NOT NULL,
    remaining_balance NUMERIC(19,2) NOT NULL,
    entry_date DATE NOT NULL,
    due_date DATE,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_user_status ON ledger_entries(user_id, status);
CREATE INDEX idx_ledger_user_direction ON ledger_entries(user_id, direction);

CREATE TABLE ledger_payments (
    id UUID PRIMARY KEY,
    ledger_entry_id UUID NOT NULL REFERENCES ledger_entries(id) ON DELETE CASCADE,
    amount NUMERIC(19,2) NOT NULL,
    payment_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_payments_entry ON ledger_payments(ledger_entry_id);

CREATE TABLE todo_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_todo_user_status ON todo_items(user_id, status);
CREATE INDEX idx_todo_due_date ON todo_items(due_date);
