-- Lưu vnp_app_user_id theo phiên RECURRING (áp dụng cả phiên đã kiểm tra khi xuất biên bản)
ALTER TABLE test_session ADD COLUMN IF NOT EXISTS recurring_app_user_id VARCHAR(100);
