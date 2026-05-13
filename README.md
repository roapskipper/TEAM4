# AuctionSpace: Real-time Online Auction System

## Giới thiệu (Abstract)
**AuctionSpace** là hệ thống Đấu giá Trực tuyến hoạt động theo kiến trúc Client-Server, được phát triển hoàn toàn bằng Java 21 và JavaFX 17+. Hệ thống cung cấp trải nghiệm đấu giá thời gian thực (Real-time), hỗ trợ xử lý đồng thời (Concurrency) để tránh tranh chấp dữ liệu (Lost Update, Race Condition), và tích hợp các tính năng nâng cao như Đấu giá tự động (Auto-Bidding), Chống bắn tỉa (Anti-sniping) cùng biểu đồ biến động giá trực quan.

Giao diện được thiết kế theo concept **"The Heritage"** — phong cách truyền thống, sang trọng, lấy cảm hứng từ Sotheby's, với bảng màu Deep Burgundy (`#722F37`) trên nền Cream (`#FBF9F6`) và bộ font Playfair Display + Lato.

Dự án được phát triển bởi **Nhóm 4 - Lớp K70I-IT6** (Trường Đại học Công nghệ - Đại học Quốc gia Hà Nội).

---

## 👥 Thành viên & Phân công công việc (Team Roles)

| Thành viên | Vai trò chính | Nhiệm vụ chi tiết |
| :--- | :--- | :--- |
| **Hải Anh** | Backend & Architect | Thiết kế kiến trúc hệ thống, xử lý Concurrency (đấu giá đồng thời), triển khai Design Patterns chính (Singleton, Factory). |
| **Trung** | Database & Logic | Quản lý Database (DAO), viết logic quản lý User/Item, xử lý logic chuyển trạng thái và tính điểm thắng cuộc. |
| **Bình** | Frontend (GUI) | Phát triển toàn bộ giao diện JavaFX/FXML (Dynamic Form), xử lý hiển thị Real-time Price Curve (biểu đồ giá) và tích hợp Observer Client. |
| **Lộc** | Networking & QA | Xử lý kết nối Socket/API, cài đặt Auto-Bidding, Anti-sniping, viết Unit Test (JUnit) và thiết lập CI/CD trên GitHub Actions. |

---

## 🌟 Tính năng nổi bật & Nghiệp vụ hệ thống (Key Features)

Hệ thống đáp ứng toàn bộ các chức năng bắt buộc và tích hợp tối đa các chức năng nâng cao theo yêu cầu của bài toán:

### 1. Chức năng cốt lõi (Core Functions)
- **Quản lý phân quyền 4 cấp:** Đăng nhập/Đăng ký với 4 vai trò:
  - **Super Admin:** Toàn quyền hệ thống, cấp mã admin code cho Admin thường.
  - **Admin thường:** Cấm tài khoản, đánh dấu sản phẩm vi phạm (cần nhập admin code khi đăng nhập).
  - **Seller:** Đăng sản phẩm, tạo phiên đấu giá.
  - **Bidder:** Tham gia đấu giá, đặt bid.
- **Quản lý Phiên đấu giá (State Machine):** Tự động chuyển đổi trạng thái nghiêm ngặt `PENDING → RUNNING → FINISHED → PAID / CANCELLED`.
- **Xử lý Ngoại lệ (Exception Handling):** Bắt lỗi chặt chẽ các trường hợp: đặt giá thấp hơn giá hiện tại, bid khi phiên đã đóng, mất kết nối mạng đột ngột (Socket timeout).

### 2. Chức năng nâng cao (Advanced & Bonus Features)
- **Xử lý Đồng thời (Concurrency Bidding):** Giải quyết triệt để bài toán *Lost Update* và *Race Condition* khi hàng chục người cùng nhấn bid trong một phần nghìn giây. Đảm bảo tính toàn vẹn dữ liệu (không ai bị trừ tiền oan, không có 2 người cùng thắng).
- **Auto-Bidding (Đấu giá tự động):** Người dùng thiết lập *maxBid* và *increment*, hệ thống dùng thuật toán (PriorityQueue) để tự động nâng giá tranh top.
- **Thuật toán Anti-sniping:** Tự động gia hạn thêm thời gian nếu phát hiện có bid đột ngột vào những giây cuối cùng.
- **Real-time Price Curve:** Biểu đồ đường (Line Chart) trực quan hóa lịch sử đặt giá, vẽ ngay lập tức mà không cần F5/Refresh.

### 3. Quy tắc Giới hạn Đặt giá (Bid Cap Rule)

