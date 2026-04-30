# AuctionSpace: Real-time Online Auction System 🚀

## Giới thiệu (Abstract)
**AuctionSpace** là hệ thống Đấu giá Trực tuyến hoạt động theo kiến trúc Client-Server, được phát triển hoàn toàn bằng Java 21. Hệ thống cung cấp trải nghiệm đấu giá thời gian thực (Real-time), hỗ trợ xử lý đồng thời (Concurrency) để tránh tranh chấp dữ liệu (Lost update, Race condition), và tích hợp các tính năng nâng cao như Đấu giá tự động (Auto-Bidding), Chống bắn tỉa (Anti-sniping) cùng biểu đồ biến động giá trực quan.

Dự án được phát triển bởi **Nhóm 4 - Lớp K70I-IT6** (Đại học Công nghệ - ĐHQGHN).

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
- **Quản lý phân quyền:** Đăng nhập/Đăng ký với 3 vai trò (Admin, Bidder, Seller).
- **Quản lý Phiên đấu giá (State Machine):** Tự động chuyển đổi trạng thái nghiêm ngặt `OPEN → RUNNING → FINISHED → PAID / CANCELED`.
- **Xử lý Ngoại lệ (Exception Handling):** Bắt lỗi chặt chẽ các trường hợp: Đặt giá thấp hơn giá hiện tại, bid khi phiên đã đóng, mất kết nối mạng đột ngột (Socket timeout).

### 2. Chức năng nâng cao (Advanced & Bonus Features)
- **Xử lý Đồng thời (Concurrency Bidding):** Giải quyết triệt để bài toán *Lost Update* và *Race Condition* khi hàng chục người cùng nhấn bid trong một phần nghìn giây. Đảm bảo tính toàn vẹn dữ liệu (Không ai bị trừ tiền oan, không có 2 người cùng thắng).
- **Auto-Bidding (Đấu giá tự động):** Người dùng thiết lập *maxBid* và *increment*, hệ thống dùng thuật toán (PriorityQueue) để tự động nâng giá tranh top.
- **Thuật toán Anti-sniping:** Tự động gia hạn thêm thời gian nếu phát hiện có bid đột ngột vào những giây cuối cùng.
- **Real-time Price Curve:** Biểu đồ đường (Line Chart) trực quan hóa lịch sử đặt giá, vẽ ngay lập tức mà không cần F5/Refresh.

---

## 🏗 Thiết kế Hướng đối tượng (OOP) & Kiến trúc

Hệ thống tuân thủ chặt chẽ 4 nguyên lý cốt lõi của OOP và sử dụng kiến trúc phân tầng MVC Client-Server.

### 1. Áp dụng 4 nguyên lý OOP
- **Đóng gói (Encapsulation):** Bảo vệ toàn bộ dữ liệu hệ thống (private/protected fields), chỉ cho phép truy cập qua getter/setter kiểm duyệt.
- **Kế thừa (Inheritance):** Thiết lập phân cấp đối tượng rõ ràng (Ví dụ: `Admin`, `Bidder`, `Seller` kế thừa từ `User`).
- **Đa hình (Polymorphism):** Ghi đè (Override) các phương thức như `getDetails()` hoặc `calculateFee()` tùy thuộc vào từng loại sản phẩm.
- **Trừu tượng (Abstraction):** Khai báo các Abstract Class (`Item`) và Interface làm bản thiết kế chuẩn cho 5 danh mục sản phẩm (Electronics, Art, Fashion, Collectibles, Vehicles).

### 2. Design Patterns áp dụng
- **Factory Method:** Khởi tạo động các loại Sản phẩm (`ItemFactory`) kết hợp linh hoạt với Dynamic Form trên giao diện JavaFX.
- **Observer Pattern:** Đồng bộ hóa giao diện đa người dùng. Server đóng vai trò Subject, đẩy (push) tín hiệu cập nhật giá qua Socket về các Client (Observers) mà không cần Polling gây nghẽn mạng.
- **Singleton Pattern:** Quản lý luồng kết nối duy nhất tới Database (DatabaseManager) và AuctionManager.

---

## Cấu trúc Dự án (Project Structure)

