# sit-vnpay — VNPay SIT Testing Tool

Công cụ nội bộ mô phỏng VNPay gửi callback **Return URL** và **IPN URL** tới server đối tác.

## Cấu trúc

```
sit-vnpay/
├── sit-api/     Spring Boot REST API (port 8001)
└── sit-ui/      React SPA theo pattern vsm-ui (port 8000)
```

## Yêu cầu

- Java 17+, Maven 3.8+
- Node.js 18+, npm

## Chạy development

**Terminal 0 — PostgreSQL (bắt buộc cho profile `dev` và `prod`):**

```bash
cd sit-api
docker compose up -d
```

PostgreSQL local: `localhost:5434`, database `sit_vnpay_db`, user `postgres` / password `sit123456`.

**Terminal 1 — API:**

```bash
cd sit-api
# Sao chép và chỉnh .env.example → export biến môi trường (xem mục Cấu hình deploy)
mvn spring-boot:run
```

API: http://localhost:8001/sit-api

**Terminal 2 — UI:**

```bash
cd sit-ui
npm install
npm start
```

UI: http://localhost:8000/sit-ui

Trang **Hướng dẫn thực hiện** (menu sidebar): http://localhost:8000/sit-ui/guide

## API endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/api/dashboard` | Thống kê dashboard |
| GET | `/api/partners` | Danh sách đối tác |
| GET | `/api/partners/{id}` | Chi tiết đối tác |
| POST | `/api/partners` | Tạo đối tác |
| PUT | `/api/partners/{id}` | Cập nhật đối tác |
| DELETE | `/api/partners/{id}` | Xóa đối tác |
| GET | `/api/sessions` | Danh sách phiên kiểm thử |
| GET | `/api/sessions/{id}` | Chi tiết phiên |
| GET | `/api/sessions/{id}/export-minutes` | Xuất biên bản SIT (`.docx`, tên file `VNPAYGW-{tmnCode}-SIT.docx`) |
| POST | `/api/sessions` | Tạo phiên kiểm thử |
| GET | `/api/tests/metadata` | Metadata form chạy test |
| POST | `/api/tests/run` | Thực thi callback (gắn `sessionId`) |
| POST | `/api/tests/run-ipn-suite` | Chạy tự động toàn bộ case IPN trong phiên |
| GET | `/api/tests` | Lịch sử (phân trang) |
| GET | `/api/tests/{id}` | Chi tiết kết quả |
| GET/POST | `/api/manual-acceptance` | Lưu/tải QC thủ công theo phiên |
| GET/POST/PUT/PATCH/DELETE | `/api/users` | Quản lý người dùng (chỉ ADMIN) |
| POST | `/api/auth/login` | Đăng nhập, trả JWT |
| GET | `/api/auth/me` | Thông tin user hiện tại |

Response format: `{ "code": "00", "data": ..., "rspMsg": "Success" }`

**UI types:** `sit-ui/src/types/api.d.ts` mô tả toàn bộ DTO (mirror `sit-api`). Gọi API qua `sit-ui/src/api/client.js` (`sitApi.*`); data fetching dùng React Query hooks trong `sit-ui/src/api/hooks.js`.

Gửi JWT qua header: `Authorization: Bearer <token>`

## Đăng nhập

UI: http://localhost:8000/sit-ui/login

| Quyền | Truy cập |
|-------|----------|
| `ADMIN` | Xem và theo dõi hành động **tất cả tài khoản** (Terminal, phiên, lịch sử test, dashboard); quản lý người dùng |
| `MERCHANT_QC` | Chỉ xem hành động **của mình** (Terminal/phiên/lịch sử do mình tạo); không xem dữ liệu tài khoản khác; không vào `/users` |

Lần đầu chạy API (DB chưa có user), hệ thống tự tạo admin theo `SIT_ADMIN_EMAIL` / `SIT_ADMIN_PASSWORD` (xem bảng cấu hình bên dưới).

## Cấu hình deploy (biến môi trường)

