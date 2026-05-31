# AuctionSpace

Hệ thống đấu giá trực tuyến bằng JavaFX, MySQL, REST API và TCP Socket.

## 1. Mô Tả Bài Toán Và Phạm Vi Hệ Thống

AuctionSpace là hệ thống đấu giá trực tuyến dạng desktop app, xây dựng theo mô hình client-server. Hệ thống hỗ trợ ba nhóm người dùng chính: Bidder, Seller và Admin.

| Thành phần | Phạm vi |
| --- | --- |
| Bidder | Xem phiên đấu giá, vào phòng đấu giá, đặt giá, dùng Auto Bid, nạp tiền, xem sản phẩm đã thắng. |
| Seller | Đăng ký và quản lý sản phẩm đưa lên đấu giá. |
| Admin | Quản lý người dùng, duyệt hoặc từ chối phiên đấu giá, theo dõi dashboard hệ thống. |
| Server | Xử lý REST API, socket realtime cho đấu giá và kết nối MySQL. |

---

## 2. Công Nghệ, Môi Trường Chạy Và Yêu Cầu Cài Đặt

### Công Nghệ Sử Dụng

| Hạng mục | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21, FXML, CSS |
| Build tool | Maven |
| Database | MySQL 8 |
| Persistence | JDBC, HikariCP |
| API | Java HttpServer, Gson |
| Realtime | TCP Socket |
| Bảo mật | BCrypt |
| Test | JUnit 5, Mockito, H2 |

### Yêu Cầu Cài Đặt

| Yêu cầu | Phiên bản/Ghi chú |
| --- | --- |
| JDK | 21 |
| Maven | 3.8 trở lên |
| MySQL | 8 |
| Git | Bất kỳ bản ổn định |
| IDE | Khuyến nghị IntelliJ IDEA |

### Cấu Hình Môi Trường

Tạo file `.env` ở thư mục gốc repository theo mẫu `.env.example`:

```env
DB_URL=jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

SERVER_HOST=127.0.0.1
SERVER_PORT=18368
API_BASE_URL=http://localhost:8080/api/
```

Server sẽ tự tạo database và seed dữ liệu mẫu từ `schema.sql` khi khởi động.

---

## 3. Cấu Trúc Thư Mục Và Module Chính

```text
AuctionSystem/                         # Repository root
├── .env.example                        # Mẫu cấu hình môi trường
├── README.md                           # Tài liệu dự án
├── .github/                            # CI workflow
└── AuctionSystem/                      # Maven project chính
    ├── pom.xml                         # Maven config
    ├── API_Contract.md
    ├── docs/
    └── src/
        ├── main/
        │   ├── java/com/team4/
        │   │   ├── client/            # JavaFX API client, socket client
        │   │   ├── controller/        # JavaFX controllers
        │   │   ├── dao/               # DAO layer
        │   │   ├── db/                # Database setup
        │   │   ├── dto/               # Request/response DTO
        │   │   ├── factory/           # Item factory
        │   │   ├── handler/           # HTTP handlers
        │   │   ├── mapper/            # DTO mappers
        │   │   ├── model/             # Domain models
        │   │   ├── server/            # API server, socket server, scheduler
        │   │   ├── service/           # Business logic
        │   │   └── util/              # Utilities
        │   └── resources/
        │       ├── com/team4/view/    # FXML, CSS
        │       ├── fonts/
        │       ├── schema.sql
        │       ├── data.sql
        │       └── database.properties
        └── test/
            ├── java/com/team4/
            └── resources/
```

---

## 4. Câu Lệnh Build Và Chạy Chương Trình

Các lệnh dưới đây dùng được trên Windows PowerShell, Windows CMD, Linux và macOS nếu đã cài JDK 21, Maven và MySQL.

```bash
cd AuctionSystem
```

| Mục đích | Lệnh |
| --- | --- |
| Build nhanh không chạy test | `mvn -q -DskipTests compile` |
| Chạy unit test | `mvn test` |

---

## 5. Hướng Dẫn Chạy Server Và Client

### Bước 1: Khởi Động MySQL

Đảm bảo MySQL đang chạy và thông tin kết nối trong `.env` là đúng.

### Bước 2: Chạy Server

Mở terminal thứ nhất tại thư mục `AuctionSystem/` và chạy:

```bash
mvn exec:java "-Dexec.mainClass=com.team4.server.Server"
```

Server sẽ khởi động:

- Database setup.
- REST API tại `http://localhost:8080/api/`.
- Socket server tại `127.0.0.1:18368`.
- Scheduler đóng các phiên đấu giá đã hết hạn.

### Bước 3: Chạy Client

Mở terminal thứ hai tại thư mục `AuctionSystem/` và chạy:

```bash
mvn exec:java "-Dexec.mainClass=com.team4.Launcher"
```

Lưu ý: phải chạy Server trước Client để đăng nhập, tải dữ liệu và đấu giá realtime hoạt động đúng.

---

## 6. Danh Sách Chức Năng Đã Hoàn Thành

| Nhóm | Chức năng đã hoàn thành |
| --- | --- |
| Authentication | Đăng nhập theo role, đăng ký Bidder/Seller, Admin login bằng admin code, điều hướng theo role. |
| Bidder | Xem danh sách auction, lọc `Pending/Ongoing/Ended`, vào phòng đấu giá, đặt giá realtime, xem lịch sử bid, dùng Auto Bid, nạp tiền, xem sản phẩm đã thắng. |
| Seller | Xem dashboard, thêm/sửa/xóa sản phẩm, xem trạng thái sản phẩm/phiên đấu giá, không được phép đặt giá trong phòng đấu giá. |
| Admin | Xem dashboard, tìm kiếm người dùng, cấp/gỡ quyền Admin, duyệt/từ chối phiên đấu giá. |
| Hệ thống | REST API, socket realtime, tự động đóng phiên đấu giá hết hạn, giao diện tiếng Anh, đã loại bỏ chat. |

---

## 7. Link Báo Cáo PDF Và Video Demo

- Báo cáo PDF: [Link](https://drive.google.com/file/d/1c4JxOPbsrDUJ1ktARJrTAWva5ufelMSz/view?usp=sharing)
- Video demo: [Link](https://drive.google.com/drive/folders/12Kd0tb7F3i6XFL77C0Q31rxc4W0FyK8S?usp=sharing)
