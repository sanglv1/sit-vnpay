# Biên bản nghiệm thu PaymentLink IPN (theo demo `cttvnpay_demo`)

## Mục tiêu
Nghiệm thu luồng **PaymentLink IPN** (không nghiệm thu Return URL) theo contract/logic trong dự án mẫu `cttvnpay_demo`, vận hành tương tự cơ chế suite auto của `sit-vnpay`.

## Phạm vi
- Endpoint: `IPN` của PaymentLink
- Phương thức: `POST`
- Không bao gồm `ReturnUrl`

## Contract IPN
### Request
- URL: IPN URL của merchant/config `VNPayPaymentLinkConfig.ipnUrl` (trong demo)
- Header:
  - `vnp-signature`: chữ ký HMAC-SHA512
    - Thuật toán: `HMAC(secretKey, rawBody)` (so khớp theo ký tự raw body mà merchant nhận)
- Body:
  - Raw body là **JSON string** (giữ nguyên chuỗi để tính chữ ký)

### Response (merchant trả về)
Merchant trả về JSON object tối thiểu:
- `RspCode`: mã phản hồi
- `Message`: mô tả

## Bộ tham số JSON (rawBody) cần gửi
Merchant/`VNPayPaymentLinkService` đọc các trường:
- `btnId`
- `prodId` (optional)
- `responseCode`
- `transactionStatus`
- `transactionNo` (optional)
- `payDate`
- `bankCode`
- `amount` (đơn vị minor trong demo)

## 6 case nghiệm thu (suite auto)
Giữ nguyên thứ tự suite auto hiện có của `sit-vnpay`:
1. `INVALID_HASH`
2. `ORDER_NOT_FOUND`
3. `WRONG_AMOUNT`
4. `FAILED`
5. `SUCCESS`
6. `ORDER_ALREADY_CONFIRMED`

Mapping expected `RspCode` cho PaymentLink:
- `INVALID_HASH` → `97`
- `ORDER_NOT_FOUND` → `01`
- `WRONG_AMOUNT` → `99`
- `FAILED` → `00`
- `SUCCESS` → `00`
- `ORDER_ALREADY_CONFIRMED` → `02`

## Quy trình thực hiện (tương tự các luồng khác trong `sit-vnpay`)
### Chuẩn bị trước khi chạy suite
- Merchant phải đã có sẵn PaymentLink transaction để IPN tìm thấy được:
  - `pendingTxnRef` trong UI (SIT) tương ứng `btnId` trong PaymentLink
  - `failedTxnRef` trong UI tương ứng `btnId` của case failed

### Chạy suite auto
1. Vào `Sessions → (chọn session) → Nghiệm thu tự động`
2. Nhập:
   - Order/Transaction 1: `pendingTxnRef` (btnId sẽ dùng cho các case 1–3 và 5–6)
   - Order/Transaction 2: `failedTxnRef` (btnId case `FAILED`)
   - Amount (Transaction 1): số tiền để tạo kỳ vọng amount
3. Bấm `Run suite`
4. Hệ thống sẽ gọi IPN theo thứ tự 1→6:
   - Case `SUCCESS` chạy trước `ORDER_ALREADY_CONFIRMED` để đảm bảo merchant đã set `payerPayDate`

## Bằng chứng nghiệm thu (evidence)
Với **mỗi case** tối thiểu lưu:
- Raw request body (JSON) đã gửi
- Header `vnp-signature` (có thể mask)
- Response body (`RspCode`, `Message`)
- Tên/mã terminal (Partner) và IPN URL

Gợi ý bổ sung:
- Vì PaymentLink demo luôn trả `RspCode=00` cho cả `SUCCESS` và `FAILED`, nên nếu QC cần kiểm tra sâu trạng thái thực tế thì nên đối chiếu DB/LOG merchant để xác nhận `SUCCESS/FAILED` đúng tương ứng.

## Kết luận đạt
- 6/6 case có `RspCode` thực tế khớp expected như bảng mapping ở trên.

