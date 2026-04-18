# AuctionSpace - Client Application ⚡

## 1. Giới thiệu
Phát triển bởi: **Nguyễn Phúc Bình** - Nhóm 4 (CN1 - UET VNU)

**AuctionSpace** là nền tảng Đấu giá Trực tuyến. Đây là kho lưu trữ phân hệ Front-End, được xây dựng bằng **JavaFX 21** với phong cách thiết kế **Glassmorphism** và **Dark Theme** hiện đại, mang lại trải nghiệm UI/UX mượt mà như các ứng dụng Web3.

## 2. Công nghệ cốt lõi
| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java 21 | Xử lý logic điều hướng và tương tác |
| **UI Framework**| JavaFX 21.0.2 | Dựng hình khối, Layout (VBox, HBox...) |
| **Styling** | JavaFX CSS | Xử lý đồ họa: Bo góc, đổ bóng, Gradient |
| **Build Tool** | Maven 3.x | Quản lý thư viện và vòng đời dự án |

## 3. Cấu trúc Thư mục (Chuẩn com.team4)
Dự án áp dụng mô hình MVC, tách biệt hoàn toàn Giao diện và Code xử lý:

```text
AuctionSystem/
├── pom.xml                        # Quản lý thư viện Maven
└── src/main/
    ├── java/com/team4/            # Mã nguồn Java
    │   ├── Main.java              # Lớp khởi tạo Stage & nạp Scene
    │   ├── Launcher.java          # Lớp mồi khởi chạy (Bypass Module)
    │   ├── controller/            # [Controller] Điều khiển logic giao diện
    │   │   ├── LoginController.java
    │   │   └── MainController.java
    │   ├── model/                 # [Model] Các đối tượng dữ liệu
    │   └── util/                  # Các hàm tiện ích dùng chung
    └── resources/com/team4/view/  # [View] Tài nguyên giao diện
        ├── login.fxml             # Giao diện Đăng nhập/Đăng ký
        ├── main.fxml              # Giao diện Trang chủ (Dashboard)
        └── style.css              # File định dạng CSS toàn cục
```
## 4. Lộ trình phát triển (Roadmap)
-[x] Sprint 1: Authentication - Thiết kế giao diện Login/Register.

-[x] Sprint 2: Dashboard - Dựng khung Trang chủ & Menu Sidebar.

-[x] Sprint 3: Refactoring - Cấu trúc lại chuẩn com.team4 & Fix Git.

-[ ] Sprint 4: Bidding Room - Thiết kế Phòng đấu giá chi tiết (Đang thực hiện).

-[ ] Sprint 5: Integration - Kết nối Socket/API với Server của nhóm.

## 5. Hướng dẫn Khởi chạy (Dành cho Team)
   Clone & Mở dự án: Mở thư mục AuctionSystem bằng IntelliJ IDEA.

Nạp thư viện: Mở file pom.xml và nhấn Ctrl + Shift + O (Reload Maven).

Khởi chạy (Quan trọng): BẮT BUỘC chạy ứng dụng từ file Launcher.java để tránh lỗi thiếu Runtime Components của Java 11+.

Tài khoản Test hệ thống:

Username: testing_bidder

Password: 123456