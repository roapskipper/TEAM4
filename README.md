# AuctionSpace

AuctionSpace là hệ thống đấu giá trực tuyến dùng JavaFX và MySQL, được xây dựng theo kiến trúc client-server. Phiên bản hiện tại sử dụng:

- JavaFX/FXML cho giao diện desktop.
- REST-style JSON API ở cổng `8080` cho đăng nhập, hồ sơ, sản phẩm, người dùng và dữ liệu đấu giá.
- TCP socket server ở cổng `18368` cho cập nhật đấu giá theo thời gian thực và forced logout.
- MySQL với HikariCP để lưu trữ dữ liệu.

Giao diện đi theo concept **The Heritage**: nền cream, bề mặt nội dung trắng, nút màu deep burgundy, chữ charcoal, tiêu đề dùng Playfair Display và nội dung dùng Lato.

## Tính Năng Hiện Có

### Xác Thực Và Phân Quyền

- Đăng nhập cho Admin, Seller và Bidder.
- Đăng ký tài khoản Bidder và Seller.
- Admin đăng nhập kèm admin code.
- Mỗi user chỉ có một socket session hoạt động: đăng nhập ở thiết bị khác sẽ buộc session cũ đăng xuất.

### Bidder

- Danh sách auction room lấy dữ liệu thật từ database qua `/api/auctions`.
- Có tìm kiếm, sắp xếp và lọc trạng thái auction theo `Pending`, `Ongoing`, `Ended`.
- Màn hình chi tiết đấu giá dùng lại UI bidding room hiện có.
- Chỉ cho đặt giá realtime qua socket server khi auction ở trạng thái `Ongoing`; auction `Pending` hoặc `Ended` vẫn xem được thông tin nhưng không thể bid.
- Hiển thị giá hiện tại, giá khởi điểm, bước giá, countdown, lịch sử bid, biểu đồ giá và số dư ví.
- Nạp tiền vào ví dành riêng cho Bidder; số dư được cập nhật lại trên UI sau khi nạp thành công.
- Có toast notification cho thông báo nhanh như nạp tiền thành công, lỗi nạp tiền hoặc chưa có thông báo mới.
- Hỗ trợ Auto Bid qua REST API cho từng auction.
- Service layer có logic proxy bidding và anti-sniping để gia hạn thời gian khi có bid sát giờ kết thúc.

### Seller

- Dashboard và bảng sản phẩm của seller.
- Chức năng đăng ký/thêm sản phẩm chỉ dành cho role Seller.
- Dialog thêm và sửa sản phẩm cho các category được hỗ trợ.
- Bảng sản phẩm hiển thị trạng thái theo `Pending`, `Ongoing`, `Ended`.
- Tải sản phẩm của seller qua `/api/seller/{sellerId}/items`.

### Admin

- Các màn dashboard cho admin.
- Danh sách và tìm kiếm người dùng qua `/api/admin/users`.
- Super Admin có thể cấp và gỡ quyền Admin cho tài khoản Seller/Bidder.
- Bảng quản lý phiên đấu giá qua `/api/admin/auctions`, có filter `all`, `pending`, `live`, `rejected`.
- Admin có thể approve hoặc reject auction đang chờ duyệt.
- UI hiển thị trạng thái auction theo `Pending`, `Ongoing`, `Ended`.

Lưu ý: hệ thống hiện không có luồng ban/suspend user end-to-end; README không liệt kê tính năng này để tránh lệch với code.

### Phạm Vi Đã Loại Bỏ

- Tính năng chat đã được loại bỏ khỏi app.

## Công Nghệ Sử Dụng

| Hạng mục | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21, FXML, CSS |
| Database | MySQL 8, `utf8mb4_unicode_ci` |
| Persistence | JDBC, HikariCP |
| API JSON | `com.sun.net.httpserver.HttpServer`, Gson |
| Realtime | TCP socket |
| Bảo mật | BCrypt password hashing, admin code verification |
| Build | Maven |
| Test | JUnit 5, Mockito, H2 cho test support |

