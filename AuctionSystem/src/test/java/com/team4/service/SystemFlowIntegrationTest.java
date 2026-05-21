package com.team4.service;

import com.team4.dao.*;
import com.team4.dao.impl.*;
import com.team4.dto.auction.*;
import com.team4.dto.auth.*;
import com.team4.dto.bidding.*;
import com.team4.dto.item.*;
import com.team4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử luồng hệ thống hoàn chỉnh (End-to-End Business Flow).
 * Mô phỏng toàn bộ vòng đời: Đăng ký -> Tạo hàng -> Đấu giá -> Thắng cuộc -> Thanh toán.
 * Không cần thông qua mạng (Server/Handler).
 */
@DisplayName("End-to-End System Flow Integration Test")
public class SystemFlowIntegrationTest extends BaseServiceIntegrationTest {

    // Khởi tạo các DAO thật
    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final AutoBiddingDAO autoDAO = new AutoBiddingDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();

    // Khởi tạo các Service thật
    private final JwtService jwtService = new JwtService();
    private final AuthenticationService authService = new AuthenticationService(userDAO, jwtService);
    private final UserService userService = new UserService(userDAO);
    private final ItemService itemService = new ItemService(itemDAO, userDAO);
    private final AuctionService auctionService = new AuctionService(auctionDAO, itemDAO);
    private final BiddingService biddingService = new BiddingService(auctionDAO, bidDAO, userDAO, autoDAO);
    private final WalletService walletService = new WalletService(userDAO);
    private final AdminService adminService = new AdminService(userService, auctionService, auctionDAO);

    @Test
    @DisplayName("Kịch bản thành công hoàn chỉnh: Từ người dùng mới đến khi mua hàng thành công")
    void testFullSuccessScenario() {
        // --- GIAI ĐOẠN 1: THIẾT LẬP NGƯỜI DÙNG ---
        // 1. Đăng ký Seller
        RegisterSellerRequestDTO regSeller = new RegisterSellerRequestDTO("seller1", "Pass123@", "Seller One", "seller1@test.com", "My Art Store");
        authService.registerSeller(regSeller);
        LoginResponseDTO sellerLogin = authService.loginSeller(new LoginRequestDTO("seller1", "Pass123@", null));
        String sellerId = sellerLogin.getUserId();

        // 2. Đăng ký Bidder
        RegisterBidderRequestDTO regBidder = new RegisterBidderRequestDTO("bidder1", "Bidder One", "Pass123@", "bidder1@test.com", "Hanoi", "0912");
        authService.registerBidder(regBidder);
        LoginResponseDTO bidderLogin = authService.loginBidder(new LoginRequestDTO("bidder1", "Pass123@", null));
        String bidderId = bidderLogin.getUserId();

        // 3. Admin (Đã có sẵn trong DB Seed hoặc tạo mới)
        // Lưu ý: Thường Admin cấp cao được tạo bằng script SQL. Ở đây ta tạo nhanh 1 Super Admin để duyệt.
        Admin superAdmin = new Admin("admin_root", "pass_hash", "System Admin", "admin@test.com", Admin.AccessLevel.SUPER_ADMIN, "Admin@123");
        userDAO.insert(superAdmin);
        String adminId = superAdmin.getId();

        // --- GIAI ĐOẠN 2: CHUẨN BỊ TÀI CHÍNH & VẬT PHẨM ---
        // 4. Bidder nạp tiền vào ví
        walletService.deposit(bidderId, new BigDecimal("5000000.00"));

        // 5. Seller tạo vật phẩm mới
        CreateArtRequestDTO artReq = new CreateArtRequestDTO("Legendary Sword Drawing", new BigDecimal("1000000.00"), "Rare art", Item.ItemCategory.ART, "Unknown Artist", 2024, Art.Medium.OIL_PAINT, "A4");
        ItemResponseDTO item = itemService.createItem(sellerId, artReq);
        String itemId = item.getId();

        // --- GIAI ĐOẠN 3: PHIÊN ĐẤU GIÁ ---
        // 6. Seller yêu cầu mở đấu giá
        CreateAuctionRequestDTO auctionReq = new CreateAuctionRequestDTO(itemId, sellerId, new BigDecimal("1000000.00"), new BigDecimal("50000.00"), LocalDateTime.now().plusHours(1));
        AuctionResponseDTO auctionDTO = auctionService.createAuction(auctionReq);
        String auctionId = auctionDTO.getId();

        // 7. Admin duyệt phiên đấu giá
        adminService.approveAuction(adminId, auctionId);
        
        // Kiểm tra trạng thái phiên phải là RUNNING
        assertEquals(Auction.AuctionStatus.RUNNING, auctionService.getAuctionById(auctionId).getStatus());

        // --- GIAI ĐOẠN 4: THỰC HIỆN ĐẶT GIÁ ---
        // 8. Bidder đặt giá (Proxy Bidding tối đa 2.000.000)
        biddingService.placeBid(new BidRequestDTO(auctionId, bidderId, new BigDecimal("2000000.00")));
        
        // Kiểm tra giá hiện tại phải là giá khởi điểm (do chưa có ai cạnh tranh)
        AuctionResponseDTO currentAuctionDTO = auctionService.getAuctionById(auctionId);
        assertEquals(0, new BigDecimal("1000000.00").compareTo(currentAuctionDTO.getCurrentPrice()));
        
        // Kiểm tra người dẫn đầu (Vì DTO không có field này, ta kiểm tra trực tiếp qua DAO/Model)
        Auction dbAuction = auctionDAO.findById(auctionId);
        assertEquals(bidderId, dbAuction.getCurrentHighestBidderId());

        // --- GIAI ĐOẠN 5: KẾT THÚC & THANH TOÁN ---
        // 9. Giả lập kết thúc phiên đấu giá
        // (Trong test ta can thiệp trực tiếp vào DB để đổi thời gian về quá khứ hoặc gọi hàm close của service)
        // Để đơn giản, ta gọi hàm tự động đóng phiên (giả định Service sẽ thấy thời gian đã hết)
        // Nhưng ở đây ta set thời gian Auction trong DB thành quá khứ trước:
        Auction rawAuction = auctionDAO.findById(auctionId);
        // Ta không thể setEndTime vì nó final trong model? Đừng lo, ta dùng chính logic close của service.
        auctionService.closeExpiredAuctions(); // Nếu may mắn LocalDateTime.now() trôi qua đủ nhanh hoặc ta dùng Mock cho Time (nhưng đây là Integration test)
        
        // Cách chắc chắn hơn cho Integration Test: Gọi thẳng hàm cập nhật trạng thái trong DAO để mô phỏng hệ thống quét xong
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);

        // 10. Thanh toán
        BigDecimal finalPrice = auctionService.getAuctionById(auctionId).getCurrentPrice();
        walletService.payForAuction(bidderId, finalPrice);
        auctionService.markPaid(auctionId);

        // --- KIỂM CHỨNG CUỐI CÙNG ---
        // - Trạng thái phiên là PAID
        assertEquals(Auction.AuctionStatus.PAID, auctionService.getAuctionById(auctionId).getStatus());
        // - Số dư Bidder bị trừ (5.000.000 - 1.000.000 = 4.000.000)
        assertEquals(0, new BigDecimal("4000000.00").compareTo(walletService.getBalance(bidderId)));
        
        System.out.println(">>> FULL SYSTEM FLOW TEST PASSED SUCCESSFULLY <<<");
    }
}
