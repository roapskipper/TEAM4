package com.team4.service;

import com.team4.dao.AutoBiddingDAO;
import com.team4.dao.AuctionDAO;
import com.team4.dao.UserDAO;
import com.team4.dto.bidding.AutoBidResponseDTO;
import com.team4.dto.bidding.AutoBidRequestDTO;
import com.team4.mapper.AutoBidMapper;
import com.team4.model.Auction;
import com.team4.model.AutoBidding;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý cấu hình tự động đặt giá (Auto-bid).
 */
public class AutoBiddingService {
    private static final Logger logger = LoggerFactory.getLogger(AutoBiddingService.class);
    private final AutoBiddingDAO autoBiddingDAO;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;

    public AutoBiddingService(AutoBiddingDAO autoBiddingDAO, AuctionDAO auctionDAO, UserDAO userDAO) {
        this.autoBiddingDAO = autoBiddingDAO;
        this.auctionDAO = auctionDAO;
        this.userDAO = userDAO;
    }

    /**
     * Bật tính năng tự động đặt giá cho một phiên đấu giá.
     */
    public AutoBidResponseDTO enableAutoBidding(AutoBidRequestDTO requestDTO) {
        String bidderId = requestDTO.getBidderId();
        String auctionId = requestDTO.getAuctionId();
        BigDecimal maxLimit = requestDTO.getMaxAmount();

        logger.info("Enabling Auto-bid: bidderId={}, auctionId={}, maxLimit={}", bidderId, auctionId, maxLimit);
        
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            logger.warn("Auto-bid rejected: auction not found or not active. auctionId={}", auctionId);
            throw new BusinessException("Auction is not accepting bids at this time.");
        }

        if (auction.getSellerId().equals(bidderId)) {
            logger.warn("Auto-bid rejected: seller cannot bid. bidderId={}", bidderId);
            throw new BusinessException("Sellers are not allowed to use auto-bid on their own items.");
        }

        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            logger.warn("Auto-bid rejected: max limit must be above current price. maxLimit={}, currentPrice={}", maxLimit, auction.getCurrentPrice());
            throw new BusinessException("Max limit must be greater than the current auction price.");
        }
        
        BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0) {
            logger.warn("Auto-bid rejected: exceeds policy limit. limit={}", allowedMax);
            throw new BusinessException("Max limit exceeds the allowed maximum policy limit.");
        }

        User user = userDAO.findById(bidderId);
        if (user == null || user.getRole() != User.Role.BIDDER) {
            logger.warn("Auto-bid rejected: invalid bidder. bidderId={}", bidderId);
            throw new BusinessException("Invalid bidder account.");
        }

        // Kiểm tra xem đã có cấu hình cho phiên này chưa
        AutoBidding existing = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (existing != null) {
            if (existing.isActive()) {
                logger.warn("Auto-bid rejected: already active for this auction. bidderId={}", bidderId);
                throw new BusinessException("Auto-bid is already enabled for this auction.");
            } else {
                logger.info("Reactivating existing auto-bid configuration: configId={}", existing.getId());
                existing.setMaxLimit(maxLimit);
                existing.activate();
                if (!autoBiddingDAO.update(existing)) {
                    throw new BusinessException("Failed to reactivate auto-bid configuration.");
                }
                return AutoBidMapper.toAutoBidResponseDTO(existing);
            }
        }

        // Tạo cấu hình mới
        AutoBidding newConfig = new AutoBidding(auctionId, bidderId, maxLimit);
        if (!autoBiddingDAO.insert(newConfig)) {
            logger.error("Failed to save auto-bid to database: bidderId={}", bidderId);
            throw new BusinessException("Unable to create auto-bid configuration.");
        }

        logger.info("Auto-bid enabled successfully: configId={}", newConfig.getId());
        return AutoBidMapper.toAutoBidResponseDTO(newConfig);
    }

    /**
     * Cập nhật giới hạn tối đa cho cấu hình tự động đặt giá.
     */
    public AutoBidResponseDTO updateAutoBidding(String configId, BigDecimal maxLimit) {
        logger.info("Updating Auto-bid: configId={}, newLimit={}", configId, maxLimit);
        AutoBidding config = autoBiddingDAO.findById(configId);
        if (config == null) {
            logger.warn("Update rejected: configuration not found. configId={}", configId);
            throw new BusinessException("Auto-bid configuration does not exist.");
        }

        Auction auction = auctionDAO.findById(config.getAuctionId());
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            throw new BusinessException("Invalid auction session.");
        }

        if (maxLimit.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new BusinessException("New limit must be greater than the current price.");
        }

        BigDecimal allowedMax = com.team4.util.BidRules.allowedMaxFor(auction.getCurrentPrice());
        if (maxLimit.compareTo(allowedMax) > 0) {
            throw new BusinessException("New limit exceeds policy limits.");
        }

        config.setMaxLimit(maxLimit);
        if (!autoBiddingDAO.update(config)) {
            logger.error("Failed to update auto-bid in database: configId={}", configId);
            throw new BusinessException("Failed to update auto-bid configuration.");
        }
        
        logger.info("Auto-bid updated successfully: configId={}", configId);
        return AutoBidMapper.toAutoBidResponseDTO(config);
    }

    /**
     * Tắt tính năng tự động đặt giá.
     */
    public void disableAutoBidding(String configId) {
        logger.info("Disabling Auto-bid: configId={}", configId);
        AutoBidding config = autoBiddingDAO.findById(configId);
        if (config == null) {
            throw new BusinessException("Auto-bid configuration does not exist.");
        }
        if (!config.isActive()) {
            throw new BusinessException("Auto-bid is already disabled.");
        }
        
        config.deactivate();
        if (!autoBiddingDAO.updateActive(configId, false)) {
            logger.error("Failed to disable auto-bid in database: configId={}", configId);
            throw new BusinessException("Failed to disable auto-bid.");
        }
        logger.info("Auto-bid disabled: configId={}", configId);
    }

    /**
     * Tìm kiếm cấu hình hiện tại của một người dùng trong một phiên.
     */
    public AutoBidResponseDTO findConfig(String bidderId, String auctionId) {
        logger.debug("Finding auto-bid config: bidderId={}, auctionId={}", bidderId, auctionId);
        AutoBidding config = autoBiddingDAO.findByAuctionAndBidder(auctionId, bidderId);
        if (config == null) {
            throw new BusinessException("No auto-bid configuration found for this auction.");
        }
        return AutoBidMapper.toAutoBidResponseDTO(config);
    }

    /**
     * Lấy danh sách các cấu hình đang hoạt động trong một phiên.
     */
    public List<AutoBidResponseDTO> findActiveConfigs(String auctionId) {
        logger.debug("Loading active auto-bid configs: auctionId={}", auctionId);
        return autoBiddingDAO.findActiveByAuctionId(auctionId).stream()
                .map(AutoBidMapper::toAutoBidResponseDTO)
                .collect(Collectors.toList());
    }
}
