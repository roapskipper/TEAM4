package com.team4;

import com.team4.dao.impl.*;
import com.team4.model.*;
import com.team4.service.AuctionManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("      HỆ THỐNG ĐẤU GIÁ TEAM 4 - BẢN TỔNG HỢP FINAL       ");
        System.out.println("==========================================================\n");

        // 1. Khởi tạo
        UserDAOImpl userDAO = new UserDAOImpl();
        ItemDAOImpl itemDAO = new ItemDAOImpl();
        AuctionDAOImpl auctionDAO = new AuctionDAOImpl();
        BidTransactionDAOImpl bidDAO = new BidTransactionDAOImpl();
        AuctionManager manager = AuctionManager.getInstance();

        // 2. Tạo người dùng (Tự sinh UUID)
        Seller sellerTrung = new Seller("trung_owner", "pass123", "Trung Auto Shop");
        Bidder bidderAn = new Bidder("an_dai_gia", "an_pass", 500000.0, "Hà Nội", "0911222333");

        userDAO.save(sellerTrung);
        userDAO.save(bidderAn);

        // 3. Tạo vật phẩm qua Factory
        Item sieuXe = new com.team4.model.Vehicle(
                "Ferrari Purosangue", 400000.0, "Siêu phẩm SUV 2024", sellerTrung.getId(),
                "Ferrari", "Purosangue", 2024, 0, "V12", "Đỏ Rosso", "30H-888.88", true, "Auto"
        );
        itemDAO.save(sieuXe);

        // 4. Mở phiên đấu giá
        Auction auctionXe = new Auction(sieuXe.getId(), sellerTrung.getId(), 400000.0, LocalDateTime.now().plusHours(1));
        auctionDAO.save(auctionXe);

        // 5. Thực hiện Đặt giá (Business Logic)
        System.out.print("[BID] An Đại Gia trả giá $450.000: ");
        if(manager.placeBid(bidderAn, auctionXe, 450000.0)) {
            auctionDAO.update(auctionXe); // Cập nhật DB
            bidDAO.save(new BidTransaction(auctionXe.getId(), bidderAn.getId(), 450000.0));
        }

        // 6. Kiểm tra kết quả vĩnh viễn trong MySQL
        System.out.println("\n[XÁC MINH] Truy vấn ngược từ MySQL...");
        Optional<Auction> fromDb = auctionDAO.findById(auctionXe.getId());
        fromDb.ifPresent(dbAuc -> {
            System.out.println("--- THÔNG TIN CHỐT TRONG DATABASE ---");
            System.out.println("Vật phẩm đấu giá: Ferrari");
            System.out.println("Giá hiện tại: $" + dbAuc.getCurrentPrice());
            System.out.println("Người dẫn đầu: " + dbAuc.getCurrentHighestBidderId());
        });

        System.out.println("\n==========================================================");
        System.out.println("      HỆ THỐNG VẬN HÀNH HOÀN HẢO! CHÚC MỪNG TEAM 4.       ");
        System.out.println("==========================================================");
    } // Đóng hàm main ở cuối cùng này nhé Trung!
}