Để chống tình trạng "thổi giá" phi lý hoặc cố ý bid sai (ví dụ: nhảy từ 1 triệu lên 500 triệu trong 1 bid), hệ thống áp dụng **giới hạn theo bậc thang** dựa trên giá hiện tại (`x`) của phiên đấu giá. Mức bid tối đa được phép = `multiplier × x`, trong đó hệ số `multiplier` giảm dần theo giá:

| Khoảng giá hiện tại (x) | Hệ số tối đa | Bid tối đa được phép |
| :--- | :---: | :--- |
| `0 < x < 1.000.000` | **5x** | gấp 5 lần giá hiện tại |
| `1.000.000 ≤ x < 5.000.000` | **4x** | gấp 4 lần giá hiện tại |
| `5.000.000 ≤ x < 25.000.000` | **3x** | gấp 3 lần giá hiện tại |
| `25.000.000 ≤ x < 50.000.000` | **2x** | gấp 2 lần giá hiện tại |
| `50.000.000 ≤ x < 100.000.000` | **1.8x** | gấp 1.8 lần giá hiện tại |
| `100.000.000 ≤ x < 250.000.000` | **1.5x** | gấp 1.5 lần giá hiện tại |
| `250.000.000 ≤ x ≤ 500.000.000` | **1.3x** | gấp 1.3 lần giá hiện tại |

> **Trần tuyệt đối:** Mọi bid không được vượt quá **500.000.000 VND** trong bất kỳ trường hợp nào.

**Cơ sở thiết kế:**
- **Càng đắt, càng siết chặt:** Với item rẻ (< 1tr), người dùng có thể bid mạnh tay (5x) để cạnh tranh nhanh. Với item đắt (> 250tr), hệ số giảm xuống 1.3x vì chênh lệch tuyệt đối đã rất lớn — không cần multiplier cao.
- **Chống "bid trolling":** Người dùng không thể "phá game" bằng cách bid quá cao để dằn mặt người khác.
- **Bảo vệ tâm lý người mua:** Tránh tình trạng FOMO khi thấy giá nhảy đột biến phi lý.

Quy tắc được áp dụng nhất quán cho cả **manual bid** và **auto-bid max limit** — người dùng không thể thiết lập `maxBid` cho hệ thống tự động vượt quá giới hạn này.

---

## 🏗 Thiết kế Hướng đối tượng (OOP) & Kiến trúc

Hệ thống tuân thủ chặt chẽ 4 nguyên lý cốt lõi của OOP và sử dụng kiến trúc phân tầng MVC Client-Server.

### 1. Áp dụng 4 nguyên lý OOP
- **Đóng gói (Encapsulation):** Bảo vệ toàn bộ dữ liệu hệ thống (private/protected fields), chỉ cho phép truy cập qua getter/setter kiểm duyệt.
- **Kế thừa (Inheritance):** Thiết lập phân cấp đối tượng rõ ràng (ví dụ: `Admin`, `Bidder`, `Seller` kế thừa từ `User`).
- **Đa hình (Polymorphism):** Ghi đè (Override) các phương thức như `getDetails()` hoặc `calculateFee()` tùy thuộc vào từng loại sản phẩm.
- **Trừu tượng (Abstraction):** Khai báo các Abstract Class (`Item`) và Interface làm bản thiết kế chuẩn cho 5 danh mục sản phẩm (Electronics, Art, Fashion, Collectibles, Vehicles).

### 2. Design Patterns áp dụng
- **Factory Method:** Khởi tạo động các loại Sản phẩm (`ItemFactory`) kết hợp linh hoạt với Dynamic Form trên giao diện JavaFX.
- **Observer Pattern:** Đồng bộ hóa giao diện đa người dùng. Server đóng vai trò Subject, đẩy (push) tín hiệu cập nhật giá qua Socket về các Client (Observers) mà không cần Polling gây nghẽn mạng.
- **Singleton Pattern:** Quản lý luồng kết nối duy nhất tới Database (`DatabaseManager`) và `AuctionManager`.

---

## 🎨 Thiết kế Giao diện (UI/UX)

Giao diện áp dụng concept **The Heritage** với các nguyên tắc thiết kế nhất quán toàn hệ thống:

