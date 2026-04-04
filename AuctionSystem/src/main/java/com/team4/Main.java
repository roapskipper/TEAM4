package com.team4;

import com.team4.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp kiểm thử hệ thống người dùng TEAM4
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("        DỰ ÁN ĐẤU GIÁ TRỰC TUYẾN - TEAM 4         ");
        System.out.println("     KIỂM THỬ ĐA HÌNH & QUẢN LÝ NGƯỜI DÙNG         ");
        System.out.println("====================================================\n");

        // 1. Tạo danh sách Người dùng (Tính Polymorphism - Đa hình)
        // Một List kiểu User có thể chứa cả Admin, Seller và Bidder
        List<User> listNguoiDung = new ArrayList<>();

        // 2. Khởi tạo các vai trò cụ thể
        // Constructor tự động sinh UUID cho từng đối tượng
        Admin quanTriVien = new Admin("trung_admin", "admin_pass", 2, "ROOT-G4-99");

        Seller nguoiBan = new Seller("trung_pottery", "seller_pass", "Gốm Sứ Bát Tràng");
        nguoiBan.setFullName("Lê Trung (Chủ Shop)");

        Bidder nguoiMua = new Bidder("trung_bidder", "buyer_pass", 2500.0,
                "123 Cầu Giấy, Hà Nội", "0988776655");
        nguoiMua.setFullName("Nguyễn Văn A (Khách hàng)");

        // 3. Đưa tất cả vào danh sách chung (Tính Kế thừa - Inheritance)
        listNguoiDung.add(quanTriVien);
        listNguoiDung.add(nguoiBan);
        listNguoiDung.add(nguoiMua);

        // 4. Chạy vòng lặp kiểm tra Đa hình (Polymorphism)
        // Mỗi đối tượng sẽ tự gọi phương thức displayRolePermissions của riêng nó
        for (User u : listNguoiDung) {
            u.displayRolePermissions();
            // In ID để kiểm tra cơ chế UUID (Inheritance từ Entity)
            System.out.println("   [DEBUG ID]: " + u.getId());
        }

        // 5. Kiểm thử Logic riêng lẻ (Business Logic)
        System.out.println("\n--- [ KIỂM TRA LOGIC NGHIỆP VỤ ] ---");

        // Thử nạp thêm tiền cho Bidder (Sử dụng encapsulation)
        nguoiMua.addBalance(500.50);

        // Thử Admin thực hiện quyền khóa User
        quanTriVien.banUser(nguoiBan);

        // Thử thêm mặt hàng cho Seller
        nguoiBan.addNewItem("ITEM-99-POT-001");
        System.out.println("Số lượng hàng trong kho Seller: " + nguoiBan.getListedItemIds().size());

        System.out.println("\n====================================================");
        System.out.println("            TESTING COMPLETED SUCCESSFULLY!         ");
        System.out.println("====================================================");
    }
}