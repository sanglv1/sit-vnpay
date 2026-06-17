-- Indexes for session list sort, latest-run lookups, and txnRef search

CREATE INDEX IF NOT EXISTS idx_test_session_created_at ON test_session (created_at);

CREATE INDEX IF NOT EXISTS idx_test_run_session_case_created
    ON test_run (session_id, test_case, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_test_run_txn_ref ON test_run (txn_ref);