## Cấu Trúc Dự Án

```text
.
├── .env.example
├── README.md
└── AuctionSystem/
    ├── pom.xml
    ├── API_Contract.md
    ├── docs/
    └── src/
        ├── main/
        │   ├── java/com/team4/
        │   │   ├── client/       # API client và socket client cho JavaFX
        │   │   ├── controller/   # JavaFX controllers
        │   │   ├── dao/          # DAO contracts và implementations
        │   │   ├── db/           # DatabaseManager và HikariCP setup
        │   │   ├── dto/          # Request/response DTOs
        │   │   ├── factory/      # Hỗ trợ khởi tạo item theo category
        │   │   ├── handler/      # HTTP API handlers
        │   │   ├── mapper/       # DTO mappers
        │   │   ├── model/        # Domain models
        │   │   ├── server/       # REST API server và socket server
        │   │   ├── service/      # Business logic
        │   │   └── util/         # Utility classes
        │   └── resources/
        │       ├── com/team4/view/   # FXML và CSS
        │       ├── fonts/
        │       ├── schema.sql
        │       ├── data.sql
        │       └── database.properties
        └── test/
            ├── java/com/team4/
            └── resources/
```

## Cấu Hình Môi Trường

Tạo file `.env` ở thư mục gốc repository từ `.env.example`.

```env
DB_URL=jdbc:mysql://localhost:3306/auction_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

SERVER_HOST=127.0.0.1
SERVER_PORT=18368
API_BASE_URL=http://localhost:8080/api/
```

Lưu ý: giữ `SERVER_PORT=18368` trừ khi bạn cũng sửa socket port đang hardcode trong `com.team4.server.Server`.

## Build Và Chạy

Yêu cầu:

- JDK 21
- Maven 3.8+
- MySQL 8 đang chạy local

Từ Maven project bên trong:

```bash
cd AuctionSystem
mvn -q -DskipTests compile
```

Chạy backend server trước. Class này sẽ khởi động database setup, REST API server, socket server và auction scheduler:

```bash
mvn exec:java -Dexec.mainClass="com.team4.server.Server"
```

Sau đó chạy JavaFX client ở terminal khác:

```bash
mvn exec:java -Dexec.mainClass="com.team4.Launcher"
```

Trong IntelliJ, chạy trực tiếp các class:

- Backend: `com.team4.server.Server`
- Client: `com.team4.Launcher`

Không chỉ chạy riêng `com.team4.server.ApiServer`, vì class này không tự khởi động socket server hoặc scheduler.

## Database

Server gọi `DatabaseSetup.initDatabase()` khi khởi động. Hàm này tạo database `auction_system` nếu cần, apply `schema.sql` và chạy một số migration nhẹ.

Dữ liệu mẫu nằm ở:

```text
AuctionSystem/src/main/resources/data.sql
```

Seed data hiện có:

- 1 Super Admin
- 3 Moderators
- 3 Sellers
- 5 Bidders
- 5 items
- 3 phiên đấu giá đang chạy và 2 phiên đang chờ duyệt

Nếu database đã có row cùng id, `INSERT IGNORE` sẽ giữ dữ liệu hiện tại.

## Tài Khoản Demo

| Role | Username | Password | Admin Code | Ghi chú |
| --- | --- | --- | --- | --- |
| Super Admin | `superadmin` | `admin123` | `adminteam4` | Quyền admin cao nhất |
| Moderator | `moderator1` | `mod123` | `mod-code-01` | Admin thường |
| Moderator | `moderator2` | `mod123` | `mod-code-02` | Admin thường |
| Moderator | `moderator3` | `mod123` | `mod-code-03` | Admin thường |
| Seller | `domixi` | `seller123` | - | Mixi Shop |
| Seller | `daucatmoi` | `seller123` | - | Premium Finds |
| Seller | `j.bieber` | `seller123` | - | JQK |
| Bidder | `ueteee` | `bidder123` | - | Số dư 10,000,000 VND |
| Bidder | `thichthimua` | `bidder123` | - | Số dư 100,000,000 VND |
| Bidder | `nguoimua3` | `bidder123` | - | Số dư 50,000,000 VND |
| Bidder | `sanhangdoc` | `bidder123` | - | Số dư 20,000,000 VND |
| Bidder | `svngheo` | `bidder123` | - | Số dư 2,000,000 VND |