| Biến | Bắt buộc | Mô tả |
|------|----------|-------|
| `SIT_JWT_SECRET` | Có | Secret ký JWT, **tối thiểu 32 ký tự** — đổi mỗi môi trường |
| `SIT_JWT_EXPIRATION_MS` | Không | Thời hạn token (ms), mặc định `86400000` (24h) |
| `SIT_ADMIN_EMAIL` | Khi seed admin | Email admin tạo lần đầu |
| `SIT_ADMIN_PASSWORD` | Khi seed admin | Mật khẩu admin tạo lần đầu (tối thiểu 6 ký tự) |
| `SIT_ADMIN_NAME` | Không | Tên hiển thị admin, mặc định `System Admin` |
| `SPRING_PROFILES_ACTIVE` | Không | `dev` (mặc định) hoặc `prod` |
| `DB_URL` | Prod/VPS | JDBC URL PostgreSQL (vd. `jdbc:postgresql://localhost:5432/sit_vnpay_db`) |
| `DB_USERNAME` / `DB_PASSWORD` | Prod | User/password PostgreSQL |
| `SPRING_DATASOURCE_URL` | Render | Ưu tiên hơn `DB_URL` khi deploy Docker/Render |
| `SIT_CORS_ORIGINS` | Prod | Origin UI (vd. `http://160.250.128.143`) — không kèm `/sit-ui` |

Mẫu cấu hình: `sit-api/.env.example`. **Không commit** file `.env` thật hoặc secret production.

**Ảnh QC trong biên bản Word (deploy VPS/Nginx):** Tab QC thủ công gửi ảnh dạng base64 trong JSON (~3MB/ảnh). Cần cấu hình:

- Nginx: `client_max_body_size 15m;` trong `server` block (mặc định chỉ `1m` → ảnh không lưu được, xuất biên bản thiếu ảnh).
- API Docker: image JRE cần `fontconfig` để nhúng ảnh vào DOCX (đã cấu hình trong `sit-api/Dockerfile`).

Sau khi sửa Nginx, **upload lại ảnh** trên tab QC thủ công → Lưu → Xuất biên bản.

### Profile & database

| Profile | Database | Schema |
|---------|----------|--------|
| `dev` (mặc định) | PostgreSQL (`localhost:5434/sit_vnpay_db`) | Flyway migrate + Hibernate `validate` |
| `test` (JUnit) | H2 in-memory | Flyway migrate + Hibernate `validate` |
| `prod` | PostgreSQL native `localhost:5432` hoặc qua env | Flyway migrate + Hibernate `validate` |

Migration SQL: `sit-api/src/main/resources/db/migration/`. Thêm migration mới khi đổi schema (vd. `V2__add_column.sql`).

**Nâng cấp từ bản cũ:** nếu khởi động lỗi validate schema, reset volume PostgreSQL local (`docker compose down -v` rồi `docker compose up -d`) hoặc chạy Flyway baseline thủ công trên DB production đã có sẵn bảng.

Ví dụ chạy local với profile `dev` (PowerShell):

```powershell
cd sit-api
docker compose up -d
$env:SIT_JWT_SECRET = "dev-only-secret-min-32-characters-long"
$env:SIT_ADMIN_EMAIL = "admin@vnpay.vn"
$env:SIT_ADMIN_PASSWORD = "YourSecurePassword"
# Tùy chọn — mặc định dev đã trỏ PostgreSQL local
$env:DB_URL = "jdbc:postgresql://localhost:5434/sit_vnpay_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sit123456"
mvn spring-boot:run
```

Chạy API với profile `prod` (PostgreSQL cài trực tiếp, port **5432**):

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/sit_vnpay_db"
$env:DB_USERNAME = "sit_vnpay"
$env:DB_PASSWORD = "YourSecurePassword"
$env:SIT_JWT_SECRET = "change-me-to-a-random-secret-at-least-32-chars"
$env:SIT_ADMIN_EMAIL = "admin@vnpay.vn"
$env:SIT_ADMIN_PASSWORD = "YourSecurePassword"
$env:SIT_CORS_ORIGINS = "http://160.250.128.143"
mvn spring-boot:run
```

### Deploy VPS (PostgreSQL native + Nginx + systemd)

1. Cài PostgreSQL trên VPS, tạo DB `sit_vnpay_db` và user (vd. `sit_vnpay`).
2. Copy `sit-api/.env.example` → `sit-api/.env`, set `SPRING_PROFILES_ACTIVE=prod` và `DB_URL=jdbc:postgresql://localhost:5432/sit_vnpay_db`.
3. API và DB cùng VPS → host luôn là `localhost` (không dùng IP public cho JDBC).
4. Build API: `mvn -B -DskipTests package` → chạy JAR qua systemd (`EnvironmentFile=.env`).
5. Build UI: `REACT_APP_BASENAME=/sit-ui`, `REACT_APP_API_URL=http://<VPS_IP>/sit-api`.
6. Nginx: `client_max_body_size 15m;`, proxy `/sit-api/` → `127.0.0.1:8001`, static `/sit-ui/`.

