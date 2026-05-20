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
        logger.info("Enabling Auto-bid: bidderId={}, auctionId={}, maxLimit={}", bidderId, auctionId, maxLimit);
        Auction auction = auctionDAO.findById(auctionId);
        // Validate phiên đáu giá
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            logger.warn("Enable Auto-bid failed: auction does not exist or is not RUNNING. auctionId={}", auctionId);
            throw new BusinessException("Invalid auction.");
        }
        if (auction.getSellerId().equals(bidderId)) {
            logger.warn("Enable Auto-bid failed: seller cannot use Auto-bid. bidderId={}, auctionId={}", bidderId, auctionId);
            throw new BusinessException("Seller cannot use Auto-bid.");
        }
        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            logger.warn("Enable Auto-bid failed: max limit ({}) is less than or equal to current price ({}).", maxLimit, auction.getCurrentPrice());
            throw new BusinessException("Max limit must be greater than the current auction price.");
        }
        
        BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0) {
            logger.warn("Enable Auto-bid failed: exceeds allowed maximum policy limit. limit={}, requested={}", allowedMax, maxLimit);
            throw new BusinessException("Max limit exceeds the allowed maximum policy limit.");
        }
        User user = userDAO.findById(bidderId);
        if (user == null || user.getRole() != User.Role.BIDDER) {
            logger.warn("Enable Auto-bid failed: invalid or missing bidder. bidderId={}", bidderId);
            throw new BusinessException("Invalid bidder.");
        }
        // Kiểm tra config đã tồn tại chưa
        AutoBidding existingConfig = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (existingConfig != null) {
            // Kiểm tra bidder đã bật autobid cho phiên này chưa
            if (existingConfig.isActive()) {
                logger.warn("Enable Auto-bid failed: bidder already has an active Auto-bid configuration for this auction. bidderId={}, auctionId={}", bidderId, auctionId);
                // Rồi thì thông báo
                throw new BusinessException("You have already enabled Auto-bid for this auction.");
            } else {
                // Chưa thì bật
                logger.info("Found an inactive Auto-bid configuration; reactivating it. configId={}", existingConfig.getId());
                existingConfig.setMaxLimit(maxLimit);
                existingConfig.activate();
                if (!autoBiddingDAO.update(existingConfig)) {
                    logger.error("Error while reactivating Auto-bid status. configId={}", existingConfig.getId());
                    throw new BusinessException("Unable to reactivate auto-bid configuration.");
                }
                logger.info("Auto-bid reactivated successfully for bidderId={} in auctionId={}", bidderId, auctionId);
                return existingConfig;
            }
        }
        // Nếu chưa có thì tạo mới
        AutoBidding newAutoBidding = new AutoBidding(auctionId, bidderId, maxLimit);
        if (!autoBiddingDAO.insert(newAutoBidding)) {
            logger.error("Error while creating Auto-bid configuration. bidderId={}, auctionId={}", bidderId, auctionId);
            throw new BusinessException("Unable to create auto-bid configuration.");
        }
        logger.info("Auto-bid created and enabled successfully. configId={}, bidderId={}, auctionId={}", newAutoBidding.getId(), bidderId, auctionId);
        return newAutoBidding;
    }

    /**
     * Cập nhật cấu hình auto-bid: đổi giới hạn tối đa hoặc bước tăng giá
     */
    public boolean updateAutoBidding(String configId, BigDecimal maxLimit) {
        logger.info("Updating Auto-bid configuration: configId={}, newMaxLimit={}", configId, maxLimit);
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            logger.warn("Auto-bid update failed: configuration does not exist. configId={}", configId);
            throw new BusinessException("Auto-bid configuration does not exist.");
        }
        Auction auction = auctionDAO.findById(autoBidding.getAuctionId());
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            logger.warn("Auto-bid update failed: invalid auction. auctionId={}", autoBidding.getAuctionId());
            throw new BusinessException("Invalid auction.");
        }

        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            logger.warn("Auto-bid update failed: new limit ({}) is not greater than current price ({}).", maxLimit, auction.getCurrentPrice());
            throw new BusinessException("Max limit must be greater than the current price.");
        }

        BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0) {
            logger.warn("Auto-bid update failed: exceeds allowed maximum policy limit. limit={}, requested={}", allowedMax, maxLimit);
            throw new BusinessException("Max limit exceeds the allowed maximum policy limit.");
        }

        autoBidding.setMaxLimit(maxLimit);
        boolean updated = autoBiddingDAO.update(autoBidding);
        if (updated) {
            logger.info("Auto-bid updated successfully. configId={}", configId);
        } else {
            logger.error("Error while updating Auto-bid in the database. configId={}", configId);
        }
        return updated;
    }

    /**
     * Tắt auto-bid: đánh dấu isActive = false, không xóa config để giữ lịch sử
     */
    public boolean disableAutoBidding(String configId, String auctionId) {
        logger.info("Disabling Auto-bid: configId={}, auctionId={}", configId, auctionId);
        AutoBidding autoBidding = autoBiddingDAO.findById(configId);
        if (autoBidding == null) {
            logger.warn("Disable Auto-bid failed: configuration does not exist. configId={}", configId);
            throw new BusinessException("Auto-bid configuration does not exist.");
        }
        if (autoBidding.isActive() == false) {
            logger.warn("Disable Auto-bid failed: configuration was already disabled. configId={}", configId);
            throw new BusinessException("Auto-bid was already disabled.");
        }
        autoBidding.deactivate();
        boolean disabled = autoBiddingDAO.updateActive(configId, false);
        if (disabled) {
            logger.info("Auto-bid disabled successfully. configId={}", configId);
        } else {
            logger.error("Error while updating disabled Auto-bid status in the database. configId={}", configId);
        }
        return disabled;
    }

    /**
     * Lấy cấu hình auto-bid của 1 bidder trong 1 phiên, kiểm tra đã cài chưa
     */
    public AutoBidding findConfig(String bidderId, String auctionId) {
        logger.debug("Looking up Auto-bid configuration: bidderId={}, auctionId={}", bidderId, auctionId);
        AutoBidding autoBidding = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (autoBidding == null) {
            logger.warn("No Auto-bid configuration found for bidderId={} in auctionId={}", bidderId, auctionId);
            throw new BusinessException("Bidder has not configured auto-bid for this auction");
        }
        return autoBidding;
    }

    /**
     * Lấy tất cả config auto-bid đang bật trong 1 phiên, dùng khi có bid mới để kích hoạt auto-bid cho những người liên quan
     */
    public List<AutoBidding> findActiveConfigs(String auctionId) {
        logger.debug("Loading active Auto-bid configurations for auctionId={}", auctionId);
        return autoBiddingDAO.findActiveByAuctionId(auctionId);
    }
}

