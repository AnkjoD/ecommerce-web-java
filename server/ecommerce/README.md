# eCommerce Web - Spring Boot

## Yêu cầu hệ thống
- Java 21
- Gradle
- PostgreSQL 14+

## Cấu hình
Copy `.env.example` thành `.env` nếu chạy qua script hoặc Docker.
Nếu chạy trực tiếp, cấu hình trong `src/main/resources/application.properties`.

Cập nhật thông tin PostgreSQL, VNPay, Mail, JWT trong file config.

## Chạy dự án
```bash
./gradlew bootRun
```

Hoặc build ra file jar:
```bash
./gradlew build
java -jar build/libs/ecommerce-0.0.1-SNAPSHOT.jar
```

## API Docs
Hiện tại không sử dụng Swagger tự động. Cấu trúc route tương tự phiên bản NestJS.
Tiền tố chung: `/api`
Ví dụ: `POST /api/auth/login`
