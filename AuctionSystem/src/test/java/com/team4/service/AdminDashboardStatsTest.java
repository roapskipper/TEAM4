package com.team4.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.team4.dao.*;
import com.team4.dao.impl.*;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auction.CreateAuctionRequestDTO;
import com.team4.dto.auth.LoginRequestDTO;
import com.team4.dto.auth.LoginResponseDTO;
import com.team4.dto.auth.RegisterBidderRequestDTO;
import com.team4.dto.auth.RegisterSellerRequestDTO;
import com.team4.dto.item.CreateArtRequestDTO;
import com.team4.dto.item.ItemResponseDTO;
import com.team4.dto.bidding.BidRequestDTO;
import com.team4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin Dashboard Stats Integration Test")
public class AdminDashboardStatsTest extends BaseServiceIntegrationTest {

    private final UserDAO userDAO = new UserDAOImpl();
    private final ItemDAO itemDAO = new ItemDAOImpl();
    private final AuctionDAO auctionDAO = new AuctionDAOImpl();
    private final AutoBiddingDAO autoDAO = new AutoBiddingDAOImpl();
    private final BidTransactionDAO bidDAO = new BidTransactionDAOImpl();

    private final JwtService jwtService = new JwtService();
    private final AuthenticationService authService = new AuthenticationService(userDAO, jwtService);
    private final UserService userService = new UserService(userDAO);
    private final ItemService itemService = new ItemService(itemDAO, userDAO, auctionDAO);
    private final AuctionService auctionService = new AuctionService(auctionDAO, itemDAO);
    private final BiddingService biddingService = new BiddingService(auctionDAO, bidDAO, userDAO, autoDAO);
    private final WalletService walletService = new WalletService(userDAO);
    private final AdminService adminService = new AdminService(userService, auctionService, auctionDAO, userDAO, itemDAO);

    @Test
    @DisplayName("Lấy thống kê dashboard thành công và trả về số liệu thực tế")
    void testGetDashboardStats_Success() {
        // --- 1. Tạo người dùng (Admin, Seller, Bidder) ---
        RegisterSellerRequestDTO regSeller = new RegisterSellerRequestDTO("seller_dash", "Pass123@", "Seller Dash", "seller_dash@test.com", "Dash Store");
        authService.registerSeller(regSeller);
        LoginResponseDTO sellerLogin = authService.loginSeller(new LoginRequestDTO("seller_dash", "Pass123@", null));
        String sellerId = sellerLogin.getUserId();

        RegisterBidderRequestDTO regBidder = new RegisterBidderRequestDTO("bidder_dash", "Bidder Dash", "Pass123@", "bidder_dash@test.com", "Hanoi", "0912345");
        authService.registerBidder(regBidder);
        LoginResponseDTO bidderLogin = authService.loginBidder(new LoginRequestDTO("bidder_dash", "Pass123@", null));
        String bidderId = bidderLogin.getUserId();

        Admin superAdmin = new Admin("admin_dash", "pass_hash", "Admin Dash", "admin_dash@test.com", Admin.AccessLevel.SUPER_ADMIN, "AdminCode@123");
        userDAO.insert(superAdmin);
        String adminId = superAdmin.getId();

        // --- 2. Chuẩn bị tài chính ---
        walletService.deposit(bidderId, new BigDecimal("3000000.00"));

        // --- 3. Tạo vật phẩm (tự động tạo phiên đấu giá ở trạng thái RUNNING) ---
        CreateArtRequestDTO artReq = new CreateArtRequestDTO("Abstract Canvas", new BigDecimal("1000000.00"), "Modern art piece", Item.ItemCategory.ART, "Unknown Artist", 2025, Art.Medium.ACRYLIC, "80x80");
        ItemResponseDTO item = itemService.createItem(sellerId, artReq);
        String itemId = item.getId();

        // Lấy auction tự động tạo
        Auction autoAuction = auctionDAO.findByItemId(itemId);
        assertNotNull(autoAuction);
        String auctionId = autoAuction.getId();

        // Kiểm tra thống kê lúc đấu giá đang hoạt động (RUNNING)
        JsonObject statsRunning = adminService.getDashboardStats(adminId);
        assertNotNull(statsRunning);
        assertEquals(3, statsRunning.get("totalUsers").getAsLong());
        assertEquals(1, statsRunning.get("totalAuctions").getAsLong());
        assertEquals(0, statsRunning.get("pendingAuctions").getAsLong());
        assertEquals(1, statsRunning.get("activeAuctions").getAsLong());
        assertEquals(0.0, statsRunning.get("totalRevenue").getAsDouble());

        // --- 4. Đặt giá ---
        biddingService.placeBid(new BidRequestDTO(auctionId, bidderId, new BigDecimal("1500000.00")));

        // --- 5. Kết thúc và thanh toán ---
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.FINISHED);
        BigDecimal finalPrice = auctionService.getAuctionById(auctionId).getCurrentPrice();
        walletService.payForAuction(bidderId, finalPrice);
        auctionService.markPaid(auctionId);

        // --- 6. Kiểm chứng kết quả stats cuối cùng ---
        JsonObject statsPaid = adminService.getDashboardStats(adminId);
        
        assertEquals(3, statsPaid.get("totalUsers").getAsLong());
        assertEquals(1, statsPaid.get("totalAuctions").getAsLong());
        assertEquals(0, statsPaid.get("activeAuctions").getAsLong());
        assertEquals(0, statsPaid.get("pendingAuctions").getAsLong());
        assertEquals(1000000.0, statsPaid.get("totalRevenue").getAsDouble());
        assertEquals(1, statsPaid.get("totalTransactions").getAsLong()); // 1 lượt bid

        // Kiểm tra biểu đồ registrationChart
        assertTrue(statsPaid.has("registrationChart"));
        JsonArray chart = statsPaid.getAsJsonArray("registrationChart");
        assertTrue(chart.size() >= 6); // Ít nhất 6 tháng gần đây
        
        // Kiểm tra xem tháng hiện tại có ghi nhận 3 lượt đăng ký hay không
        String currentMonthKey = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.US));
        boolean foundCurrentMonth = false;
        for (com.google.gson.JsonElement el : chart) {
            JsonObject point = el.getAsJsonObject();
            if (currentMonthKey.equals(point.get("month").getAsString())) {
                foundCurrentMonth = true;
                assertEquals(3, point.get("count").getAsInt());
            }
        }
        assertTrue(foundCurrentMonth, "Bieu do dang ky phai chua thang hien tai");
    }

    @Test
    @DisplayName("Gọi lấy dashboard stats từ tài khoản không phải Admin sẽ bị từ chối")
    void testGetDashboardStats_NotAdmin_ThrowsException() {
        RegisterBidderRequestDTO regBidder = new RegisterBidderRequestDTO("user_dash_fail", "Bidder Dash", "Pass123@", "user_dash_fail@test.com", "Hanoi", "0912345");
        authService.registerBidder(regBidder);
        LoginResponseDTO bidderLogin = authService.loginBidder(new LoginRequestDTO("user_dash_fail", "Pass123@", null));
        String bidderId = bidderLogin.getUserId();

        assertThrows(com.team4.util.BusinessException.class, () -> {
            adminService.getDashboardStats(bidderId);
        });
    }
}

