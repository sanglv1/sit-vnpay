-- Backfill created_by_email cho Terminal cũ đang NULL/rỗng.
-- Ưu tiên lấy ADMIN đầu tiên trong hệ thống; nếu chưa có thì fallback email mặc định.

UPDATE partner_config
SET created_by_email = COALESCE(
    (SELECT su.email
     FROM sit_user su
     WHERE su.role = 'ADMIN'
     ORDER BY su.id
     LIMIT 1),
    'admin@vnpay.vn'
)
WHERE created_by_email IS NULL
   OR TRIM(created_by_email) = '';
