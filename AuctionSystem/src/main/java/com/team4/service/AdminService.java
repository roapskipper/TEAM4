package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dto.auction.AuctionResponseDTO;
import com.team4.dto.auth.UserResponseDTO;
import com.team4.mapper.AuctionMapper;
import com.team4.mapper.UserMapper;
import com.team4.model.Admin;
import com.team4.model.Auction;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Điều phối các nghiệp vụ quản trị cấp cao.
 */
public class AdminService {
    private final UserService userService;
    private final AuctionService auctionService;
    private final AuctionDAO auctionDAO;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    public AdminService(UserService userService, AuctionService auctionService, AuctionDAO auctionDAO) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.auctionDAO = auctionDAO;
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
}
