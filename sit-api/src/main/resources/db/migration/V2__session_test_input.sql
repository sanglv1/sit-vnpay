-- Lưu txnRef / số tiền nhập trên form nghiệm thu IPN theo phiên

ALTER TABLE test_session ADD COLUMN IF NOT EXISTS pending_txn_ref VARCHAR(100);
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS pending_amount_vnd BIGINT;
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS confirmed_txn_ref VARCHAR(100);
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS confirmed_amount_vnd BIGINT;
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS wrong_amount_vnd BIGINT;
