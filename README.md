# Homura Shop — Java E-commerce

Ứng dụng thương mại điện tử chuyển backend từ NestJS/Prisma sang **Java 21 và Spring Boot**, gồm website khách hàng, trang quản trị và REST API với PostgreSQL.

Hướng dẫn mặc định chạy **demo local**. VNPay, MoMo, vận chuyển và email được mô phỏng trong máy; trợ lý sản phẩm tìm trong danh mục local, không gọi mô hình AI. Không cần khóa dịch vụ ngoài hoặc thanh toán thật.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Bean Validation |
| Database | PostgreSQL; JSONB cho metadata sản phẩm và dữ liệu đơn |
| Xác thực | JWT, cookie HttpOnly, BCrypt, refresh token lưu hash và xoay vòng |
| Website khách hàng | React 19, TypeScript, Vite, Material UI, TanStack Query |
| Trang quản trị | React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui, Recharts |
| Công cụ | Gradle Wrapper, Docker, PowerShell, JUnit 5, MockMvc |
| Phần mở rộng | Python/FastAPI trong `ai/`; không khởi động trong demo local |

## Chạy nhanh trên Windows

Cần **JDK 21**, **Node.js/npm** và **Docker Desktop đang chạy**. Máy kiểm chứng dùng Node.js 24. Lần cài đầu cần Internet để tải dependency/image; luồng demo không gọi dịch vụ bên ngoài.

Mở PowerShell tại thư mục dự án:

```powershell
cd client
npm ci
cd ../admin
npm ci
cd ..
./scripts/start-demo.ps1
```

| Trang | Địa chỉ |
| --- | --- |
| Website khách hàng | http://localhost:3000 |
| Trang quản trị | http://localhost:8081 |
| Java API | http://127.0.0.1:6000/api |
| PostgreSQL demo | `127.0.0.1:55432`, database `ecommerce_demo` |

| Vai trò | Email | Mật khẩu demo |
| --- | --- | --- |
| Khách hàng | `customer@homura.test` | `DemoCustomer123!` |
| Quản trị viên | `admin@homura.test` | `DemoAdmin123!` |

Script dùng container riêng `ecommerce-java-demo-db`, không dùng database NestJS gốc. Profile `demo` tạo tài khoản, ba sản phẩm và coupon `DEMO10` khi chưa tồn tại. Khởi động lại giữ dữ liệu. Shop và admin có cookie riêng nên mở đồng thời được.

```powershell
# Dừng ứng dụng/container, giữ dữ liệu
./scripts/stop-demo.ps1
# Chạy lại nhanh sau khi đã build
./scripts/start-demo.ps1 -SkipBuild
```

Log, ảnh upload và JAR chạy nằm trong `.demo/`, không đưa vào Git. Xem [RUNNING.md](RUNNING.md) để xử lý lỗi.

## Chức năng

### Khách hàng

- Đăng ký/đăng nhập, làm mới phiên, đăng xuất; sửa hồ sơ, ngày sinh, giới tính, avatar PNG/JPEG và đổi mật khẩu.
- Xem danh mục, tìm kiếm/lọc/sắp xếp sản phẩm, chọn biến thể và xem tồn kho.
- Quản lý địa chỉ và địa chỉ mặc định; bộ tỉnh/quận/phường demo có dữ liệu mẫu Hà Nội, Hồ Chí Minh, Đà Nẵng.
- Giỏ hàng, thay đổi số lượng, chọn món cần mua, áp coupon và checkout.
- COD hoặc VNPay/MoMo mô phỏng: chọn kết quả, thử lại, lưu/xóa phương thức demo. Không lưu số thẻ hay dữ liệu ngân hàng thật.
- Lịch sử đơn theo trạng thái, hủy đơn chưa trả tiền ở trạng thái cho phép; mua lại và đánh giá sau khi nhận hàng.
- Trợ lý local gợi ý sản phẩm còn hàng, liệt kê coupon và tra đơn của chính tài khoản đang đăng nhập.

### Quản trị viên

