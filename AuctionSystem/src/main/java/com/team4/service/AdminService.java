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
        logger.info("Admin đang duyệt phiên đấu giá: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Duyệt phiên đấu giá thất bại: Người dùng không có quyền admin. adminId={}", adminId);
            throw new IllegalArgumentException("Người dùng không phải là admin");
        }
        auctionService.approveAuction(auctionId);
        logger.info("Đã duyệt thành công phiên đấu giá: auctionId={} bởi adminId={}", auctionId, adminId);
    }

    /**
     * Từ chối phiên đấu giá: ghi lý do từ chối, chuyển status → CANCELLED qua AuctionService
     */
    public void rejectAuction(String adminId, String auctionId) {
        logger.info("Admin đang từ chối phiên đấu giá: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Từ chối phiên đấu giá thất bại: Người dùng không có quyền admin. adminId={}", adminId);
            throw new IllegalArgumentException("Người dùng không phải là admin");
        }
        auctionService.rejectAuction(auctionId);
        logger.info("Đã từ chối thành công phiên đấu giá: auctionId={} bởi adminId={}", auctionId, adminId);
    }

    /**
     * Admin hủy phiên đang chạy: kiểm tra quyền SUPER_ADMIN, gọi AuctionService.cancelAuction(...)
     */
    public void cancelAuctionByAdmin(String auctionId, String adminId) {
        logger.info("Admin đang yêu cầu hủy phiên đấu giá: adminId={}, auctionId={}", adminId, auctionId);
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            logger.warn("Hủy phiên đấu giá thất bại: Người dùng không có quyền admin. adminId={}", adminId);
            throw new BusinessException("Người dùng không phải là admin");
        }
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            logger.warn("Hủy phiên đấu giá thất bại: Yêu cầu quyền SUPER_ADMIN. adminId={}, accessLevel={}", adminId, adm.getAccessLevel());
            throw new BusinessException("Chỉ Super Admin mới có quyền hủy phiên đấu giá");
        }
        auctionService.cancelAuction(auctionId);
        logger.info("Super Admin đã hủy thành công phiên đấu giá: auctionId={} bởi adminId={}", auctionId, adminId);
    }

    /**
     * Xem toàn bộ danh sách user: ủy quyền cho UserService.getAllUsers()
     */
    public List<User> viewSystemUsers() {
        logger.debug("Admin đang xem danh sách người dùng hệ thống");
        return userService.getAllUsers();
    }

    /**
     * Xem toàn bộ phiên đấu giá: ủy quyền cho AuctionService.findByStatus(...) hoặc lấy tất cả
     */
    public List<Auction> viewAllAuctions() {
        logger.debug("Admin đang xem tất cả các phiên đấu giá");
        return auctionDAO.findAll();
    }
}
