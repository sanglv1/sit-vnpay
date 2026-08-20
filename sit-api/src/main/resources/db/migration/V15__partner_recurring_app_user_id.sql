-- Lưu vnp_app_user_id theo Terminal RECURRING — dùng chung cho phiên cũ và phiên mới
ALTER TABLE partner_config ADD COLUMN IF NOT EXISTS recurring_app_user_id VARCHAR(100);
