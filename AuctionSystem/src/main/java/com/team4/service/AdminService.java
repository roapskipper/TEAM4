package com.team4.service;

import com.team4.model.Admin;
import com.team4.dao.AuctionDAO;
import com.team4.model.Auction;
import com.team4.model.User;
import com.team4.util.BusinessException;

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

    /**
     * Duyệt phiên đấu giá: ủy quyền cho AuctionService.approveAuction(...), kiểm tra admin có đủ quyền không
     */
    public void approveAuction(String adminId, String auctionId) {
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là admin");
        }
        auctionService.approveAuction(auctionId);}

    /**
     * Từ chối phiên đấu giá: ghi lý do từ chối, chuyển status → CANCELLED qua AuctionService
     */
    public void rejectAuction(String adminId, String auctionId) {
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Người dùng không phải là admin");
        }
        auctionService.rejectAuction(auctionId);}

    /**
     * Admin hủy phiên đang chạy: kiểm tra quyền SUPER_ADMIN, gọi AuctionService.cancelAuction(...)
     */
    public void cancelAuctionByAdmin(String auctionId, String adminId) {
        User admin = userService.getUserById(adminId);
        if (admin == null || admin.getRole() != User.Role.ADMIN) {
            throw new BusinessException("Người dùng không phải là admin");
        }
        Admin adm = (Admin) admin;
        if (adm.getAccessLevel() != Admin.AccessLevel.SUPER_ADMIN) {
            throw new BusinessException("Chỉ Super Admin mới có quyền hủy phiên đấu giá");
        }
        auctionService.cancelAuction(auctionId);
    }

    /**
     * Xem toàn bộ danh sách user: ủy quyền cho UserService.getAllUsers()
     */
    public List<User> viewSystemUsers() {
        return userService.getAllUsers();
    }

    /**
     * Xem toàn bộ phiên đấu giá: ủy quyền cho AuctionService.findByStatus(...) hoặc lấy tất cả
     */
    public List<Auction> viewAllAuctions() {
        return auctionDAO.findAll();
    }
}