**Reset mật khẩu admin trên VPS** (dùng cùng JDBC + BCrypt như API):

```bash
cd sit-api
chmod +x scripts/reset-admin.sh
./scripts/reset-admin.sh sanglv@vnpay.vn 'YourPassword'
```

## Luồng hỗ trợ

PAY, TOKEN, RECURRING, INSTALMENT

## Coverage gate backend (lộ trình)

`sit-api` dùng JaCoCo check tại phase `verify` với ngưỡng line coverage cấu hình theo property:

- mặc định (an toàn CI hiện tại): `0.10`
- sprint gate: `0.30`
- target gate: `0.40`

Chạy theo từng mức:

```bash
cd sit-api

# Mặc định CI hiện tại
mvn verify

# Gate sprint 30%
mvn verify -Pcoverage-sprint

# Gate mục tiêu 40%
mvn verify -Pcoverage-target
```

Có thể override linh hoạt theo pipeline:

```bash
mvn verify -Djacoco.line.coverage.minimum=0.35
```

## Hướng dẫn thực hiện

Quy trình kiểm thử SIT gồm **6 bước**. Cả 4 luồng (PAY, TOKEN, RECURRING, INSTALMENT) đều đi theo cùng quy trình; khác nhau ở cấu hình đối tác và bộ tham số callback mà hệ thống tự sinh.

```
Chuẩn bị merchant → Tạo đối tác → Tạo phiên → Nghiệm thu IPN (tự động)
                                                          ↓
                              Xem kết quả ← Lưu QC thủ công ← Nghiệm thu thủ công
```

### Bước 1 — Chuẩn bị phía merchant

Trước khi dùng SIT, merchant cần:

1. Tích hợp xong luồng thanh toán tương ứng.
2. Cung cấp cho QC: **TMN Code**, **Secret Key**, **Return URL**, **IPN URL**.
3. Đảm bảo server IPN xử lý đúng thứ tự kiểm tra và trả `RspCode` theo chuẩn VNPay.

**Chuẩn bị 2 giao dịch cho nghiệm thu IPN tự động** (trên giao diện merchant, không qua SIT):

1. Tạo **giao dịch 1** (Pay) → đến màn **OTP** → **dừng**, chưa xác nhận OTP → copy `txnRef` + số tiền.
2. Tạo **giao dịch 2** (`txnRef` khác) → đến **OTP** → **dừng** → copy `txnRef` + số tiền.
3. Nhập vào SIT: đơn 1 → ô **Case 5**; đơn 2 → ô **Case 6**.

SIT chỉ **giả lập IPN** gửi về merchant — không cần hoàn tất thanh toán trên cổng VNPay.

### Bước 2 — Tạo đối tác (`/partners/create`)

Vào **Đối tác → Thêm đối tác**, khai báo:

| Trường | Mô tả |
|--------|-------|
| Tên đối tác | Tên merchant / dự án |
| Luồng | `PAY`, `TOKEN`, `RECURRING` hoặc `INSTALMENT` |
| TMN Code | Mã terminal merchant |
| Secret Key | Khóa bí mật ký HMAC |
| Return URL | URL redirect sau thanh toán |
| IPN URL | URL API nhận callback server-to-server |

**Khác biệt theo luồng** (hệ thống tự xử lý khi gửi callback):

