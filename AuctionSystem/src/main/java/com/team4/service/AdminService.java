package com.team4.service;

import com.team4.dao.impl.AuctionDAOImpl;
import com.team4.model.Admin;
import com.team4.dao.AuctionDAO;
import com.team4.model.Auction;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mục đích: Gom các use case quản trị cấp cao. Gọi AuctionService, UserService, ItemService thay vì tự viết lại logic.
 * Admin service điều phối, không copy nghiệp vụ của service khác.
 */
public class AdminService {
    private UserService userService;
    private AuctionService auctionService;
    private AuctionDAO auctionDAO;

    public AdminService(UserService userService, AuctionService auctionService, AuctionDAO auctionDAO) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.auctionDAO = auctionDAO;
    }

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    /**
     * Duyệt phiên đấu giá: ủy quyền cho AuctionService.approveAuction(...), kiểm tra admin có đủ quyền không
     */
    public void approveAuction(String adminId, String auctionId) {
        logger.info("Admin is approving auction: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Auction approval failed: user does not have admin privileges. adminId={}", adminId);
            throw new IllegalArgumentException("User is not an admin");
        }
        auctionService.approveAuction(auctionId);
        logger.info("Auction approved successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Từ chối phiên đấu giá: ghi lý do từ chối, chuyển status → CANCELLED qua AuctionService
     */
    public void rejectAuction(String adminId, String auctionId) {
        logger.info("Admin is rejecting auction: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Auction rejection failed: user does not have admin privileges. adminId={}", adminId);
            throw new IllegalArgumentException("User is not an admin");
        }
        auctionService.rejectAuction(auctionId);
        logger.info("Auction rejected successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Admin hủy phiên đang chạy: kiểm tra quyền SUPER_ADMIN, gọi AuctionService.cancelAuction(...)
     */
    public void cancelAuctionByAdmin(String auctionId, String adminId) {
        logger.info("Admin is requesting auction cancellation: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Auction cancellation failed: user does not have admin privileges. adminId={}", adminId);
            throw new BusinessException("User is not an admin");
        }
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            logger.warn("Auction cancellation failed: SUPER_ADMIN privileges required. adminId={}, accessLevel={}", adminId, adm.getAccessLevel());
            throw new BusinessException("Only Super Admin can cancel auctions");
        }
        auctionService.cancelAuction(auctionId);
        logger.info("Super Admin cancelled auction successfully: auctionId={} by adminId={}", auctionId, adminId);
    }

    /**
     * Xem toàn bộ danh sách user: ủy quyền cho UserService.getAllUsers()
     */
    public List<User> viewSystemUsers() {
        logger.debug("Admin is viewing the system user list");
        return userService.getAllUsers();
    }

    /**
     * Xem toàn bộ phiên đấu giá: ủy quyền cho AuctionService.findByStatus(...) hoặc lấy tất cả
     */
    public List<Auction> viewAllAuctions() {
        logger.debug("Admin is viewing all auctions");
        return auctionDAO.findAll();
    }
}
