-- Safe backward-compatible migration to store authoritative scheduled loan end date
ALTER TABLE loans ADD COLUMN IF NOT EXISTS end_date DATE;

