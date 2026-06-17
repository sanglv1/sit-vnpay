import { useMemo, useState } from 'react';

const CASE_DESCRIPTIONS = {
  INVALID_HASH: 'Thay đổi / giả mạo vnp_SecureHash — Merchant trả RspCode 97',
  ORDER_NOT_FOUND: 'Thay đổi / giả mạo txnRef — Merchant không tìm thấy giao dịch, trả RspCode 01',
  WRONG_AMOUNT: 'Thay đổi / giả mạo số tiền — Merchant trả RspCode 04',
  SUCCESS: 'Giao dịch thành công — Merchant cập nhật trạng thái và trả RspCode 00',
  FAILED: 'IPN mang kết quả GD thất bại (ResponseCode ≠ 00) — Merchant ghi nhận FAIL và trả RspCode 00',
  ORDER_ALREADY_CONFIRMED: 'Gửi lại IPN khi đơn đã xác nhận — Merchant trả RspCode 02',
};

const INPUT_KEYS_PAY = [
  'vnp_TxnRef', 'vnp_TmnCode', 'vnp_TransactionNo', 'vnp_ResponseCode', 'vnp_TransactionStatus',
];
const INPUT_KEYS_SNAKE = [
  'vnp_txn_ref', 'vnp_tmn_code', 'vnp_transaction_no', 'vnp_response_code', 'vnp_transaction_status',
];

const caseTitle = (label) => label.replace(/\s*\(Case\s+\d+\)\s*$/i, '');

const evaluationLabel = (passed) => (passed ? 'ĐẠT' : 'KHÔNG ĐẠT');

const parseParams = (json) => {
  if (!json) return {};
  try {
    return JSON.parse(json);
  } catch {
    return {};
  }
};

const parseOutput = (body) => {
  if (!body) return { rspCode: '—', message: '—' };
  try {
    const obj = JSON.parse(body);
    return {
      rspCode: obj.RspCode ?? obj.rspCode ?? '—',
      message: obj.Message ?? obj.message ?? '—',
    };
  } catch {
    return { rspCode: '—', message: body };
  }
};

const inputKeysForRun = (run) => {
  const params = parseParams(run?.requestParams);
  const usesPascalCase = 'vnp_TxnRef' in params || run?.flow === 'PAY' || run?.flow === 'INSTALMENT';
  return usesPascalCase ? INPUT_KEYS_PAY : INPUT_KEYS_SNAKE;
};

const formatInputBlock = (run) => {
  const params = parseParams(run.requestParams);
  const keys = inputKeysForRun(run);
  return keys
    .map((key) => `${key}: ${params[key] ?? '—'}`)
    .join('\n');
};

const formatOutputBlock = (run) => {
  const { rspCode, message } = parseOutput(run.responseBody);
  return `rspCode: "${rspCode}"\nMessage: "${message}" | Đánh giá: ${evaluationLabel(run.passed)}`;
};

