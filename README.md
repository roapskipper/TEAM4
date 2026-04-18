# AuctionSpace - Client Application ⚡

## 1. Giới thiệu
Phát triển bởi: **Nguyễn Phúc Bình** - Nhóm 4 (K70I - IT6)
Ứng dụng Đấu giá Trực tuyến viết bằng **JavaFX 21** với giao diện **Dark Theme**.

## 2. Cấu trúc Thư mục (Mới nhất)
Đây là cấu trúc chuẩn sau khi đã tái cấu trúc sang package `com.team4`:

```text
AuctionSystemClient/
├── pom.xml                        # Quản lý thư viện Maven
└── src/main/
    ├── java/com/team4/            # Mã nguồn Java
    │   ├── Main.java              # Lớp khởi tạo Stage
    │   ├── Launcher.java          # Lớp chạy ứng dụng (Main Entry)
    │   ├── controller/            # Xử lý logic giao diện
    │   │   ├── LoginController.java
    │   │   └── MainController.java
    │   ├── model/                 # Các đối tượng dữ liệu (User, Product...)
    │   └── util/                  # Các hàm tiện ích bổ trợ
    └── resources/com/team4/view/  # Tài nguyên giao diện
        ├── login.fxml             # Giao diện Đăng nhập
        ├── main.fxml              # Giao diện Trang chủ
        └── style.css              # File định dạng CSS toàn cục
```

## 3. Lộ trình phát triển (Roadmap)
Dưới đây là tiến độ thực hiện các phân hệ Front-End:

- [x] **Sprint 1: Authentication**
   - Thiết kế giao diện Login/Register (Glassmorphism).
   - Setup cấu hình Maven và JavaFX 21.
- [x] **Sprint 2: Dashboard**
   - Dựng khung Trang chủ (Main View).
   - Thiết kế Sidebar điều hướng mượt mà.
- [x] **Sprint 3: Refactoring**
   - Chuyển đổi package sang `com.team4`.
   - Cấu trúc lại Resource và fix lỗi Runtime.
- [ ] **Sprint 4: Bidding Room (Current)**
   - Thiết kế phòng đấu giá chi tiết.
   - Hiển thị danh sách người tham gia và lịch sử giá.
- [ ] **Sprint 5: Integration**
   - Kết nối Socket/API với Server của nhóm.

## 4. Hướng dẫn chạy
1. Mở dự án trong IntelliJ.
2. Nhấn `Ctrl + Shift + O` để nạp Maven.
3. Chuột phải vào file `Launcher.java` chọn **Run**.
## 5. Tài khoản dùng thử (Mock Data)
Tài khoản: testing_bidder

Mật khẩu: 123456