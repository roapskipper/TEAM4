package com.team4;

import com.team4.factory.*;
import com.team4.model.*;
import com.team4.service.AuctionManager;
import java.time.LocalDateTime;

/**
 * LỚP KIỂM THỬ HỆ THỐNG TEAM4 - Đợt cuối trước khi lên Database
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("      HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - TEAM 4 - PHASE 2     ");
        System.out.println("   (Testing: Factory, Singleton, Observer, Polymorphism)  ");
        System.out.println("==========================================================\n");

        // 1. KHỞI TẠO HỆ THỐNG QUẢN LÝ (SINGLETON)
        AuctionManager manager = AuctionManager.getInstance();

        // 2. TẠO CÁC VAI TRÒ NGƯỜI DÙNG (INHERITANCE & UUID)
        Seller shopLuxury = new Seller("trung_luxury", "pass123", "Trung Luxury Center");
        Bidder bidderAn = new Bidder("an_dai_gia", "pass456", 50000.0, "123 Cầu Giấy", "0911");
        Bidder bidderBinh = new Bidder("binh_ngheo", "pass789", 500.0, "456 Giải Phóng", "0922");
        Admin adminHeThong = new Admin("admin_chu", "root123", 2, "G4-ROOT");

        System.out.println("--- [Users Created] ---");
        System.out.println(shopLuxury.toString());
        System.out.println(bidderAn.toString());
        System.out.println(bidderBinh.toString() + "\n");

        // 3. SẢN XUẤT HÀNG HÓA BẰNG NHÀ MÁY (FACTORY PATTERN)
        // Ta tạo một chiếc siêu xe Mercedes qua VehicleFactory
        ItemFactory xeFactory = new VehicleFactory(
                "Mercedes-Maybach S680", 25000.0, "Xe VIP nguyên bản 100%", shopLuxury.getId(),
                "Mercedes", "S680", 2024, 50, "Petrol", "Đen", "30H-999.99", true, "Auto"
        );
        Item sieuXe = xeFactory.createItem(); // Đa hình: trả về Item nhưng linh hồn là Vehicle

        // Ta tạo một cái túi hiệu qua FashionFactory
        ItemFactory tuiFactory = new FashionFactory(
                "Túi Hermès Kelly", 8000.0, "Hàng hiếm bản kỷ niệm", shopLuxury.getId(),
                "Hermès", "32", "Da Cá Sấu", "Vàng Chanh", "Nữ", "Like New", true
        );
        Item chiecTui = tuiFactory.createItem();

        System.out.println("--- [Inventory Produced by Factories] ---");
        sieuXe.showInfo();
        chiecTui.showInfo();

        // 4. THIẾT LẬP PHIÊN ĐẤU GIÁ (AUCTION SETUP)
        // Tạo phiên đấu giá cho xe hơi kết thúc sau 2 giờ nữa
        Auction xeAuction = new Auction(sieuXe.getId(), shopLuxury.getId(),
                sieuXe.getStartingPrice(), LocalDateTime.now().plusHours(2));
        manager.createAuction(xeAuction);

        System.out.println("\n--- [STARTING BUSINESS LOGIC TESTS] ---");

        // THỬ NGHIỆM 1: ĐẶT GIÁ THẤP HƠN GIÁ HIỆN TẠI
        System.out.println(">> TEST 1: Người dùng An đặt giá $20.000 (Thấp hơn giá khởi điểm $25.000)");
        manager.placeBid(bidderAn, xeAuction, 20000.0);

        // THỬ NGHIỆM 2: NGƯỜI DÙNG KHÔNG ĐỦ TIỀN (VÍ CÓ $500 MÀ ĐẶT $26.000)
        System.out.println("\n>> TEST 2: Người dùng Bình đặt giá $26.000 (Vượt quá số dư ví $500)");
        manager.placeBid(bidderBinh, xeAuction, 26000.0);

        // THỬ NGHIỆM 3: ADMIN KHÔNG ĐƯỢC PHÉP ĐẶT GIÁ (QUYỀN HẠN ROLE)
        System.out.println("\n>> TEST 3: Admin đặt giá thử (Sai vai trò - Role)");
        manager.placeBid(adminHeThong, xeAuction, 30000.0);

        // THỬ NGHIỆM 4: ĐẶT GIÁ THÀNH CÔNG
        System.out.println("\n>> TEST 4: Người dùng An đặt giá $30.000 (Thành công)");
        manager.placeBid(bidderAn, xeAuction, 30000.0);

        // THỬ NGHIỆM 5: ĐẶT GIÁ KHI PHIÊN ĐÃ HẾT HẠN (SIMULATION)
        System.out.println("\n>> TEST 5: Thử đặt giá cho một phiên đã bị khóa (CANCELLED)");
        xeAuction.setStatus("CANCELLED");
        manager.placeBid(bidderAn, xeAuction, 35000.0);

        // 5. HIỂN THỊ KẾT QUẢ CUỐI CÙNG
        System.out.println("\n==========================================================");
        System.out.println("          HỆ THỐNG KẾT THÚC KIỂM THỬ THÀNH CÔNG           ");
        System.out.println("        TÌNH TRẠNG PHIÊN CUỐI CÙNG: " + xeAuction.toString());
        System.out.println("==========================================================");
    }
}