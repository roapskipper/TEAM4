# AuctionSpace: Real-time Online Auction System 🚀

## Giới thiệu (Abstract)
**AuctionSpace** là hệ thống Đấu giá Trực tuyến hoạt động theo kiến trúc Client-Server, được phát triển hoàn toàn bằng Java 21. Hệ thống cung cấp trải nghiệm đấu giá thời gian thực (Real-time), hỗ trợ xử lý đồng thời (Concurrency) để tránh tranh chấp dữ liệu, và tích hợp các tính năng nâng cao như Đấu giá tự động (Auto-Bidding) cùng biểu đồ biến động giá trực quan.

Dự án được phát triển bởi **Nhóm 4 - Lớp K70I-IT6** (Đại học Công nghệ - ĐHQGHN).

---

## 👥 Thành viên & Phân công công việc (Team Roles)

| Thành viên | Vai trò chính | Nhiệm vụ chi tiết |
| :--- | :--- | :--- |
| **Hải Anh** | Backend & Architect | Thiết kế kiến trúc hệ thống, xử lý Concurrency (đấu giá đồng thời), triển khai Design Patterns chính (Singleton, Factory). |
| **Trung** | Database & Logic | Quản lý Database (DAO), viết logic nghiệp vụ cho User/Item, xử lý logic kết thúc phiên đấu giá và tính điểm thắng cuộc. |
| **Bình** | Frontend (GUI) | Phát triển toàn bộ giao diện JavaFX/FXML (Dynamic Form), xử lý hiển thị Real-time Price Curve (biểu đồ giá) và tích hợp Observer Client. |
| **Lộc** | Networking & QA | Xử lý kết nối Socket/API, cài đặt Auto-Bidding, viết Unit Test (JUnit) và thiết lập CI/CD trên GitHub Actions. |

---

## 🏗 Kiến trúc & Các Class cốt lõi (Core Architecture)

Hệ thống tuân thủ chặt chẽ các nguyên lý hướng đối tượng (OOP) và sử dụng kiến trúc MVC kết hợp Client-Server. Dựa trên sơ đồ cơ sở dữ liệu và yêu cầu giao diện, các Class chính được thiết kế như sau:

1. **Nhóm User (Người dùng):**
    - Class cha `User` phân nhánh thành 3 vai trò: `Admin`, `Bidder`, và `Seller`.
    - *Tích hợp FE/BE:* Giao diện đăng ký sử dụng ChoiceBox để người dùng chọn vai trò, sau đó luồng logic sẽ định tuyến lưu dữ liệu vào bảng tương ứng.

2. **Nhóm Item (Sản phẩm) - Áp dụng Factory Pattern:**
    - Class cha `Item` và 5 class con kế thừa: `Electronics`, `Art`, `Fashion`, `Collectibles`, `Vehicles`.
    - *Tích hợp FE/BE:* Phía Frontend xử lý **Form động (Dynamic Form)** (thay đổi trường nhập liệu theo Category). Phía Backend dùng **Factory Pattern** để tự động khởi tạo đúng đối tượng sản phẩm và lưu vào các bảng con tương ứng.

3. **Nhóm Auction & Giao dịch:**
    - `Auction`: Quản lý phiên đấu giá (start_time, end_time, current_price, status).
    - `BidTransaction`: Lưu trữ lịch sử đặt giá. Dữ liệu này được đẩy qua Socket để Frontend vẽ biểu đồ Line Chart theo thời gian thực.
    - `AutoBidding`: Chứa cấu hình tự động đặt giá (`max_bid`, `increment_amount`).

---

## 📅 Các giai đoạn thực hiện dự án (Roadmap)

- [x] **Giai đoạn 1: Phân tích và Thiết kế hệ thống (Tuần 1)**
    - Thiết kế lớp (OOP): `User`, `Item`, `Auction`, `BidTransaction`.
    - Thiết kế Cơ sở dữ liệu và chọn công nghệ (Maven, JavaFX, Socket/REST API).
- [x] **Giai đoạn 2: Xây dựng nền tảng Backend & Networking (Tuần 2-3)**
    - Thiết lập Server-Client truyền nhận JSON.
    - Áp dụng **Singleton** cho Database Manager và **Factory** cho khởi tạo sản phẩm.
- [x] **Giai đoạn 3: Phát triển GUI & Logic Đấu giá (Tuần 4-5)**
    - Thiết kế màn hình JavaFX, ghép nối form động.
    - Cập nhật giá Real-time thông qua **Observer Pattern**.
- [ ] **Giai đoạn 4: Xử lý nâng cao & Concurrency (Tuần 6-7)**
    - Xử lý tranh chấp (Race condition) khi nhiều người bid cùng lúc (Lost update).
    - Cài đặt thuật toán Auto-Bidding, Anti-sniping và vẽ biểu đồ biến động giá.
- [ ] **Giai đoạn 5: Kiểm thử, Tối ưu & Hoàn thiện (Tuần 8)**
    - Viết Unit Test bằng JUnit cho logic tính toán giá.
    - Refactoring mã nguồn theo Google Java Style Guide và đóng gói Demo.

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
---

## 🔐 Tài khoản Thử nghiệm (Demo Accounts)

Để thuận tiện cho việc kiểm thử và đánh giá hệ thống, nhóm cung cấp sẵn tài khoản test với thông tin như sau:

> **Vai trò:** Người đấu giá (Bidder)
> - **Tên đăng nhập:** `tester_bidder`
> - **Mật khẩu:** `123456`

*(Lưu ý: Mật khẩu đã được mã hóa trong Database, vui lòng sử dụng đúng thông tin trên để đăng nhập qua giao diện Client).*

> ⚠️ **Lưu ý quan trọng (Troubleshooting):**
> Nếu hệ thống báo lỗi không thể chạy được file `Launcher`, nguyên nhân thường là do IntelliJ chưa nhận diện cấu hình thư viện. Hãy kiểm tra xem file `pom.xml` đã có biểu tượng chữ **M** (Maven) màu xanh chưa. Nếu chưa, hãy làm theo 3 bước sau:
> 1. Click chuột phải vào file **`pom.xml`**.
> 2. Chọn **"Add as Maven Project"** (hoặc "Add Maven Projects").
> 3. Đợi thanh tiến trình của IntelliJ chạy xong ở góc dưới bên phải, sau đó bấm Run lại.
>
> 🚨 *Nếu đã làm các bước trên mà vẫn gặp bất kỳ lỗi nào khác (đỏ file, lỗi SDK,...), vui lòng chụp ảnh màn hình lỗi và gửi vào group chat để các thành viên khác hỗ trợ xử lý ngay!*