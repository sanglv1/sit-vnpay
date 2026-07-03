# Quy trình deploy bản cập nhật mới (sit-vnpay VPS)

Setup **thực tế** trên VPS `160.250.128.143`:

| Thành phần | Cách chạy |
|------------|-----------|
| **API** | `systemd` service `sit-api.service` → `java -jar sit-api/target/sit-api-*.jar` |
| **PostgreSQL** | Cài trực tiếp trên VPS (port 5432) |
| **UI** | Build React trên Windows → `scp` → **`/var/www/sit-vnpay/`** |
| **Nginx** | `/sit-api/` → API (port 8001), `/sit-ui/` → static từ `/var/www/sit-vnpay/` |

> **Lưu ý:** Không dùng `docker compose` trên VPS này (plugin Compose chưa cài). Docker trong repo chỉ dùng **PostgreSQL local** khi dev (`sit-api/docker-compose.yml`).

## Tổng quan

```
Windows:  git push → build UI (REACT_APP_*) → scp → /var/www/sit-vnpay/
VPS:      git pull → mvn package → systemctl restart sit-api
Browser:  tab ẩn danh → http://<IP>/sit-ui/login
```

---

## Bước 1 — Windows: Push code lên GitHub

```powershell
cd D:\VNPAY\demo_javaVnpay\sit-vnpay

git status
git add .
# Không commit file .env chứa secret
git commit -m "Mô tả ngắn thay đổi"
git push origin main
```

---

## Bước 2 — Windows: Build UI

**Bắt buộc** set biến môi trường **trước** `npm run build` (nhúng vào bundle lúc build):

```powershell
cd D:\VNPAY\demo_javaVnpay\sit-vnpay\sit-ui
npm install

$env:REACT_APP_BASENAME = "/sit-ui"
$env:REACT_APP_API_URL = "http://160.250.128.143/sit-api"
npm run build
```

**Command Prompt (cmd):**

```cmd
cd D:\VNPAY\demo_javaVnpay\sit-vnpay\sit-ui
set "REACT_APP_BASENAME=/sit-ui"
set "REACT_APP_API_URL=http://160.250.128.143/sit-api"
npm run build
```

### Kiểm tra build trước khi upload

```powershell
$HASH = (Select-String build\index.html -Pattern 'main\.([^"]+)\.js').Matches.Groups[1].Value
(Select-String "build\static\js\main.$HASH.js" -Pattern '/sit-ui' -AllMatches).Matches.Count
```

Kỳ vọng: **≥ 5** (khoảng 6). Nếu chỉ **1** → thiếu `REACT_APP_BASENAME` → màn trắng sau deploy.

