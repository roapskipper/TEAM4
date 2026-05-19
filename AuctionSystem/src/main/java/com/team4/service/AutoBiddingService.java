package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.UserDAO;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mục đích: Quản lý cấu hình auto-bid và hỗ trợ logic auto-bid. Dùng AutoBiddingDAO và AuctionDAO.
 * Khi thật sự ghi bid xuống hệ thống thì đi qua BiddingService, để chỉ có một nơi sở hữu flow đặt giá.
 */
public class AutoBiddingService {
    private static final Logger logger = LoggerFactory.getLogger(AutoBiddingService.class);
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
        logger.info("Đang bật tính năng Auto-bid: bidderId={}, auctionId={}, maxLimit={}", bidderId, auctionId, maxLimit);
        Auction auction = auctionDAO.findById(auctionId);
        // Validate phiên đáu giá
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            logger.warn("Bật Auto-bid thất bại: Phiên đấu giá không tồn tại hoặc không ở trạng thái RUNNING. auctionId={}", auctionId);
            throw new BusinessException("Phiên đấu giá không hợp lệ.");
        }
        if (auction.getSellerId().equals(bidderId)) {
            logger.warn("Bật Auto-bid thất bại: Người bán không được phép dùng Auto-bid. bidderId={}, auctionId={}", bidderId, auctionId);
            throw new BusinessException("Người bán không được dùng Auto-bid.");
        }
        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            logger.warn("Bật Auto-bid thất bại: Giới hạn tối đa ({}) nhỏ hơn hoặc bằng giá hiện tại ({}).", maxLimit, auction.getCurrentPrice());
            throw new BusinessException("Giới hạn tối đa phải lớn hơn giá hiện tại của phiên đấu giá.");
        }
        
        java.math.BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0 || maxLimit.compareTo(com.team4.util.BidRules.ABSOLUTE_MAX) > 0) {
            logger.warn("Bật Auto-bid thất bại: Giới hạn tối đa ({}) vượt quá giới hạn cho phép ({}).", maxLimit, allowedMax);
            throw new BusinessException("Bid exceeds allowed maximum for current price (policy limit).");
        }
        User user = userDAO.findById(bidderId);
        if (user == null || user.getRole() != User.Role.BIDDER) {
            logger.warn("Bật Auto-bid thất bại: Bidder không hợp lệ hoặc không tồn tại. bidderId={}", bidderId);
            throw new BusinessException("Bidder không hợp lệ.");
        }
        // Kiểm tra config đã tồn tại chưa
        AutoBidding existingConfig = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (existingConfig != null) {
            // Kiểm tra bidder đã bật autobid cho phiên này chưa
            if (existingConfig.isActive()) {
                logger.warn("Bật Auto-bid thất bại: Bidder đã có cấu hình Auto-bid đang hoạt động cho phiên này. bidderId={}, auctionId={}", bidderId, auctionId);
                // Rồi thì thông báo
                throw new BusinessException("Bạn đã bật Auto-bid cho phiên đấu giá này.");
            } else {
                // Chưa thì bật
                logger.info("Phát hiện cấu hình Auto-bid cũ đã tắt, đang kích hoạt lại. configId={}", existingConfig.getId());
                existingConfig.setMaxLimit(maxLimit);
                existingConfig.activate();
                if (!autoBiddingDAO.update(existingConfig)) {
                    logger.error("Lỗi khi cập nhật trạng thái kích hoạt lại Auto-bid. configId={}", existingConfig.getId());
                    throw new BusinessException("Không thể bật lại cấu hình auto-bid.");
                }
                logger.info("Đã kích hoạt lại thành công Auto-bid cho bidderId={} trong phiên auctionId={}", bidderId, auctionId);
                return existingConfig;
            }
        }
        // Nếu chưa có thì tạo mới
        AutoBidding newAutoBidding = new AutoBidding(auctionId, bidderId, maxLimit);
        if (!autoBiddingDAO.insert(newAutoBidding)) {
            logger.error("Lỗi khi tạo mới cấu hình Auto-bid. bidderId={}, auctionId={}", bidderId, auctionId);
            throw new BusinessException("Không thể tạo cấu hình auto-bid.");
        }
        logger.info("Đã tạo mới và bật thành công Auto-bid. configId={}, bidderId={}, auctionId={}", newAutoBidding.getId(), bidderId, auctionId);
        return newAutoBidding;
    }

    /**
     * Cập nhật cấu hình auto-bid: đổi giới hạn tối đa hoặc bước tăng giá
     */
    public boolean updateAutoBidding(String configId, BigDecimal maxLimit) {
        logger.info("Đang cập nhật cấu hình Auto-bid: configId={}, newMaxLimit={}", configId, maxLimit);
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            logger.warn("Cập nhật Auto-bid thất bại: Cấu hình không tồn tại. configId={}", configId);
            throw new BusinessException("Cấu hình Auto-bid không tồn tại.");
        }
        Auction auction = auctionDAO.findById(autoBidding.getAuctionId());
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            logger.warn("Cập nhật Auto-bid thất bại: Phiên đấu giá không hợp lệ. auctionId={}", autoBidding.getAuctionId());
            throw new BusinessException("Phiên đấu giá không hợp lệ.");
        }

        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            logger.warn("Cập nhật Auto-bid thất bại: Giới hạn mới ({}) không lớn hơn giá hiện tại ({}).", maxLimit, auction.getCurrentPrice());
            throw new BusinessException("Giới hạn tối đa phải lớn hơn giá hiện tại.");
        }

        java.math.BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0 || maxLimit.compareTo(com.team4.util.BidRules.ABSOLUTE_MAX) > 0) {
            logger.warn("Cập nhật Auto-bid thất bại: Giới hạn mới ({}) vượt quá giới hạn cho phép ({}).", maxLimit, allowedMax);
            throw new BusinessException("Bid exceeds allowed maximum for current price (policy limit).");
        }

        autoBidding.setMaxLimit(maxLimit);
        boolean updated = autoBiddingDAO.update(autoBidding);
        if (updated) {
            logger.info("Đã cập nhật thành công Auto-bid. configId={}", configId);
        } else {
            logger.error("Lỗi khi cập nhật Auto-bid vào database. configId={}", configId);
        }
        return updated;
    }

    /**
     * Tắt auto-bid: đánh dấu isActive = false, không xóa config để giữ lịch sử
     */
    public boolean disableAutoBidding(String configId, String auctionId) {
        logger.info("Đang tắt Auto-bid: configId={}, auctionId={}", configId, auctionId);
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            logger.warn("Tắt Auto-bid thất bại: Cấu hình không tồn tại. configId={}", configId);
            throw new BusinessException("Cấu hình Auto-bid không tồn tại.");
        }
        if (autoBidding.isActive() == false) {
            logger.warn("Tắt Auto-bid thất bại: Cấu hình đã được tắt trước đó. configId={}", configId);
            throw new BusinessException("Auto-bid đã tắt trước đó rồi.");
        }
        autoBidding.deactivate();
        boolean disabled = autoBiddingDAO.updateActive(configId, false);
        if (disabled) {
            logger.info("Đã tắt thành công Auto-bid. configId={}", configId);
        } else {
            logger.error("Lỗi khi cập nhật trạng thái tắt Auto-bid vào database. configId={}", configId);
        }
        return disabled;
    }

    /**
     * Lấy cấu hình auto-bid của 1 bidder trong 1 phiên, kiểm tra đã cài chưa
     */
    public AutoBidding findConfig(String bidderId, String auctionId) {
        logger.debug("Đang tìm cấu hình Auto-bid: bidderId={}, auctionId={}", bidderId, auctionId);
        AutoBidding autoBidding = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (autoBidding == null) {
            logger.warn("Không tìm thấy cấu hình Auto-bid cho bidderId={} trong phiên auctionId={}", bidderId, auctionId);
            throw new BusinessException("Bidder chưa cài cấu hình autobid cho phiên này");
        }
        return autoBidding;
    }

    /**
     * Lấy tất cả config auto-bid đang bật trong 1 phiên, dùng khi có bid mới để kích hoạt auto-bid cho những người liên quan
     */
    public List<AutoBidding> findActiveConfigs(String auctionId) {
        logger.debug("Đang lấy danh sách các cấu hình Auto-bid đang hoạt động cho phiên auctionId={}", auctionId);
        return autoBiddingDAO.findActiveByAuctionId(auctionId);
    }
}