- Tạo/sửa/lưu trữ sản phẩm, metadata, biến thể, giá/tồn kho và ảnh local.
- Quản lý danh mục, coupon và người dùng; khóa/mở tài khoản, tìm kiếm đơn/người dùng.
- Chuyển trạng thái đơn; xác nhận thu tiền COD; mô phỏng hoàn tiền/trả hàng với tồn kho cập nhật một lần.
- Xem, trả lời, kiểm duyệt và xóa đánh giá; cập nhật điểm trung bình từ đánh giá.
- Doanh thu đã trả tiền, biểu đồ 30 ngày, trạng thái đơn, top sản phẩm/khách hàng và xuất CSV.
- Nhật ký thao tác quản trị và hộp thư **Email demo** lưu trong PostgreSQL, không gửi SMTP.

## Quy tắc nghiệp vụ

Checkout lấy giá/coupon từ server, khóa người dùng và biến thể trong transaction, kiểm tra tồn rồi trừ tồn ngay khi tạo đơn. Hai giỏ có thể chứa món cuối nhưng chỉ đơn còn đủ tồn được tạo. Hủy/hoàn tiền trả tồn một lần. Coupon kiểm tra trạng thái, hạn dùng, giá trị tối thiểu và lượt sử dụng; lượt đã dùng không được trả lại khi hủy.

Đơn giữ bản chụp địa chỉ khi đặt hàng. Địa chỉ đã được đơn tham chiếu không thể xóa; sửa địa chỉ không đổi thông tin giao hàng của đơn mới đã lưu snapshot.

Ship mô phỏng: 20.000đ tại Hồ Chí Minh, 30.000đ tại Hà Nội/Đà Nẵng, 40.000đ nơi khác; miễn phí khi tổng sau giảm đạt 500.000đ.

```text
pending → confirmed → processing → shipping → delivered
```

Online phải trả tiền trước khi admin xử lý. Thất bại chuyển sang `payment_failed`, giữ hàng để thử lại/hủy. Phiên thanh toán hết hạn sau 15 phút: tạo lại hoặc hủy đơn; chưa có job tự động giải phóng hàng. Mã `DEMO-GHTK-*` và shipping log chỉ mô phỏng.

Hoàn tiền cho đơn đã trả tiền chưa gửi (`pending`, `confirmed`) hoặc đã giao (`delivered`, coi là đã nhận hàng trả về). Đơn sau hoàn tiền bị hủy, trả tồn kho và không còn được tính vào doanh thu đã trả tiền.

## Cấu trúc và kiến trúc

```text
ecommerce-web/
├── client/                 Website khách hàng
├── admin/                  Trang quản trị
├── server/ecommerce/       Spring Boot
│   └── src/main/java/com/ankkun/ecommerce/
│       ├── common/         DTO, enum, lỗi
│       ├── security/       JWT, cookie, phân quyền
│       ├── config/         Seed demo, nhật ký
│       └── module/         Auth, user, product, cart, order, payment...
├── ai/                     Sidecar Python tùy chọn
├── scripts/                Khởi động/dừng demo, smoke test
├── README.md
└── RUNNING.md
```

Frontend gọi `/api` qua Vite proxy tới Java port 6000. Backend đi theo controller → service → repository; JPA quản lý transaction/khóa dữ liệu. PostgreSQL lưu nghiệp vụ, refresh token, nhật ký và outbox. Ảnh ở `.demo/uploads/`, phục vụ qua `/api/files/...`.

## API chính

Response thường có dạng `{ success, message, data }`; phân trang có `data, page, limit, total, total_pages`. Trường JSON dùng `snake_case`. Upload sản phẩm admin trả trực tiếp `{ url }`.