Thư mục output: `sit-ui\build\`.

---

## Bước 3 — Upload UI lên VPS

**Thư mục đích đúng:** `/var/www/sit-vnpay/` (Nginx phục vụ `/sit-ui/` từ đây).

**Không** upload vào `/var/www/sit-ui/` — thư mục đó Nginx không dùng.

Trên VPS (SSH), xóa bản cũ trước:

```bash
rm -rf /var/www/sit-vnpay/*
```

Trên Windows (trong `sit-ui`):

```powershell
scp -r .\build\* root@160.250.128.143:/var/www/sit-vnpay/
```

---

## Bước 4 — VPS: Cập nhật API (SSH)

```bash
ssh root@160.250.128.143

cd /opt/sit-vnpay
git pull origin main
```

Nếu `git pull` bị chặn bởi `chmod` trên `reset-admin.sh`:

```bash
git diff sit-api/scripts/reset-admin.sh   # thường chỉ đổi quyền 100644→100755
git restore sit-api/scripts/reset-admin.sh
git pull origin main
```

Build jar và restart:

```bash
cd /opt/sit-vnpay/sit-api
mvn -B -DskipTests -Dcheckstyle.skip=true package
systemctl restart sit-api
```

Theo dõi log (đợi `Started SitVnpayApplication`):

```bash
journalctl -u sit-api -f
```

`Ctrl+C` để thoát log (service vẫn chạy).

**Flyway:** migration DB mới tự chạy khi API khởi động. **Không xóa** database PostgreSQL — dữ liệu phiên/đối tác giữ nguyên.

---

## Bước 5 — VPS: Kiểm tra sau deploy

### API

```bash
git log -1 --oneline
systemctl status sit-api --no-pager
curl -s http://127.0.0.1:8001/sit-api/health
```

Kỳ vọng: `{"status":"UP"}`.

### UI (disk vs Nginx phải khớp)

```bash
HASH=$(grep -o 'main\.[^"]*\.js' /var/www/sit-vnpay/index.html)
echo "disk index -> $HASH"
ls -la /var/www/sit-vnpay/static/js/$HASH

grep -c '/sit-ui' /var/www/sit-vnpay/static/js/$HASH
curl -s http://127.0.0.1/sit-ui/index.html | grep -o 'main\.[^"]*\.js'
curl -s -o /dev/null -w "js: %{http_code}\n" "http://127.0.0.1/sit-ui/static/js/$HASH"
```

| Kiểm tra | Kỳ vọng |
|----------|---------|
| `curl index.html` vs `grep disk` | Cùng hash `main.*.js` |
| `grep -c '/sit-ui'` | ≥ 5 |
| `js: %{http_code}` | 200 |

---

## Bước 6 — Trình duyệt

| URL | Kỳ vọng |
|-----|---------|
| `http://160.250.128.143/sit-api/health` | `{"status":"UP"}` |
| `http://160.250.128.143/sit-ui/login` | Form đăng nhập |

- Dùng **tab ẩn danh** hoặc Ctrl+Shift+R (tránh cache `index.html` / JS cũ).
- F12 → Network: file `main.*.js` phải **200**, khớp hash trên VPS.

### Test tính năng theo luồng

Vẫn chỉ **2 tab**: **Nghiệm thu tự động** / **Nghiệm thu thủ công**. Nội dung đổi theo luồng đối tác:

| Luồng đối tác | Tab thủ công | Tab tự động |
|---------------|--------------|-------------|
| **TOKEN** | 8 mục API Token | Mở **Tùy chọn nâng cao: loại IPN** → dropdown command |
| **RECURRING** | 14 mục | Dropdown command Recurring |
| **INSTALMENT** | 8 mục | — |
| **PAY** | Form Return URL cũ (không đổi) | — |

Tạo **đối tác + phiên mới** đúng luồng để kiểm tra (phiên PAY cũ không đổi giao diện).

---

## Lệnh hữu ích

```bash
# Log API
journalctl -u sit-api -n 50 --no-pager
journalctl -u sit-api -f

# Restart sau khi sửa .env (không cần mvn nếu không đổi code)
systemctl restart sit-api

# Rebuild API (sau git pull có đổi Java)
cd /opt/sit-vnpay/sit-api
mvn -B -DskipTests -Dcheckstyle.skip=true package
systemctl restart sit-api
```

**Reset mật khẩu admin:**

```bash
cd /opt/sit-vnpay/sit-api
chmod +x scripts/reset-admin.sh
./scripts/reset-admin.sh admin@vnpay.vn 'MatKhauMoi'
```

(`chmod` trên VPS có thể làm `git diff` báo thay đổi — bình thường, không ảnh hưởng deploy.)

---

## Lưu ý

| Mục | Ghi chú |
|-----|---------|
| **API trên VPS** | `systemctl restart sit-api` — **không** dùng `docker compose` |
| **UI upload** | **`/var/www/sit-vnpay/`** — không phải `/var/www/sit-ui/` |
| **Giữ nguyên** | `/opt/sit-vnpay/.env` trên VPS — không commit |
| **JWT secret** | Không đổi `SIT_JWT_SECRET` nếu không muốn logout toàn bộ user |
| **CORS** | `SIT_CORS_ORIGINS=http://160.250.128.143` (không kèm `/sit-ui`) |
| **Ảnh QC** | Nginx `client_max_body_size 15m;` — thiếu → lỗi 413 khi lưu QC |
| **Docker local** | Chỉ PostgreSQL dev (`cd sit-api && docker compose up -d`) |

### Nginx (tham khảo)

```nginx
server {
    listen 80;
    server_name 160.250.128.143;

    client_max_body_size 15m;

    location /sit-api/ {
        proxy_pass http://127.0.0.1:8001/sit-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location = /sit-ui/index.html {
        alias /var/www/sit-vnpay/index.html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location /sit-ui/ {
        alias /var/www/sit-vnpay/;
        try_files $uri $uri/ /sit-ui/index.html;
    }
}
```

Sau khi sửa Nginx: `nginx -t` → `systemctl reload nginx`.

---

## Xử lý sự cố

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| UI cũ / không thấy tính năng mới | `scp` nhầm `/var/www/sit-ui/` | Upload lại vào **`/var/www/sit-vnpay/`** |
| Màn trắng | Build thiếu `REACT_APP_BASENAME` | Build lại với env, `grep -c '/sit-ui'` ≥ 5 |
| Browser load JS hash khác VPS | Cache `index.html` | Tab ẩn danh; so `curl` vs `grep` index |
| `git pull` bị chặn | `chmod +x` trên `reset-admin.sh` | `git restore sit-api/scripts/reset-admin.sh` rồi pull |
| Health UP nhưng tính năng cũ | Chưa `mvn package` + restart | `systemctl restart sit-api` sau build |
| Form PAY dù test TOKEN | Phiên/đối tác luồng PAY | Tạo đối tác TOKEN + phiên mới |
| Lưu QC lỗi 413 | Nginx body limit | `client_max_body_size 15m;` |
| `curl` health không UP | Flyway / DB / `.env` | `journalctl -u sit-api -n 80` |

---

## Checklist nhanh

- [ ] `git push` từ Windows
- [ ] `npm run build` với `REACT_APP_BASENAME` + `REACT_APP_API_URL`
- [ ] Kiểm tra `grep /sit-ui` trong JS ≥ 5
- [ ] `rm -rf /var/www/sit-vnpay/*` → `scp` vào **`/var/www/sit-vnpay/`**
- [ ] VPS: `git pull` → `mvn package` → `systemctl restart sit-api`
- [ ] `curl` health = `UP`; Nginx và disk cùng hash `main.*.js`
- [ ] Browser tab ẩn danh → `/sit-ui/login` OK

**Cả 7 mục tick → deploy bản cập nhật xong.**