| Thành phần | Đặc tả |
| :--- | :--- |
| **Concept** | Truyền thống, giàu có, lâu đời (Sotheby's-inspired) |
| **Nền chính** | Cream `#FBF9F6` |
| **Thẻ / Card** | Pure White `#FFFFFF` |
| **Màu nhấn / Button** | Deep Burgundy `#722F37` |
| **Chữ** | Charcoal `#2C2C2C` |
| **Font tiêu đề** | Playfair Display |
| **Font nội dung** | Lato |
| **Hiệu ứng card** | Soft drop shadow: `dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 4)` |
| **Bidding timer** | Sử dụng monospace font (tabular numerals) để countdown không bị giật ngang |

---

## 📂 Cấu trúc Dự án (Project Structure)

Hệ thống được phân chia module rõ ràng theo mô hình MVC và Client-Server:

```text
AuctionSystem/
├── src/
│   ├── main/
│   │   ├── java/com/team4/
│   │   │   ├── client/             # Giao tiếp mạng phía Client (ApiClient, Client)
│   │   │   ├── controller/         # Controller cho các màn JavaFX
│   │   │   ├── dao/                # Data Access Object — truy vấn Database
│   │   │   ├── db/                 # Quản lý kết nối DB (DatabaseManager - Singleton)
│   │   │   ├── factory/            # Factory Pattern khởi tạo 5 loại sản phẩm
│   │   │   ├── handler/            # Xử lý request/response Client - Server
│   │   │   ├── model/              # Các thực thể (User, Item, Auction, AutoBidding...)
│   │   │   ├── network/            # Quản lý gói tin và giao tiếp mạng chung
│   │   │   ├── observer/           # Observer Pattern cho cập nhật Real-time
│   │   │   ├── server/             # Lõi Server xử lý API và Socket
│   │   │   ├── service/            # Luồng nghiệp vụ chính (AuctionManager)
│   │   │   ├── util/               # Tiện ích (UserSession, Hash...)
│   │   │   ├── Launcher.java       # Điểm khởi chạy ứng dụng (Bypass JavaFX module layer)
│   │   │   ├── Main.java           # Cấu hình JavaFX Application
│   │   │   └── TestConnect.java    # Test nhanh kết nối DB
│   │   └── resources/
│   │       ├── com/team4/view/
│   │       │   ├── *.fxml          #Các file .fxml
│   │       │   └── style.css       # CSS theo concept The Heritage
│   │       ├── fonts/              # Font Playfair Display & Lato (TTF)
│   │       ├── data.sql            # Dữ liệu khởi tạo mẫu
│   │       ├── schema.sql          # Định nghĩa cấu trúc Database (MySQL)
│   │       ├── database.properties # Cấu hình kết nối DB
│   │       └── logback.xml         # Cấu hình logging
│   └── test/                       # Unit Test (JUnit)
├── target/                         # Output build của Maven (auto-generated)
├── KeHoachTrienKhaiBackend_AuctionSystem.pdf   # Tài liệu kế hoạch Backend
├── API_Contract.md                 # Đặc tả giao tiếp API Client - Server
├── Query.sql                       # Các câu truy vấn mẫu / debug
├── .env.example                    # Mẫu file biến môi trường
├── .gitignore
├── pom.xml                         # Cấu hình Maven & Dependencies
└── README.md                       # Tài liệu tổng quan dự án
```

---

## 🛠 Tiêu chuẩn & Công nghệ (Technologies & Standards)

Hệ thống được thiết kế hướng tới tiêu chuẩn công nghiệp:

| Hạng mục | Công nghệ / Tiêu chuẩn |
| :--- | :--- |
| **Ngôn ngữ** | Java 21 |
| **GUI Framework** | JavaFX 17+ (FXML, CSS) |
| **Database** | MySQL 8.0 (charset `utf8mb4_unicode_ci`) |
| **Giao tiếp mạng** | Socket TCP (real-time) + REST API JSON (CRUD) |
| **Build Tool** | Apache Maven |
| **Coding Standard** | Google Java Style Guide |
| **Version Control** | Git — Conventional Commits, branching theo feature |
| **Testing** | JUnit 5 |
| **CI/CD** | GitHub Actions |

> **Nguyên tắc bảo mật:** Chỉ Server có quyền thao tác trực tiếp với cơ sở dữ liệu. Client luôn giao tiếp gián tiếp qua API. Mật khẩu được hash bằng BCrypt trước khi lưu.

---

## 📅 Roadmap & Tiến trình hiện tại

- [x] **Giai đoạn 1:** Phân tích yêu cầu, thiết kế OOP, Database schema và chọn công nghệ.
- [x] **Giai đoạn 2:** Xây dựng Backend cơ bản, Server-Client truyền nhận JSON, cấu hình Singleton & Factory.
- [x] **Giai đoạn 3:** Phát triển GUI JavaFX hoàn chỉnh — đăng nhập/đăng ký 4 vai trò, dashboard cho từng role, phòng đấu giá, profile, áp dụng concept The Heritage và responsive layout.
- [ ] **Giai đoạn 4 (Đang thực hiện):** Xử lý Concurrency (khóa luồng chống Lost Update), Auto-Bidding, Anti-sniping, cập nhật biểu đồ giá Real-time qua Observer Pattern.
- [ ] **Giai đoạn 5:** Hoàn thiện Unit Test (JUnit), Refactoring mã nguồn theo Google Style, đóng gói và viết tài liệu cuối cùng.

---

## 💻 Hướng dẫn Khởi chạy (Build & Run)

### Yêu cầu môi trường
- **JDK 21** (hoặc cao hơn)
- **Maven 3.8+**
- **MySQL 8.0** đang chạy ở `localhost:3306`

### Các bước

```bash
# 1. Clone dự án về máy
git clone https://github.com/roapskipper/TEAM4.git
cd TEAM4/AuctionSystem

# 2. Cấu hình biến môi trường
cp .env.example .env
# Mở .env và điền DB_USER, DB_PASSWORD theo MySQL local của bạn

# 3. Tạo database và import schema + data mẫu
mysql -u root -p < src/main/resources/schema.sql
mysql -u root -p auction_system < src/main/resources/data.sql

# 4. Cài đặt dependencies
mvn clean install

# 5. Chạy Server (terminal 1)
mvn exec:java -Dexec.mainClass="com.team4.server.ApiServer"

# 6. Chạy Client Application (terminal 2)
# BẮT BUỘC chạy từ file Launcher.java để bypass lỗi module layer của JavaFX trên JDK 11+
mvn exec:java -Dexec.mainClass="com.team4.Launcher"
```

---

## 🔐 Tài khoản Thử nghiệm (Demo Accounts)

Sử dụng các tài khoản dưới đây để truy cập Client Application và kiểm tra luồng phân quyền tương ứng:

| Vai trò                | Username          | Password          | Admin Code        | Chức năng chính                                                                                        |
|:-----------------------|:------------------|:------------------|:------------------|:-------------------------------------------------------------------------------------------------------|
| **Super Admin**        | `superadmin`      | `admin123`        | `adminteam4`      | Quản lý toàn hệ thống, cấp mã cho Admin thường.                                                        |
| **Admin**              | *(Chưa cập nhật)* | *(Chưa cập nhật)* | *(Chưa cập nhật)* | Có ít chức năng hơn super admin, chỉ có những chức năng cơ bản như báo vi phạm, khóa tài khoản,...
| **Seller (Mixi Shop)** | `domixi`          | `seller123`       | —                 | Đăng sản phẩm, tạo và quản lý phiên đấu giá của mình.                                                  |
| **Seller (Shop Chất)** | `daucatmoi`       | `seller123`       | —                 | Tương tự trên — dùng để test multi-seller.                                                             |
| **Bidder (10 triệu)**  | `ueteee`          | `bidder123`       | —                 | Tham gia đấu giá với balance thấp — test logic không đủ tiền.                                          |
| **Bidder (100 triệu)** | `thichthimua`     | `bidder123`       | —                 | Tham gia đấu giá với balance cao — test luồng đặt bid bình thường.                                     |

> *(Lưu ý: mật khẩu thực được lưu dưới dạng BCrypt hash trong cột `password_hash`. Server tự xử lý đối chiếu khi đăng nhập.)*

---

## ⚠️ Lưu ý quan trọng (Troubleshooting)

> [!CAUTION]
> **1. File chạy chính:** BẮT BUỘC chạy ứng dụng từ `src/main/java/com/team4/Launcher.java`, không phải `Main.java`. `Launcher` tồn tại để bypass lỗi module layer của JavaFX trên JDK 11+.
>
> **2. Lỗi Maven (`cannot find symbol`, không nhận diện thư viện):**
> Nguyên nhân thường do IntelliJ chưa nhận cấu hình Maven.
>
> Cách khắc phục:
> - Chuột phải vào `pom.xml` ở thư mục gốc → chọn **"Add as Maven Project"**.
> - Vào tab Maven bên lề phải → bấm icon **Reload All Maven Projects**.
> - Đợi tiến trình chạy xong → Run lại `Launcher`.
>
> **3. Lỗi Database (`Access denied` hoặc `Communications link failure`):**
> - Kiểm tra MySQL đang chạy ở port `3306`.
> - Đảm bảo đã import cả `schema.sql` và `data.sql`.
> - Kiểm tra thông tin kết nối trong `src/main/resources/database.properties` (username, password, database name `auction_system`).
>
> **4. Lỗi Font không load:**
> Nếu thấy log `[Main] Font not found`, kiểm tra folder `src/main/resources/fonts/` đã có đủ các file `.ttf` của Playfair Display và Lato.
