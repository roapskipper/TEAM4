package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auction.AdminAuctionResponseDTO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.AuctionMapper;
import com.team4.model.Admin;
import com.team4.model.Auction;
import com.team4.model.Item;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.team4.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Điều phối các nghiệp vụ quản trị cấp cao.
 */
public class AdminService {
    private final UserService userService;
    private final AuctionService auctionService;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    public AdminService(UserService userService, AuctionService auctionService, AuctionDAO auctionDAO, UserDAO userDAO, ItemDAO itemDAO) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
        this.itemDAO = itemDAO;
    }

    /**
     * Admin duyệt phiên đấu giá.
     */
    public void approveAuction(String adminId, String auctionId) {
        logger.info("Admin is approving auction: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            logger.warn("Approval failed: User is not an admin. adminId={}", adminId);
            throw new BusinessException("User does not have admin privileges");
        }
        auctionService.approveAuction(auctionId);
        logger.info("Auction approved successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Admin từ chối phê duyệt phiên đấu giá.
     */
    public void rejectAuction(String adminId, String auctionId) {
        logger.info("Admin is rejecting auction: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            logger.warn("Rejection failed: User is not an admin. adminId={}", adminId);
            throw new BusinessException("User does not have admin privileges");
        }
        auctionService.rejectAuction(auctionId);
        logger.info("Auction rejected successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Admin hủy phiên đấu giá đang chạy (Yêu cầu quyền SUPER_ADMIN).
     */
    public void cancelAuctionByAdmin(String adminId, String auctionId) {
        logger.info("Admin is requesting auction cancellation: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }
        
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            logger.warn("Cancellation failed: SUPER_ADMIN privileges required. adminId={}", adminId);
            throw new BusinessException("Only Super Admin can cancel active auctions");
        }
        
        auctionService.cancelAuction(auctionId);
        logger.info("Super Admin cancelled auction successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Xem danh sách tất cả người dùng trong hệ thống (DTO).
     */
    public List<UserResponseDTO> viewSystemUsers(String adminId) {
        logger.debug("Admin is viewing all system users: adminId={}", adminId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }
        return userService.getAllUsers();
    }

    /**
     * Overload method for backward compatibility in existing tests.
     */
    public List<UserResponseDTO> viewSystemUsers() {
        logger.debug("Admin is viewing all system users");
        return userService.getAllUsers();
    }

    /**
     * Xem danh sách tất cả các phiên đấu giá trong hệ thống (DTO).
     */
    public List<AuctionResponseDTO> viewAllAuctions() {
        logger.debug("Admin is viewing all auctions");
        return auctionDAO.findAll().stream()
                .map(AuctionMapper::toAuctionResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Xem danh sách đấu giá với thông tin chi tiết (cho admin handler).
     */
    public List<AdminAuctionResponseDTO> viewAuctions(String adminId, String filter) {
        logger.debug("Admin is viewing auctions with filter: adminId={}, filter={}", adminId, filter);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }

        List<Auction> auctions;
        String f = filter == null ? "all" : filter.trim().toLowerCase();
        switch (f) {
            case "pending":
                auctions = auctionDAO.findByStatus(Auction.AuctionStatus.PENDING);
                break;
            case "live":
                auctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
                break;
            case "rejected":
                auctions = auctionDAO.findByStatus(Auction.AuctionStatus.CANCELLED);
                break;
            case "all":
            default:
                auctions = auctionDAO.findAll();
                break;
        }

        return auctions.stream().map(auction -> {
            String itemName = "Unknown Item";
            Item item = itemDAO.findById(auction.getItemId());
            if (item != null) {
                itemName = item.getName();
            }

            String sellerName = "Unknown Seller";
            User seller = userDAO.findById(auction.getSellerId());
            if (seller != null) {
                String sName = seller.getFullName();
                if (sName == null || sName.trim().isEmpty()) {
                    sName = seller.getUsername();
                }
                sellerName = sName;
            }

            return new AdminAuctionResponseDTO(
                    auction.getId(),
                    itemName,
                    sellerName,
                    auction.getStartingPrice(),
                    auction.getStatus().name(),
                    0
            );
        }).collect(Collectors.toList());
    }

    /**
     * Cấp quyền admin cho user.
     */
    public void grantAdmin(String adminId, String targetUserId, String adminCode) {
        logger.info("Admin is granting admin privileges: adminId={}, targetUserId={}", adminId, targetUserId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            logger.warn("Grant admin failed: SUPER_ADMIN privileges required. adminId={}", adminId);
            throw new BusinessException("Only Super Admin can grant admin privileges");
        }

        if (adminCode == null || adminCode.trim().length() < 8) {
            throw new BusinessException("Admin code must be at least 8 characters long.");
        }

        String codeHash = com.team4.util.PasswordHasher.hashPassword(adminCode.trim());
        if (!userDAO.grantAdminRole(targetUserId, codeHash)) {
            logger.warn("Grant admin failed in DAO for targetUserId={}", targetUserId);
            throw new BusinessException("Failed to grant admin role. Check if user exists and is a Bidder or Seller.");
        }
        logger.info("Admin privileges granted successfully to targetUserId={}", targetUserId);
    }

    /**
     * Thu hồi quyền admin của user.
     */
    public void revokeAdmin(String adminId, String targetUserId) {
        logger.info("Admin is revoking admin privileges: adminId={}, targetUserId={}", adminId, targetUserId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            logger.warn("Revoke admin failed: SUPER_ADMIN privileges required. adminId={}", adminId);
            throw new BusinessException("Only Super Admin can revoke admin privileges");
        }

        if (!userDAO.revokeAdminRole(targetUserId)) {
            logger.warn("Revoke admin failed in DAO for targetUserId={}", targetUserId);
            throw new BusinessException("Failed to revoke admin role or user is not an Admin or is a Super Admin.");
        }
        logger.info("Admin privileges revoked successfully from targetUserId={}", targetUserId);
    }

    /**
     * Lấy dữ liệu thống kê tổng quan cho Admin Dashboard.
     */
    public JsonObject getDashboardStats(String adminId) {
        logger.info("Admin is requesting dashboard stats: adminId={}", adminId);
        User admin = userService.getRawUserById(adminId);
        if (admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("User does not have admin privileges");
        }

        JsonObject stats = new JsonObject();
        long totalUsers = 0;
        long totalAuctions = 0;
        long activeAuctions = 0;
        long pendingAuctions = 0;
        double totalRevenue = 0.0;
        long totalTransactions = 0;

        String queryUsers = "SELECT COUNT(*) FROM users";
        String queryAuctions = "SELECT COUNT(*) FROM auctions";
        String queryActiveAuctions = "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING'";
        String queryPendingAuctions = "SELECT COUNT(*) FROM auctions WHERE status = 'PENDING'";
        String queryRevenue = "SELECT COALESCE(SUM(current_price), 0.0) FROM auctions WHERE status = 'PAID'";
        String queryTransactions = "SELECT COUNT(*) FROM bid_transactions";
        String queryChart = "SELECT created_at FROM users ORDER BY created_at ASC";

        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Total Users
            try (PreparedStatement stmt = conn.prepareStatement(queryUsers);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) totalUsers = rs.getLong(1);
            }
            // 2. Total Auctions
            try (PreparedStatement stmt = conn.prepareStatement(queryAuctions);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) totalAuctions = rs.getLong(1);
            }
            // 3. Active Auctions
            try (PreparedStatement stmt = conn.prepareStatement(queryActiveAuctions);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) activeAuctions = rs.getLong(1);
            }
            // 4. Pending Auctions
            try (PreparedStatement stmt = conn.prepareStatement(queryPendingAuctions);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) pendingAuctions = rs.getLong(1);
            }
            // 5. Total Revenue
            try (PreparedStatement stmt = conn.prepareStatement(queryRevenue);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) totalRevenue = rs.getDouble(1);
            }
            // 6. Total Transactions
            try (PreparedStatement stmt = conn.prepareStatement(queryTransactions);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) totalTransactions = rs.getLong(1);
            }

            // 7. Registration Chart
            List<LocalDateTime> userCreatedDates = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(queryChart);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        userCreatedDates.add(ts.toLocalDateTime());
                    }
                }
            }

            // Gom nhóm theo tháng (VD: "Jan", "Feb"...)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM", Locale.US);
            Map<String, Integer> monthlyCountMap = new LinkedHashMap<>();
            
            // Khởi tạo các tháng trước (để đảm bảo có đủ ít nhất 6 tháng gần nhất)
            LocalDateTime temp = LocalDateTime.now().minusMonths(5);
            for (int i = 0; i < 6; i++) {
                monthlyCountMap.put(temp.plusMonths(i).format(formatter), 0);
            }

            for (LocalDateTime date : userCreatedDates) {
                String monthKey = date.format(formatter);
                if (monthlyCountMap.containsKey(monthKey)) {
                    monthlyCountMap.put(monthKey, monthlyCountMap.get(monthKey) + 1);
                } else {
                    // Để vẽ biểu đồ, ta cũng chấp nhận gom nhóm các tháng trước đó nếu có dữ liệu
                    monthlyCountMap.put(monthKey, monthlyCountMap.getOrDefault(monthKey, 0) + 1);
                }
            }

            JsonArray chartArray = new JsonArray();
            for (Map.Entry<String, Integer> entry : monthlyCountMap.entrySet()) {
                JsonObject point = new JsonObject();
                point.addProperty("month", entry.getKey());
                point.addProperty("count", entry.getValue());
                chartArray.add(point);
            }

            stats.addProperty("totalUsers", totalUsers);
            stats.addProperty("totalAuctions", totalAuctions);
            stats.addProperty("activeAuctions", activeAuctions);
            stats.addProperty("pendingAuctions", pendingAuctions);
            stats.addProperty("totalRevenue", totalRevenue);
            stats.addProperty("totalTransactions", totalTransactions);
            stats.add("registrationChart", chartArray);

        } catch (SQLException e) {
            logger.error("Failed to load dashboard stats", e);
            throw new BusinessException("Failed to retrieve dashboard statistics from database");
        }

        return stats;
    }
}