| Luồng | Đặc điểm callback |
|-------|-------------------|
| PAY | PascalCase: `vnp_TmnCode`, `vnp_TxnRef`, `vnp_Amount`, `vnp_OrderInfo`, `vnp_BankCode`, `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_SecureHash` — ký UTF-8 |
| TOKEN | snake_case: `vnp_tmn_code`, `vnp_txn_ref`, `vnp_amount`, `vnp_command`, `vnp_app_user_id`, `vnp_txn_desc`, `vnp_curr_code`, `vnp_response_code`, `vnp_transaction_status`, `vnp_secure_hash`; thêm `vnp_token`, `vnp_card_number` (tùy chọn) khi GD thành công |
| RECURRING | snake_case: `vnp_command=pay_n_recurring`, `vnp_order_info`, `vnp_app_user_id`, `vnp_curr_code`, `vnp_response_code`, `vnp_transaction_status`, `vnp_secure_hash`; thêm `vnp_token`, `vnp_token_exp_date`, `vnp_card_number`, `vnp_bank_code`, `vnp_bank_tran_no`, `vnp_card_type` khi GD thành công |
| INSTALMENT | PascalCase (tương tự PAY): `vnp_TmnCode`, `vnp_TxnRef`, `vnp_Amount`, `vnp_OrderInfo`, `vnp_BankCode`, `vnp_ResponseCode`, `vnp_TransactionStatus`, `vnp_SecureHash` — ký UTF-8 |

### Bước 3 — Tạo phiên kiểm thử (`/sessions/new`)

1. Vào **Phiên kiểm thử → Tạo phiên**.
2. Chọn đối tác (đã gắn luồng).
3. Nhập ghi chú (tùy chọn) → **Tạo phiên**.

Hệ thống chuyển sang tab **Nghiệm thu tự động**. Mỗi phiên theo dõi tiến độ `X/6 ĐẠT`.

### Bước 4 — Nghiệm thu tự động: IPN (`/sessions/{id}/auto`)

Tool mô phỏng VNPay gửi **HTTP GET** tới **IPN URL** của merchant, kèm header `X-Forwarded-For: 113.160.92.202` (IP sandbox VNPay).

**Chuẩn bị dữ liệu trên form SIT** (sau khi tạo 2 GD dừng OTP trên merchant):

| Tham số | Mô tả |
|---------|-------|
| Đơn Case 5 | `txnRef` + số tiền **giao dịch 1** (dừng OTP) — dùng Case 3, 5; Case 4 gửi lại IPN sau Case 5 |
| Đơn Case 6 | `txnRef` + số tiền **giao dịch 2** (dừng OTP, **khác** Case 5) — mỗi txnRef chỉ cập nhật DB một lần (`00`), gọi lại trả `02` |
| Số tiền sai (VND) | Dùng cho Case 3 — ghi đè amount trong IPN giả lập (mặc định = số tiền Case 5 + 1.000 VND) |

**Hai cách chạy:**

- **Gửi 1 callback IPN** — chọn test case riêng lẻ rồi gửi.
- **Tiến hành kiểm tra tự động** — chạy lần lượt toàn bộ 6 case:

| Bước | Case | Kịch bản | RspCode mong đợi |
|------|------|----------|------------------|
| 1 | Case 1 | Chữ ký không hợp lệ (`vnp_SecureHash` / `vnp_secure_hash`) | **97** |
| 2 | Case 2 | `vnp_TxnRef` / `vnp_txn_ref` không tồn tại (tool tự sinh mã giả) | **01** |
| 3 | Case 3 | `vnp_Amount` / `vnp_amount` không khớp | **04** |
| 4 | Case 6 | Giao dịch thất bại (`vnp_ResponseCode` ≠ 00) — **đơn txnRef riêng** | **00** |
| 5 | Case 5 | Giao dịch thành công — **đơn txnRef riêng** | **00** |
| 6 | Case 4 | Gửi lại IPN đơn Case 5 khi đã xác nhận | **02** |

**Tiêu chí PASS:** HTTP 2xx, không lỗi kết nối, `RspCode` trong response body khớp giá trị mong đợi.

**Logic IPN merchant cần đáp ứng:**

```
1. Kiểm tra chữ ký
   PAY/INSTALMENT: vnp_SecureHash | TOKEN/RECURRING: vnp_secure_hash → sai: RspCode 97
2. Kiểm tra mã giao dịch
   PAY/INSTALMENT: vnp_TxnRef | TOKEN/RECURRING: vnp_txn_ref → không có: RspCode 01
3. Kiểm tra số tiền
   PAY/INSTALMENT: vnp_Amount | TOKEN/RECURRING: vnp_amount → sai: RspCode 04
4. Kiểm tra trạng thái đơn → đã xử lý: RspCode 02
5. Kiểm tra kết quả giao dịch
   PAY/INSTALMENT: vnp_ResponseCode=00 & vnp_TransactionStatus=00 → SUCCESS → RspCode 00
   TOKEN/RECURRING: vnp_response_code=00 & vnp_transaction_status=00 → SUCCESS → RspCode 00
   Ngược lại → FAIL → RspCode 00 (đã nhận IPN)
```