Hệ thống được phân chia module rõ ràng theo mô hình MVC và Client-Server:
```text
AuctionSystem/
├── src/main/java/com/team4/
│   ├── client/         # Xử lý giao tiếp mạng phía Client (ApiClient, Client)
│   ├── controller/     # Điều khiển giao diện JavaFX (Login, Main, Admin, Bidder...)
│   ├── db/             # Quản lý kết nối cơ sở dữ liệu (DatabaseManager - Singleton)
│   ├── factory/        # Triển khai Factory Pattern khởi tạo 5 loại sản phẩm
│   ├── model/          # Các thực thể dữ liệu cốt lõi (User, Item, Auction, AutoBidding...)
│   ├── network/        # Quản lý các gói tin và giao tiếp mạng chung
│   ├── observer/       # Triển khai Observer Pattern để cập nhật dữ liệu Real-time
│   ├── server/         # Lõi Server xử lý API và Socket (ApiServer, ClientHandler)
│   ├── service/        # Chứa luồng nghiệp vụ đấu giá chính (AuctionManager)
│   ├── util/           # Các lớp tiện ích hỗ trợ (UserSession, Hash...)
│   ├── Launcher.java   # Điểm khởi chạy ứng dụng (Bypass JavaFX module layer)
│   └── Main.java       # Cấu hình JavaFX Application
├── src/main/resources/com/team4/view/
│   ├── *.fxml          # Toàn bộ file thiết kế giao diện ứng dụng (XML)
│   └── style.css       # File định dạng CSS tĩnh cho toàn hệ thống
├── API_Contract.md     # Tài liệu đặc tả giao tiếp API giữa Client - Server
├── pom.xml             # File cấu hình thư viện và Dependencies của Maven
└── README.md           # Tài liệu tổng quan dự án
```

## 🛠 Tiêu chuẩn & Công nghệ (Technologies & Standards)

Hệ thống được thiết kế hướng tới tiêu chuẩn công nghiệp:
- **Giao tiếp:** Socket / REST API định dạng JSON. Chỉ Server có quyền thao tác với cơ sở dữ liệu.
- **Quản lý dự án:** Build tool `Maven`.
- **Chất lượng mã nguồn (Clean Code):** Tuân thủ tuyệt đối **Google Java Style Guide**.
- **Quản lý phiên bản:** Sử dụng quy chuẩn **Conventional Commits** trên Git, chia nhánh (branching) rõ ràng cho từng tính năng.
- **Kiểm thử & CI/CD:** Tích hợp **JUnit** để Unit Test các logic tính toán giá quan trọng, tự động hóa luồng test qua **GitHub Actions**.

---

## 📅 Các giai đoạn thực hiện dự án (Roadmap)

- [x] **Giai đoạn 1:** Phân tích, thiết kế OOP, Database và chọn công nghệ.
- [x] **Giai đoạn 2:** Xây dựng Backend, Server-Client truyền nhận JSON, cấu hình Singleton & Factory.
- [x] **Giai đoạn 3:** Phát triển GUI JavaFX (Form động), cập nhật giá Real-time bằng Observer.
- [ ] **Giai đoạn 4:** Xử lý Concurrency (Khóa luồng Lost update), Auto-Bidding, Anti-sniping và vẽ biểu đồ.
- [ ] **Giai đoạn 5:** Hoàn thiện Unit Test (JUnit), Refactoring mã nguồn theo chuẩn Google, đóng gói.

---

## 💻 Hướng dẫn Khởi chạy (Build & Run)

```bash
# 1. Clone dự án về máy
git clone [https://github.com/roapskipper/TEAM4.git](https://github.com/roapskipper/TEAM4.git)
cd TEAM4/AuctionSystem

# 2. Tải các thư viện phụ thuộc (Dependencies)
mvn clean install

# 3. Khởi chạy Client Application (Lưu ý quan trọng)
# BẮT BUỘC chạy từ file: src/main/java/com/team4/Launcher.java
# (Để bypass lỗi module layer của JavaFX trên JDK 11+)
```
## 🔐 Tài khoản Thử nghiệm (Demo Accounts)

Sử dụng các tài khoản dưới đây để truy cập Client Application và kiểm tra luồng phân quyền tương ứng:

| Vai trò | Tên đăng nhập | Mật khẩu | Chức năng chính trên giao diện |
| :--- | :--- | :--- | :--- |
| **Quản trị viên** | `admin` | `admin` | Xem tổng quan, quản lý người dùng, duyệt phiên đấu giá. |
| **Người bán** | `seller` | `seller` | Thêm sản phẩm, tạo và quản lý phiên đấu giá của mình. |
| **Người mua** | `bidder` | `bidder` | Tham gia phòng đấu giá, đặt giá, xem lịch sử và biểu đồ giá. |

> *(Lưu ý: Mật khẩu thực tế đã được mã hóa Hash an toàn trong Database. Client sẽ tự xử lý đối chiếu khi đăng nhập).*

---

## ⚠️ Lưu ý quan trọng (Troubleshooting)

> [!CAUTION]
> **Khắc phục lỗi khởi chạy:**
>
> 1. **File chạy chính:** BẮT BUỘC chạy ứng dụng từ file `src/main/java/com/team4/Launcher.java`.
>
> 2. **Lỗi Maven:** Nếu hệ thống báo lỗi không thể chạy được file `Launcher` hoặc `cannot find symbol`, nguyên nhân thường là do IntelliJ chưa nhận diện cấu hình thư viện.
>
> **Cách khắc phục:** > - Click chuột phải vào file `pom.xml` ở thư mục gốc.
> - Chọn **"Add as Maven Project"**.
> - Sau đó bấm icon **Reload All Maven Projects** ở tab Maven bên lề phải, đợi thanh tiến trình chạy xong rồi Run lại.