| Nhóm | Endpoint tiêu biểu |
| --- | --- |
| Xác thực | `POST /api/auth/register`, `/login`, `/admin/login`, `/refresh`, `/logout`; `GET /api/auth/me` |
| Hồ sơ | `GET/PATCH /api/users/me`; `POST /api/users/me/avatar`, `/me/change-password` |
| Danh mục/sản phẩm | `GET /api/categories`, `/products`, `/products/{variantId}`, `/products/{productId}/variants` |
| Địa chỉ | `GET/POST /api/addresses`; `PATCH/DELETE /api/addresses/{id}`; `PATCH /{id}/default` |
| Giỏ hàng | `GET /api/carts`; `POST /api/carts/items`; `PATCH/DELETE /api/carts/items/{id}` |
| Đơn | `GET/POST /api/orders`; `GET /{id}`; `POST /{id}/cancel` |
| Coupon | `GET /api/coupons/active`; `POST /validate`; admin CRUD `/api/coupons` |
| Thanh toán demo | `POST /api/payments/create-url`, `/momo/create/{id}`; `GET /demo/{id}`; `POST /demo/{id}/complete`; `/methods` |
| Đánh giá | `GET /api/reviews/product/{id}`; `POST /api/reviews`; `PATCH/DELETE /{id}` |
| Trợ lý local | `POST /api/chat/completions` |
| Admin | `/api/admin/products`, `/categories`, `/orders`, `/users`, `/reviews`, `/audit-logs`, `/mail/outbox`, `/stats/overview` |

API kiểm tra quyền sở hữu địa chỉ, giỏ, đơn, đánh giá và phương thức thanh toán. Khách không truy cập API admin. DTO người dùng/địa chỉ không trả password hash hoặc refresh token.

## Cấu hình

Backend đọc **biến môi trường tiến trình**, không tự đọc `.env`. Script demo đặt profile/thông số local; Docker Compose có cấu hình riêng và không cần dùng cho hướng dẫn này.

| Biến | Mục đích |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=demo` | Seed và mô phỏng local |
| `DATABASE_URL` | URL JDBC PostgreSQL, ví dụ `jdbc:postgresql://127.0.0.1:55432/ecommerce_demo` |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản database |
| `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET` | Hai khóa JWT riêng |
| `SERVER_PORT`, `SERVER_ADDRESS` | Demo dùng 6000/loopback |
| `FRONTEND_URL`, `COOKIE_SECURE` | Origin và cookie HTTPS |
| `VITE_PROXY_TARGET` | Backend cho Vite proxy |

Trong profile demo, thanh toán đi vào mô phỏng dù máy có biến VNPay. Email chỉ ghi outbox, chat dùng local. Biến tích hợp cũ còn trong code để phát triển sau; có cấu hình không có nghĩa tích hợp thật đã được kiểm chứng.

## Kiểm thử

PostgreSQL demo cần chạy trước:

```powershell
# HTTP smoke test, demo đang chạy
./scripts/smoke-test.ps1
# Kiểm thử tính năng mở rộng và hai checkout đồng thời (cần Python 3)
python scripts/smoke-features.py

# Integration tests và đóng gói Java
$env:DATABASE_URL='jdbc:postgresql://127.0.0.1:55432/ecommerce_demo'
$env:DB_USERNAME='demo'
$env:DB_PASSWORD='demo_local_only'
cd server/ecommerce
./gradlew.bat test bootJar --max-workers=1 -I ../../scripts/demo-build.gradle

# Chạy riêng tại client/ và admin/
npm run build
```

**17 bài backend**: 8 `ShoppingFlowTests`, 8 `DemoFeaturesTests`, 1 `contextLoads`. Kiểm tra cookie/refresh/replay, quyền sở hữu, tồn kho/coupon, checkout món chọn, thanh toán thành công/thất bại/thử lại/hết hạn, phương thức lưu, hoàn tiền một lần, đánh giá sau nhận hàng, upload và thu hồi access token sau đổi mật khẩu.

Fixture integration rollback; file ảnh kiểm thử có thể còn trong thư mục upload. Smoke tạo dữ liệu giả trong database demo. Script smoke-features.py kiểm tra hai checkout đồng thời tranh một món cuối, VNPay/MoMo thử lại, giao hàng/đánh giá/hoàn tiền, coupon, outbox, audit, thống kê và quyền riêng tư chat; sản phẩm fixture được lưu trữ sau khi test qua. Báo cáo JUnit: `server/ecommerce/build/demo/reports/tests/test/index.html`. Frontend được kiểm chứng bằng build và chạy thử trình duyệt; chưa có bộ E2E tự động đầy đủ hoặc benchmark tải.