### Bước 5 — Nghiệm thu thủ công (`/sessions/{id}/manual`)

Các hạng mục sau **không tự động** — QC xác nhận bằng checklist và bằng chứng:

| Hạng mục | Nội dung | Cách thực hiện |
|----------|----------|----------------|
| Return URL — GD thành công | Merchant hiển thị đúng trang kết quả | Thực hiện GD thật trên cổng VNPay → nhập `vnp_TxnRef` / `vnp_txn_ref` + upload ảnh chụp màn hình |
| Return URL — GD thất bại | Merchant hiển thị đúng trang thất bại | Tương tự, dùng `vnp_TxnRef` / `vnp_txn_ref` của GD thất bại |
| Case ex | Xử lý exception ở đầu IPN URL | Xác nhận merchant trả **RspCode 99** khi lỗi không xử lý được |
| Whitelist IP | Merchant đã whitelist IP VNPay | Xác nhận IPN chỉ chấp nhận request từ dải IP VNPay |
| Lưu log | Merchant lưu log request/response | Xác nhận có log đầy đủ hai chiều |

Bấm **Lưu kết quả QC** để lưu theo phiên.

### Bước 6 — Xem kết quả & xuất biên bản

| Màn hình | Nội dung |
|----------|----------|
| `/sessions/{id}/suite-result` | Kết quả sau khi chạy full IPN suite |
| `/tests/{id}` | Chi tiết 1 lần chạy (request, response, pass/fail) |
| `/tests/history` | Lịch sử toàn bộ lần chạy |
| `/sessions` | Tiến độ `X/6 ĐẠT` theo phiên |

**Xuất biên bản:** Trong phiên kiểm thử, bấm **Xuất biên bản** để tải file Word theo mẫu VNPay (PAY / TOKEN / RECURRING / INSTALMENT). Tên file: `VNPAYGW-{tmnCode}-SIT.docx`. Hệ thống tự điền thông tin merchant, kết quả IPN tự động và dữ liệu nghiệm thu thủ công.

### Mẫu biên bản Word (DOCX)

Bốn file template nằm trong repo tại `sit-api/src/main/resources/templates/minutes/`:

| File template | Luồng thanh toán |
|---------------|------------------|
| `VNPAYGW-Pay-SIT-VN.docx` | PAY |
| `VNPAYGW-Token-SIT-VN.docx` | TOKEN |
| `VNPAYGW-Recurring-SIT-VN.docx` | RECURRING |
| `VNPAYGW-Installment-SIT-VN.docx` | INSTALMENT |

API chọn template theo `PaymentFlow` của Terminal; file tải về đặt tên `VNPAYGW-{tmnCode}-SIT.docx`.

**Nếu thiếu template sau khi clone:** lấy lại từ git (`git checkout HEAD -- sit-api/src/main/resources/templates/minutes/`) hoặc copy từ bản phát hành nội bộ VNPay. Khi VNPay cập nhật mẫu biên bản chính thức, thay file tương ứng trong thư mục trên — **giữ nguyên tên file** và các nhãn đoạn văn mà code filler đọc (vd. `Return URL`, `IPN URL`, `Input:`, `Output:`, `Giao dịch thành công`, …).

### Checklist nhanh

| # | Việc cần làm | Tự động / Thủ công |
|---|--------------|-------------------|
| 1 | Tạo đối tác đúng luồng | Thủ công |
| 2 | Tạo phiên kiểm thử | Thủ công |
| 3 | Tạo 2 GD trên merchant (dừng OTP), nhập txnRef Case 5 & 6 | Thủ công |
| 4 | Case 1–6 IPN (97, 01, 04, 00, 00, 02) | **Tự động** |
| 5 | Return URL thành công / thất bại | **Thủ công** |
| 6 | Case ex (RspCode 99) | **Thủ công** |
| 7 | Whitelist IP | **Thủ công** |
| 8 | Lưu log | **Thủ công** |

## Quản lý người dùng (`/users` — ADMIN)

| Quyền | Mô tả |
|-------|-------|
| `ADMIN` | Quản trị hệ thống |
| `MERCHANT_QC` | Kiểm thử / QC merchant |
