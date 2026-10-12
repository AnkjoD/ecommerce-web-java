# Vận hành demo local

Tính năng, API, kiến trúc và kiểm thử: [README.md](README.md).

## Khởi động

Cài JDK 21, Node.js/npm, mở Docker Desktop. Chạy `npm ci` trong `client/` và `admin/` lần đầu. Tại root:

```powershell
./scripts/start-demo.ps1
```

- Shop: http://localhost:3000
- Admin: http://localhost:8081
- API: http://127.0.0.1:6000/api
- Khách: `customer@homura.test` / `DemoCustomer123!`
- Admin: `admin@homura.test` / `DemoAdmin123!`
- Coupon: `DEMO10`

Port 3000, 6000, 8081, 55432 cần trống. Mở frontend bằng localhost. Khởi động lại không reset dữ liệu. Không chạy Python sidecar, SMTP hoặc thanh toán/giao hàng thật.

## Kịch bản thử

1. Đăng nhập khách, thêm địa chỉ giả lập tại Hồ sơ → Địa chỉ.
2. Chọn sản phẩm/biến thể, thêm giỏ, áp DEMO10, chọn món mua.
3. Chọn COD/VNPay/MoMo. Online mở màn hình Thanh toán demo local, chọn mô phỏng thành công hoặc thất bại.
4. Thử lỗi → Đơn mua → Thanh toán demo / thử lại, hoặc Hủy đơn để trả tồn kho.
5. Admin chuyển đơn lần lượt: xác nhận → xử lý → giao → đã giao. Online cần trả tiền; COD đánh dấu trả tiền khi đã giao.
6. Khách đánh giá đơn đã nhận; admin mở Đánh giá để trả lời.
7. Admin kiểm tra Tổng quan, Nhật ký, Email demo. Email chỉ được lưu trong database.

Phiên thanh toán 15 phút; hết hạn thì tạo lại/hủy. Không tự hủy đơn bỏ dở. Danh mục có con/sản phẩm cần chuyển tham chiếu trước khi xóa. Địa chỉ đã có đơn cần giữ lại.

## Dừng và chạy lại

```powershell
./scripts/stop-demo.ps1
./scripts/start-demo.ps1 -SkipBuild
```

Sau khi sửa Java, bỏ -SkipBuild để đóng gói lại. Script chỉ dừng các tiến trình của project được ghi nhận và container demo, giữ dữ liệu.

Database: container `ecommerce-java-demo-db`, user `demo`, password `demo_local_only`, database `ecommerce_demo`, port 55432.

## Xử lý lỗi

| Lỗi | Cách xử lý |
| --- | --- |
| Port đang dùng | Dừng demo cũ; kiểm tra tiến trình khác trước khi chạy lại |
| Không kết nối Docker | Mở Docker Desktop, chờ engine sẵn sàng |
| Java không build | Kiểm tra java -version là 21, dependency tải được |
| Thiếu Vite/dependency | Chạy npm ci trong từng frontend |
| Server không sẵn sàng | Xem .demo/server.log và server-error.log |
| Frontend lỗi | Xem .demo/client.log, admin.log và các file *-error.log |
| Phiên cũ không hợp lệ | Đăng nhập lại; đổi mật khẩu/đăng xuất/khóa tài khoản thu hồi phiên |
| Khóa file build Windows | Script dùng build/demo và .demo/app.jar, tránh ghi đè JAR đang chạy |
| Ảnh không hợp lệ | PNG/JPEG tối đa 5MB, lưu trong .demo/uploads |

.demo/, dependency, build và .env* được bỏ qua Git. Không xóa container để sửa lỗi nếu cần giữ dữ liệu.

## Kiểm tra lại

```powershell
./scripts/smoke-test.ps1
```

Smoke tạo fixture local. Lệnh integration test/build frontend có trong README. Báo cáo JUnit: server/ecommerce/build/demo/reports/tests/test/index.html.

Hướng dẫn dành cho demo Windows; chưa phải quy trình production hoặc chuyển database NestJS.
