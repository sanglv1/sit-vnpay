-- Backfill created_by_email cho phiên cũ đang NULL/rỗng (MERCHANT_QC bị chặn export do requireSessionAccess).
UPDATE test_session ts
SET created_by_email = (
    SELECT pc.created_by_email
    FROM partner_config pc
    WHERE pc.id = ts.partner_id
      AND pc.created_by_email IS NOT NULL
      AND TRIM(pc.created_by_email) <> ''
)
WHERE (ts.created_by_email IS NULL OR TRIM(ts.created_by_email) = '')
  AND EXISTS (
    SELECT 1
    FROM partner_config pc
    WHERE pc.id = ts.partner_id
      AND pc.created_by_email IS NOT NULL
      AND TRIM(pc.created_by_email) <> ''
  );
