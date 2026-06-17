-- Terminal do Merchant QC tự tạo; ADMIN xem tất cả

ALTER TABLE partner_config ADD COLUMN IF NOT EXISTS created_by_email VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_partner_config_created_by ON partner_config (created_by_email);