const TestCasePanel = ({
  testCases,
  runsByCase,
  autoPassed,
  autoTotal,
  onRunCase,
  runningCase,
}) => {
  const sortedCases = useMemo(
    () => [...testCases].sort((a, b) => (a.checkOrder || 0) - (b.checkOrder || 0)),
    [testCases],
  );

  const [selectedValue, setSelectedValue] = useState(null);

  const selectedCase = sortedCases.find((tc) => tc.value === selectedValue) || sortedCases[0];
  const selectedRun = selectedCase ? runsByCase[selectedCase.value] : null;

  return (
    <div className="tc-panel">
      <div className="tc-panel-header">
        Danh sách kịch bản (Test Cases)
        {' — '}
        <span className={autoPassed === autoTotal && autoTotal > 0 ? 'text-success' : ''}>
          {autoPassed}
          /
          {autoTotal}
          {' '}
          ĐẠT
        </span>
      </div>

      <div className="tc-panel-body">
        <div className="tc-list">
          <div className="tc-list-head">
            <span className="tc-list-head-main">Tình huống</span>
            <span className="tc-list-head-codes">RspCode</span>
            <span className="tc-list-head-eval">Đánh giá</span>
            <span className="tc-list-head-action" />
          </div>
          {sortedCases.map((tc) => {
            const run = runsByCase[tc.value];
            const isSelected = selectedCase?.value === tc.value;
            const isRunning = runningCase === tc.value;

            return (
              <div
                key={tc.value}
                className={`tc-item${isSelected ? ' tc-item-selected' : ''}${run ? (run.passed ? ' tc-item-pass' : ' tc-item-fail') : ''}`}
                onClick={() => setSelectedValue(tc.value)}
                onKeyDown={(e) => e.key === 'Enter' && setSelectedValue(tc.value)}
                role="button"
                tabIndex={0}
              >
                <div className="tc-item-main">
                  <div className="tc-item-head">
                    <span className="tc-item-id">
                      #
                      {tc.caseCode}
                    </span>
                    <span className="tc-item-title">{caseTitle(tc.label)}</span>
                  </div>
                  <p className="tc-item-desc">{CASE_DESCRIPTIONS[tc.value] || tc.label}</p>
                </div>

                <div className="tc-item-codes">
                  <span className="tc-code-box" title="RspCode kỳ vọng">{tc.expectedRspCode}</span>
                  <span
                    className={`tc-code-box${run ? (run.passed ? ' tc-code-ok' : ' tc-code-bad') : ''}`}
                    title="RspCode thực tế"
                  >
                    {run?.actualRspCode ?? '—'}
                  </span>
                </div>

                <div className="tc-item-eval">
                  {run ? (
                    <span className={`tc-eval-badge${run.passed ? ' dat' : ' khong-dat'}`}>
                      {evaluationLabel(run.passed)}
                    </span>
                  ) : (
                    <span className="tc-eval-badge pending">—</span>
                  )}
                </div>

                <button
                  type="button"
                  className="tc-play-btn"
                  title="Chạy case này"
                  disabled={isRunning}
                  onClick={(e) => {
                    e.stopPropagation();
                    onRunCase(tc.value);
                  }}
                >
                  <i className={isRunning ? 'ri-loader-4-line tc-spin' : 'ri-play-fill'} />
                </button>
              </div>
            );
          })}
        </div>

        {selectedCase && (
          <div className="tc-debug">
            <div className="tc-debug-header">
              <i className="ri-code-s-slash-line" />
              <div>
                <div className="tc-debug-title">
                  Debug Case #
                  {selectedCase.caseCode}
                </div>
                <div className="tc-debug-subtitle">{caseTitle(selectedCase.label)}</div>
              </div>
            </div>

            {selectedRun ? (
              <>
                <div className="tc-debug-section">
                  <div className="tc-debug-label">Kết quả test</div>
                </div>

                <div className="tc-debug-section">
                  <div className="tc-debug-label">Input:</div>
                  <pre className="code-block tc-debug-code">{formatInputBlock(selectedRun)}</pre>
                </div>

                <div className="tc-debug-section">
                  <div className="tc-debug-response-head">
                    <span className="tc-debug-label mb-0">Output:</span>
                    <span className={`tc-eval-badge tc-eval-badge-lg${selectedRun.passed ? ' dat' : ' khong-dat'}`}>
                      {evaluationLabel(selectedRun.passed)}
                    </span>
                  </div>
                  <pre className="code-block tc-debug-code">{formatOutputBlock(selectedRun)}</pre>
                </div>

                <div className="tc-debug-section">
                  <div className="tc-debug-label">Request gửi tới IPN URL</div>
                  <pre className="code-block tc-debug-code">{selectedRun.requestParams || '(empty)'}</pre>
                </div>

                {selectedRun.errorMessage && (
                  <div className="tc-debug-error">{selectedRun.errorMessage}</div>
                )}
              </>
            ) : (
              <div className="tc-debug-empty">
                <i className="ri-play-circle-line" />
                <p>Chưa có kết quả cho case này. Bấm nút play hoặc chạy kiểm tra tự động.</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default TestCasePanel;