## API Chính

| Method | Route | Mục đích |
| --- | --- | --- |
| `POST` | `/api/login` | Đăng nhập |
| `POST` | `/api/register/bidder` | Đăng ký bidder |
| `POST` | `/api/register/seller` | Đăng ký seller |
| `GET` | `/api/items` | Lấy danh sách item |
| `GET` | `/api/auctions` | Lấy danh sách auction |
| `GET` | `/api/auctions/{auctionId}` | Lấy chi tiết auction kèm lịch sử bid |
| `GET` | `/api/auctions/{auctionId}/highest-bid` | Lấy bid cao nhất của auction |
| `POST` | `/api/auctions/{auctionId}/autobid` | Bật Auto Bid cho bidder |
| `DELETE` | `/api/auctions/{auctionId}/autobid` | Tắt Auto Bid cho bidder |
| `GET` | `/api/auctions/{auctionId}/autobid?bidderId=...` | Lấy trạng thái Auto Bid hiện tại |
| `GET` | `/api/user/{userId}/profile` | Lấy hồ sơ |
| `PUT` | `/api/user/{userId}/profile` | Cập nhật hồ sơ |
| `PUT` | `/api/user/{userId}/password` | Đổi mật khẩu |
| `GET` | `/api/user/{userId}/owned-items` | Item đã sở hữu hoặc thắng đấu giá của bidder |
| `GET` | `/api/user/{userId}/bid-history` | Lịch sử bid của bidder |
| `GET` | `/api/user/{userId}/wallet/balance` | Lấy số dư ví |
| `POST` | `/api/user/{userId}/wallet/deposit` | Nạp tiền vào ví Bidder |
| `GET` | `/api/seller/{sellerId}/items` | Sản phẩm của seller |
| `GET` | `/api/seller/{sellerId}/stats` | Thống kê dashboard của seller |
| `GET` | `/api/admin/users` | Danh sách user cho admin |
| `GET` | `/api/admin/users/search?q=...` | Tìm kiếm user cho admin |
| `PUT` | `/api/admin/users/{userId}/grant-admin` | Super Admin cấp quyền Admin |
| `PUT` | `/api/admin/users/{userId}/revoke-admin` | Super Admin gỡ quyền Admin |
| `GET` | `/api/admin/auctions?filter=all|pending|live|rejected` | Danh sách auction cho admin |
| `PUT` | `/api/admin/auctions/{auctionId}/approve` | Duyệt auction |
| `PUT` | `/api/admin/auctions/{auctionId}/reject` | Từ chối auction |
| `GET` | `/api/admin/dashboard/stats` | Thống kê dashboard admin |

Các endpoint admin đọc `requesterId` từ query/body hoặc token Bearer nếu client gửi token. Socket bidding dùng JSON messages với các command như `LOGIN` và `BID`.

## Testing

Chạy unit tests:

```bash
cd AuctionSystem
mvn test
```

Compile nhanh không chạy test:

```bash
cd AuctionSystem
mvn -q -DskipTests compile
```

## Troubleshooting

- Nếu client không bid được, kiểm tra `com.team4.server.Server` đã chạy chưa và `.env` có `SERVER_PORT=18368` không.
- Nếu login không kết nối được API, kiểm tra cổng `8080` có đang rảnh không và `API_BASE_URL=http://localhost:8080/api/`.
- Nếu database startup lỗi, kiểm tra MySQL đang chạy và `.env` có `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` hợp lệ.
- Nếu font JavaFX không load, kiểm tra `AuctionSystem/src/main/resources/fonts/`.
- Nếu IntelliJ không nhận dependency, reload Maven project từ `AuctionSystem/pom.xml`.
