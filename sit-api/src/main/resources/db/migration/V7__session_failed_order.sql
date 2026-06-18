-- Đơn riêng cho Case 6 (IPN thất bại), tách khỏi đơn Case 5 (thành công)

ALTER TABLE test_session ADD COLUMN IF NOT EXISTS failed_txn_ref VARCHAR(100);
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS failed_amount_vnd BIGINT;
