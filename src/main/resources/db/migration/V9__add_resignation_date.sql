-- V9__add_resignation_date.sql
-- Needed for the dashboard's monthly resignation trend chart.

ALTER TABLE employees
    ADD COLUMN resignation_date DATE NULL AFTER confirmation_date;

CREATE INDEX idx_employees_resignation_date ON employees(resignation_date);
