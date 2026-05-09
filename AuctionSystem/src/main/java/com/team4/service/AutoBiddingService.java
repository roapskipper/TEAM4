package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.UserDAO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.User;
import com.team4.util.BusinessException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Mục đích: Quản lý cấu hình auto-bid và hỗ trợ logic auto-bid. Dùng AutoBiddingDAO và AuctionDAO.
 * Khi thật sự ghi bid xuống hệ thống thì đi qua BiddingService, để chỉ có một nơi sở hữu flow đặt giá.
 */
public class AutoBiddingService {
    private AutoBiddingDAO autoBiddingDAO;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    public AutoBiddingService(AutoBiddingDAO autoBiddingDAO, AuctionDAO auctionDAO, UserDAO userDAO) {
        this.autoBiddingDAO = autoBiddingDAO;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }
    /**
     * Bật tính năng auto-bid: kiểm tra config chưa tồn tại, validate maxLimit > currentPrice, tạo và lưu config
     */
    public AutoBidding enableAutoBidding(String bidderId, String auctionId, BigDecimal maxLimit) {
        Auction auction = auctionDAO.findById(auctionId);
        // Validate phiên đáu giá
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            throw new BusinessException("Phiên đấu giá không hợp lệ.");
        }
        if (auction.getSellerId().equals(bidderId)) {
            throw new BusinessException("Người bán không được dùng Auto-bid.");
        }
        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new BusinessException("Giới hạn tối đa phải lớn hơn giá hiện tại của phiên đấu giá.");
        }
        if (userDAO.findById(bidderId) == null || userDAO.findById(bidderId).getRole()!= User.Role.BIDDER) {
            throw new BusinessException("Bidder không hợp lệ.");
        }
        // Kiểm tra config đã tồn tại chưa
        AutoBidding existingConfig = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (existingConfig != null) {
            // Kiểm tra bidder đã bật autobid cho phiên này chưa
            if (existingConfig.isActive()) {
                // Rồi thì thông báo
                throw new BusinessException("Bạn đã bật Auto-bid cho phiên đấu giá này.");
            } else {
                // Chưa thì bật
                existingConfig.setMaxLimit(maxLimit);
                existingConfig.activate();
                if (!autoBiddingDAO.update(existingConfig)) {
                    throw new BusinessException("Không thể bật lại cấu hình auto-bid.");
                }
                return existingConfig;
            }
        }
        // Nếu chưa có thì tạo mới
        AutoBidding newAutoBidding = new AutoBidding(auctionId, bidderId, maxLimit);
        if (!autoBiddingDAO.insert(newAutoBidding)) {
            throw new BusinessException("Không thể tạo cấu hình auto-bid.");
        }
        return newAutoBidding;
    }

    /**
     * Cập nhật cấu hình auto-bid: đổi giới hạn tối đa hoặc bước tăng giá
     */
    public boolean updateAutoBidding(String configId, BigDecimal maxLimit) {
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            throw new BusinessException("Cấu hình Auto-bid không tồn tại.");
        }
        Auction auction = auctionDAO.findById(autoBidding.getAuctionId());
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            throw new BusinessException("Phiên đấu giá không hợp lệ.");
        }

        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new BusinessException("Giới hạn tối đa phải lớn hơn giá hiện tại.");
        }

        autoBidding.setMaxLimit(maxLimit);
        return autoBiddingDAO.update(autoBidding);
    }

    /**
     * Tắt auto-bid: đánh dấu isActive = false, không xóa config để giữ lịch sử
     */
    public boolean disableAutoBidding(String configId, String auctionId) {
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            throw new BusinessException("Cấu hình Auto-bid không tồn tại.");
        }
        if (autoBidding.isActive() == false) {
            throw new BusinessException("Auto-bid đã tắt trước đó rồi.");
        }
        autoBidding.deactivate();
        return autoBiddingDAO.updateActive(configId, false);
    }

    /**
     * Lấy cấu hình auto-bid của 1 bidder trong 1 phiên, kiểm tra đã cài chưa
     */
    public AutoBidding findConfig(String bidderId, String auctionId) {
        AutoBidding autoBidding = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (autoBidding == null) {
            throw new BusinessException("Bidder chưa cài cấu hình autobid cho phiên này");
        }
        return autoBidding;
    }

    /**
     * Lấy tất cả config auto-bid đang bật trong 1 phiên, dùng khi có bid mới để kích hoạt auto-bid cho những người liên quan
     */
    public List<AutoBidding> findActiveConfigs(String auctionId) {
        return autoBiddingDAO.findActiveByAuctionId(auctionId);
    }
}

