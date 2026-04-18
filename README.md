# AuctionSpace - Client (Front-End Application) ⚡

## 1. Giới thiệu (Abstract)

Đây là kho lưu trữ mã nguồn phân hệ **Front-End (Client-side)** của dự án AuctionSpace - Nền tảng Đấu giá Trực tuyến (Phát triển bởi Nhóm 4, CN1 - UET VNU).

Dự án này tập trung xây dựng một ứng dụng Desktop hiện đại bằng **JavaFX**. Khác với các ứng dụng Java truyền thống, AuctionSpace Client áp dụng triệt để phong cách thiết kế **Glassmorphism** và **Dark Theme**, mang đến trải nghiệm UI/UX mượt mà, trực quan tương tự như các nền tảng Web3/Dashboard hiện đại.

## 2. Tính năng Giao diện Nổi bật (UI/UX Features)

- **Dark Mode & Neon Gradient:** Sử dụng dải màu tím/hồng đặc trưng trên nền tối sậm (`#0a0a0f`), tối ưu cho thị giác người dùng.
- **Responsive Layout:** Giao diện tự động co giãn (Flexible 50-50) trên mọi độ phân giải màn hình.
- **Bảo vệ Tràn viền (Overflow Protection):** Tích hợp ScrollPane ẩn, tự động kích hoạt thanh cuộn khi thu nhỏ cửa sổ, đảm bảo Form không bị bóp méo.
- **State Toggle mượt mà:** Chuyển đổi trạng thái Đăng nhập/Đăng ký ngay lập tức trên cùng một Scene thông qua quản lý Visibility trong Controller.

## 3. Công nghệ cốt lõi (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java 21 | Xử lý logic điều hướng và tương tác người dùng |
| **UI Framework**| JavaFX 21.0.2 | Dựng hình khối, Layout (VBox, HBox, StackPane) |
| **Styling** | JavaFX CSS | Xử lý đồ họa: Bo góc, đổ bóng (DropShadow), Gradient |
| **Build Tool** | Maven 3.x | Tự động tải thư viện và quản lý vòng đời dự án |

## 4. Hướng dẫn Môi trường & Khởi chạy (Setup & Run)

Dự án sử dụng Maven nên toàn bộ thư viện lõi (`javafx-controls`, `javafx-fxml`) sẽ được tự động tải về, loại bỏ rủi ro thiếu hụt thư viện nội bộ.

### Yêu cầu hệ thống:
- Java Development Kit (JDK) phiên bản 11 trở lên (Khuyến nghị JDK 21+).
- IntelliJ IDEA hoặc IDE có hỗ trợ Maven.

### Các bước khởi chạy:
1. **Clone mã nguồn:**
   ```bash
   git clone -b fe-javafx [https://github.com/roapskipper/TEAM4.git](https://github.com/roapskipper/TEAM4.git)
Tải thư viện (Sync):
Mở dự án bằng IntelliJ > Mở file pom.xml > Nhấn Ctrl + Shift + O (Load Maven Changes).

Khởi chạy (Quan trọng):
Chạy ứng dụng từ file Launcher.java (Bỏ qua file Main.java để vượt qua lỗi rào cản Module của Java 11+).

5. Cấu trúc Thư mục (Architecture)
   Phân hệ Front-End được thiết kế theo mô hình MVC (Model-View-Controller), tách biệt hoàn toàn Giao diện và Code xử lý:

```text
AuctionSystemClient/
├── pom.xml                     # Trái tim của Maven (Dependencies & Plugins)
└── src/
    └── main/
        ├── java/auctionsystemclient/
        │   ├── Main.java       # Khởi tạo kích thước Stage & nạp Scene
        │   ├── Launcher.java   # Mồi khởi chạy (Tránh lỗi runtime components)
        │   └── controller/     # [Controller] Bắt sự kiện (VD: LoginController.java)
        └── resources/
            ├── views/          # [View] Cấu trúc UI bằng FXML (login.fxml)
            └── styles/         # [Style] Giao diện toàn cục (global.css)

```
6. Lộ trình phát triển FE (Roadmap)
-[x] Sprint 1: Hoàn thiện Authentication UI (Login/Register) & Setup Maven.

-[ ] Sprint 2: Xây dựng Dashboard (Trang chủ) & Menu Sidebar.

-[ ] Sprint 3: Thiết kế Item Card (Thẻ sản phẩm) với dữ liệu Mocking.

-[ ] Sprint 4: Xây dựng Bidding Room (Phòng đấu giá chi tiết).

-[ ] Sprint 5: Ghép nối API/Socket với Back-End Server.