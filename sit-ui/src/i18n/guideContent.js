export const guideContent = {
  vi: {
    title: 'Hướng dẫn thực hiện',
    subtitle: 'Quy trình kiểm thử SIT cho merchant tích hợp VNPay',
    intro:
      'Quy trình gồm 6 bước. Cả 4 luồng (PAY, TOKEN, RECURRING, INSTALMENT) đều đi theo cùng quy trình; khác nhau ở cấu hình Terminal và bộ tham số callback mà hệ thống tự sinh.',
    flowDiagram: `Chuẩn bị merchant → Quản lý Terminal → Tạo phiên → Nghiệm thu IPN (tự động)
                                                          ↓
                              Xem kết quả ← Lưu QC thủ công ← Nghiệm thu thủ công`,
    steps: [
      {
        id: 1,
        title: 'Chuẩn bị phía merchant',
        items: [
          'Tích hợp xong luồng thanh toán tương ứng.',
          'Cung cấp cho QC: TMN Code, Secret Key, Return URL, IPN URL.',
          'Đảm bảo server IPN xử lý đúng thứ tự kiểm tra và trả RspCode theo chuẩn VNPay.',
        ],
      },
      {
        id: 2,
        title: 'Quản lý Terminal',
        path: '/partners/create',
        pathLabel: 'Thêm Terminal',
        partnerFields: [
          ['Tên Terminal', 'Tên merchant / dự án'],
          ['Luồng', 'PAY, TOKEN, RECURRING hoặc INSTALMENT'],
          ['TMN Code', 'Mã terminal merchant'],
          ['Secret Key', 'Khóa bí mật ký HMAC'],
          ['Return URL', 'URL redirect sau thanh toán'],
          ['IPN URL', 'URL API nhận callback server-to-server'],
        ],
        flowDiff: [
          ['PAY', 'PascalCase: `vnp_TmnCode`, `vnp_TxnRef`, `vnp_Amount`, `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_SecureHash` — ký UTF-8'],
          ['TOKEN', 'snake_case: `vnp_tmn_code`, `vnp_txn_ref`, `vnp_amount`, `vnp_response_code`, `vnp_transaction_status`, `vnp_secure_hash`; thêm `vnp_token`, `vnp_card_number` khi GD thành công'],
          ['RECURRING', 'snake_case (tương tự TOKEN); thêm `vnp_recurring_id` khi GD thành công'],
          ['INSTALMENT', 'snake_case (tương tự TOKEN); thêm `vnp_installment_term` (mặc định `3`)'],
        ],
      },
      {
        id: 3,
        title: 'Tạo phiên kiểm thử',
        path: '/sessions/new',
        pathLabel: 'Tạo phiên',
        items: [
          'Vào Phiên kiểm thử → Tạo phiên.',
          'Chọn Terminal (đã gắn luồng).',
          'Nhập ghi chú (tùy chọn) → Tạo phiên.',
          'Hệ thống chuyển sang tab Nghiệm thu tự động. Mỗi phiên theo dõi tiến độ X/5 Passed.',
        ],
      },
      {
        id: 4,
        title: 'Nghiệm thu tự động: IPN',
        note:
          'Tool mô phỏng VNPay gửi HTTP GET tới IPN URL của merchant, kèm header X-Forwarded-For: 113.160.92.202 (IP sandbox VNPay).',
        params: [
          ['Mã giao dịch', '`vnp_TxnRef` (PAY) / `vnp_txn_ref` (TOKEN, RECURRING, INSTALMENT) — nhập trên form SIT'],
          ['Số tiền (VND)', '`vnp_Amount` (PAY) / `vnp_amount` (các luồng khác) — giá trị gửi đi = số VND × 100'],
          ['Số tiền sai (VND)', 'Dùng cho Case 3 — ghi đè `vnp_Amount` / `vnp_amount` (mặc định = số tiền đúng + 1.000 VND)'],
        ],
        runModes: [
          'Gửi 1 callback IPN — chọn test case riêng lẻ rồi gửi.',
          'Tiến hành kiểm tra tự động — chạy lần lượt toàn bộ 5 case.',
        ],
        ipnCases: [
          ['1', 'Case 1', 'Chữ ký không hợp lệ (`vnp_SecureHash` / `vnp_secure_hash`)', '97'],
          ['2', 'Case 2', '`vnp_TxnRef` / `vnp_txn_ref` không tồn tại (tool tự sinh mã giả)', '01'],
          ['3', 'Case 3', '`vnp_Amount` / `vnp_amount` không khớp', '04'],
          ['4', 'Case 5', 'Giao dịch thành công', '00'],
          ['5', 'Case 4', 'Gửi lại khi đơn đã xác nhận', '02'],
        ],
        passCriteria: 'HTTP 2xx, không lỗi kết nối, `RspCode` trong response body khớp giá trị mong đợi.',
        ipnLogic: `1. Kiểm tra chữ ký
   PAY: vnp_SecureHash | Khác: vnp_secure_hash → sai: RspCode 97
2. Kiểm tra mã giao dịch
   PAY: vnp_TxnRef | Khác: vnp_txn_ref → không có: RspCode 01
3. Kiểm tra số tiền
   PAY: vnp_Amount | Khác: vnp_amount → sai: RspCode 04
4. Kiểm tra trạng thái đơn → đã xử lý: RspCode 02
5. Kiểm tra kết quả giao dịch
   PAY: vnp_ResponseCode=00 & vnp_TransactionStatus=00 → SUCCESS → RspCode 00
   Khác: vnp_response_code=00 & vnp_transaction_status=00 → SUCCESS → RspCode 00
   Ngược lại → FAIL → RspCode 00 (đã nhận IPN)`,
      },
      {
        id: 5,
        title: 'Nghiệm thu thủ công',
        note: 'Các hạng mục sau không tự động — QC xác nhận bằng checklist và bằng chứng.',
        manualItems: [
          ['Return URL — GD thành công', 'Merchant hiển thị đúng trang kết quả', 'Thực hiện GD thật trên cổng VNPay → nhập `vnp_TxnRef` / `vnp_txn_ref` + upload ảnh chụp màn hình'],
          ['Return URL — GD thất bại', 'Merchant hiển thị đúng trang thất bại', 'Tương tự, dùng `vnp_TxnRef` / `vnp_txn_ref` của GD thất bại'],
          ['Case ex', 'Xử lý exception ở đầu IPN URL', 'Xác nhận merchant trả RspCode 99 khi lỗi không xử lý được'],
          ['Whitelist IP', 'Merchant đã whitelist IP VNPay', 'Xác nhận IPN chỉ chấp nhận request từ dải IP VNPay'],
          ['Lưu log', 'Merchant lưu log request/response', 'Xác nhận có log đầy đủ hai chiều'],
        ],
        saveNote: 'Bấm Lưu kết quả QC để lưu theo phiên.',
      },
      {
        id: 6,
        title: 'Xem kết quả',
        resultScreens: [
          ['/sessions/{id}/suite-result', 'Kết quả sau khi chạy full IPN suite'],
          ['/tests/{id}', 'Chi tiết 1 lần chạy (request, response, pass/fail)'],
          ['/tests/history', 'Lịch sử toàn bộ lần chạy'],
          ['/sessions', 'Tiến độ X/5 Passed theo phiên'],
        ],
      },
    ],
    checklistTitle: 'Checklist nhanh',
    checklist: [
      ['Tạo Terminal đúng luồng', 'Thủ công'],
      ['Tạo phiên kiểm thử', 'Thủ công'],
      ['Case 1–5 IPN (97, 01, 04, 00, 02)', 'Tự động'],
      ['Return URL thành công / thất bại', 'Thủ công'],
      ['Case ex (RspCode 99)', 'Thủ công'],
      ['Whitelist IP', 'Thủ công'],
      ['Lưu log', 'Thủ công'],
    ],
    labels: {
      step: 'Bước',
      field: 'Trường',
      description: 'Mô tả',
      flow: 'Luồng',
      feature: 'Đặc điểm callback',
      param: 'Tham số',
      case: 'Case',
      scenario: 'Kịch bản',
      rspCode: 'RspCode mong đợi',
      item: 'Hạng mục',
      content: 'Nội dung',
      howTo: 'Cách thực hiện',
      screen: 'Màn hình',
      task: 'Việc cần làm',
      mode: 'Tự động / Thủ công',
      flowDiffTitle: 'Khác biệt theo luồng',
      partnerFieldsTitle: 'Thông tin cần khai báo',
      paramsTitle: 'Chuẩn bị dữ liệu',
      runModesTitle: 'Hai cách chạy',
      ipnCasesTitle: 'Bảng case IPN tự động',
      passCriteriaTitle: 'Tiêu chí PASS',
      ipnLogicTitle: 'Logic IPN merchant cần đáp ứng',
      goTo: 'Đi tới',
    },
  },
  en: {
    title: 'Implementation guide',
    subtitle: 'SIT testing workflow for VNPay integration partners',
    intro:
      'The workflow has 6 steps. All four flows (PAY, TOKEN, RECURRING, INSTALMENT) follow the same process; they differ in partner configuration and callback parameters generated by the system.',
    flowDiagram: `Merchant prep → Terminal management → Create session → Auto IPN acceptance
                                                          ↓
                              View results ← Save manual QC ← Manual acceptance`,
    steps: [
      {
        id: 1,
        title: 'Merchant preparation',
        items: [
          'Complete integration for the relevant payment flow.',
          'Provide QC with: TMN Code, Secret Key, Return URL, IPN URL.',
          'Ensure the IPN server validates in the correct order and returns RspCode per VNPay spec.',
        ],
      },
      {
        id: 2,
        title: 'Terminal management',
        path: '/partners/create',
        pathLabel: 'Add terminal',
        partnerFields: [
          ['Terminal name', 'Merchant / project name'],
          ['Flow', 'PAY, TOKEN, RECURRING or INSTALMENT'],
          ['TMN Code', 'Merchant terminal code'],
          ['Secret Key', 'HMAC signing secret'],
          ['Return URL', 'Redirect URL after payment'],
          ['IPN URL', 'Server-to-server callback API URL'],
        ],
        flowDiff: [
          ['PAY', 'PascalCase: `vnp_TmnCode`, `vnp_TxnRef`, `vnp_Amount`, `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_SecureHash` — UTF-8 signing'],
          ['TOKEN', 'snake_case: `vnp_tmn_code`, `vnp_txn_ref`, `vnp_amount`, `vnp_response_code`, `vnp_transaction_status`, `vnp_secure_hash`; adds `vnp_token`, `vnp_card_number` on success'],
          ['RECURRING', 'snake_case (same as TOKEN); adds `vnp_recurring_id` on success'],
          ['INSTALMENT', 'snake_case (same as TOKEN); adds `vnp_installment_term` (default `3`)'],
        ],
      },
      {
        id: 3,
        title: 'Create test session',
        path: '/sessions/new',
        pathLabel: 'New session',
        items: [
          'Go to Test sessions → New session.',
          'Select a terminal (with flow configured).',
          'Optional note → Create session.',
          'You are redirected to Auto acceptance. Progress is tracked as X/5 Passed.',
        ],
      },
      {
        id: 4,
        title: 'Auto acceptance: IPN',
        note:
          'The tool simulates VNPay sending HTTP GET to the merchant IPN URL with X-Forwarded-For: 113.160.92.202 (VNPay sandbox IP).',
        params: [
          ['Transaction ID', '`vnp_TxnRef` (PAY) / `vnp_txn_ref` (TOKEN, RECURRING, INSTALMENT) — entered on SIT form'],
          ['Amount (VND)', '`vnp_Amount` (PAY) / `vnp_amount` (other flows) — sent value = VND × 100'],
          ['Wrong amount (VND)', 'For Case 3 — overrides `vnp_Amount` / `vnp_amount` (default = correct amount + 1,000 VND)'],
        ],
        runModes: [
          'Send one IPN callback — pick a single test case and send.',
          'Run full auto suite — executes all 5 cases in sequence.',
        ],
        ipnCases: [
          ['1', 'Case 1', 'Invalid signature (`vnp_SecureHash` / `vnp_secure_hash`)', '97'],
          ['2', 'Case 2', '`vnp_TxnRef` / `vnp_txn_ref` not found (tool generates fake ID)', '01'],
          ['3', 'Case 3', '`vnp_Amount` / `vnp_amount` mismatch', '04'],
          ['4', 'Case 5', 'Successful transaction', '00'],
          ['5', 'Case 4', 'Resend when order already confirmed', '02'],
        ],
        passCriteria: 'HTTP 2xx, no connection error, `RspCode` in response body matches expected value.',
        ipnLogic: `1. Verify signature
   PAY: vnp_SecureHash | Other: vnp_secure_hash → invalid: RspCode 97
2. Verify transaction ID
   PAY: vnp_TxnRef | Other: vnp_txn_ref → not found: RspCode 01
3. Verify amount
   PAY: vnp_Amount | Other: vnp_amount → mismatch: RspCode 04
4. Verify order status → already processed: RspCode 02
5. Verify transaction result
   PAY: vnp_ResponseCode=00 & vnp_TransactionStatus=00 → SUCCESS → RspCode 00
   Other: vnp_response_code=00 & vnp_transaction_status=00 → SUCCESS → RspCode 00
   Otherwise → FAIL → RspCode 00 (IPN received)`,
      },
      {
        id: 5,
        title: 'Manual acceptance',
        note: 'The following items are not automated — QC confirms via checklist and evidence.',
        manualItems: [
          ['Return URL — success', 'Merchant shows correct success page', 'Complete a real payment on VNPay → enter `vnp_TxnRef` / `vnp_txn_ref` + upload screenshot'],
          ['Return URL — failure', 'Merchant shows correct failure page', 'Same process with failed transaction `vnp_TxnRef` / `vnp_txn_ref`'],
          ['Case ex', 'Exception handling at IPN URL', 'Confirm merchant returns RspCode 99 for unhandled errors'],
          ['Whitelist IP', 'Merchant whitelisted VNPay IPs', 'Confirm IPN only accepts requests from VNPay IP range'],
          ['Log storage', 'Merchant stores request/response logs', 'Confirm full bidirectional logs exist'],
        ],
        saveNote: 'Click Save QC result to store per session.',
      },
      {
        id: 6,
        title: 'View results',
        resultScreens: [
          ['/sessions/{id}/suite-result', 'Results after full IPN suite run'],
          ['/tests/{id}', 'Single run detail (request, response, pass/fail)'],
          ['/tests/history', 'Full run history'],
          ['/sessions', 'X/5 Passed progress per session'],
        ],
      },
    ],
    checklistTitle: 'Quick checklist',
    checklist: [
      ['Create terminal with correct flow', 'Manual'],
      ['Create test session', 'Manual'],
      ['IPN Cases 1–5 (97, 01, 04, 00, 02)', 'Auto'],
      ['Return URL success / failure', 'Manual'],
      ['Case ex (RspCode 99)', 'Manual'],
      ['Whitelist IP', 'Manual'],
      ['Log storage', 'Manual'],
    ],
    labels: {
      step: 'Step',
      field: 'Field',
      description: 'Description',
      flow: 'Flow',
      feature: 'Callback characteristics',
      param: 'Parameter',
      case: 'Case',
      scenario: 'Scenario',
      rspCode: 'Expected RspCode',
      item: 'Item',
      content: 'Content',
      howTo: 'How to',
      screen: 'Screen',
      task: 'Task',
      mode: 'Auto / Manual',
      flowDiffTitle: 'Differences by flow',
      partnerFieldsTitle: 'Required fields',
      paramsTitle: 'Test data',
      runModesTitle: 'Two run modes',
      ipnCasesTitle: 'Auto IPN case table',
      passCriteriaTitle: 'PASS criteria',
      ipnLogicTitle: 'IPN logic merchant must implement',
      goTo: 'Go to',
    },
  },
};

export function getGuideContent(locale) {
  return guideContent[locale] || guideContent.vi;
}
