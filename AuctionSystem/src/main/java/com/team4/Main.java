package com.team4;

import com.team4.dao.impl.*;
import com.team4.factory.*;
import com.team4.model.*;
import com.team4.service.AuctionManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("      HỆ THỐNG ĐẤU GIÁ TEAM 4 - TỔNG NGHIỆM THU          ");
        System.out.println("        (Database + Factory + DAO + Logic)               ");
        System.out.println("==========================================================\n");

        // 1. KHỞI TẠO TẦNG DAO & SERVICE
        UserDAOImpl userDAO = new UserDAOImpl();
        ItemDAOImpl itemDAO = new ItemDAOImpl();
        AuctionDAOImpl auctionDAO = new AuctionDAOImpl();
        BidTransactionDAOImpl bidDAO = new BidTransactionDAOImpl();
        AuctionManager auctionManager = AuctionManager.getInstance();

        // 2. KHỞI TẠO NGƯỜI CHƠI (Persistent Users)
        System.out.println("[KỊCH BẢN] Bước 1: Khởi tạo Seller và Bidder...");
        Seller mercedesHanoi = new Seller("mercedes_hn", "pass123", "Mercedes Vietnam Center");
        Bidder cuongDollar = new Bidder("cuong_dollar", "pass456", 2000000.0, "TP.HCM", "09001");
        Bidder tungMtp = new Bidder("tung_mtp", "pass789", 1500000.0, "Thái Bình", "09002");

        userDAO.save(mercedesHanoi);
        userDAO.save(cuongDollar);
        userDAO.save(tungMtp);
        System.out.println(">> Đã lưu các tài khoản vào Database.\n");

        // 3. SẢN XUẤT VẬT PHẨM QUA NHÀ MÁY (Factory Design Pattern)
        System.out.println("[KỊCH BẢN] Bước 2: Sản xuất siêu xe đấu giá...");
        ItemFactory factory = new VehicleFactory(
                "Mercedes Maybach S680", 250000.0, "Xe đời mới nhất 2025", mercedesHanoi.getId(),
                "Mercedes", "Maybach S680", 2025, 0, "V12", "Trắng Obsidian", "UN-9999", true, "Auto"
        );
        Item sieuXe = factory.createItem();
        itemDAO.save(sieuXe);
        System.out.println(">> Sản phẩm: " + sieuXe.getName() + " đã lên kệ hàng.\n");

        // 4. MỞ PHIÊN ĐẤU GIÁ (Start Auction)
        System.out.println("[KỊCH BẢN] Bước 3: Thiết lập sàn đấu giá cho xe...");
        Auction auction = new Auction(sieuXe.getId(), mercedesHanoi.getId(),
                sieuXe.getStartingPrice(), LocalDateTime.now().plusHours(1));
        auctionDAO.save(auction);
        auctionManager.createAuction(auction);

        // 5. CÔNG CUỘC TRẢ GIÁ KỊCH TÍNH (Business Logic & Transactions)
        System.out.println("--- BẮT ĐẦU TRẢ GIÁ ---");

        // Lượt 1: Tùng MTP trả giá khởi điểm + $10,000
        System.out.print("[Bid] Tùng MTP đặt giá $260.000: ");
        if(auctionManager.placeBid(tungMtp, auction, 260000.0)) {
            auctionDAO.update(auction); // Cập nhật người dẫn đầu vào Database
            bidDAO.save(new BidTransaction(auction.getId(), tungMtp.getId(), 260000.0));
        }

        // Lượt 2: Cường Dollar trả giá cực mạnh $350.000
        System.out.print("[Bid] Cường Dollar đặt giá $350.000: ");
        if(auctionManager.placeBid(cuongDollar, auction, 350000.0)) {
            auctionDAO.update(auction);
            bidDAO.save(new BidTransaction(auction.getId(), cuongDollar.getId(), 350000.0));
        }

        // 6. KIỂM TRA TÍNH NHẤT QUÁN DỮ LIỆU CUỐI CÙNG (Final Verification)
        System.out.println("\n[KỊCH BẢN] Bước 4: Kiểm tra kết quả vĩnh viễn trong MySQL...");

        Optional<Auction> resultFromDb = auctionDAO.findById(auction.getId());
        resultFromDb.ifPresent(dbAuc -> {
            System.out.println("--- THÔNG TIN CHỐT TRONG DATABASE ---");
            System.out.println("Vật phẩm đấu giá: " + dbAuc.getItemId());
            System.out.println("Giá thầu cao nhất hiện tại: $" + dbAuc.getCurrentPrice());
            System.out.println("ID Đại gia đang dẫn đầu: " + dbAuc.getCurrentHighestBidderId());
        });

        System.out.println("\n[NHẬT KÝ] Lịch sử các bước nâng giá (Lấy từ Database):");
        List<BidTransaction> logs = bidDAO.findByAuctionId(auction.getId());
        for (BidTransaction log : logs) {
            System.out.println("   -> Lúc " + log.getBidTime().toLocalTime() + ": Bidder ID [" + log.getBidderId() + "] thầu giá $" + log.getBidAmount());
        }

        System.out.println("\n==========================================================");
        System.out.println("      Hệ thống vận hành HOÀN HẢO! Chúc mừng TEAM 4.       ");
        System.out.println("==========================================================");
    }
}