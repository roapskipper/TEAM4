package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.BidTransactionDAO;
import com.team4.dao.UserDAO;
import com.team4.db.DatabaseManager;
import com.team4.model.Auction;
import com.team4.model.BidTransaction;
import com.team4.model.Bidder;
import com.team4.model.User;
import com.team4.util.BusinessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BiddingService {
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final UserDAO userDAO;

    public BiddingService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO, UserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
        this.userDAO = userDAO;
    }

    /**
     * Đặt giá: kiểm tra phiên còn active không, kiểm tra bidder không phải seller, kiểm tra số dư đủ không
     * kiểm tra amount hợp lệ, cập nhật currentPrice + ghi BidTransaction trong 1 transaction
     */
    public void placeBid(String auctionId, String bidderB_Id, BigDecimal amountB) {
        // Kiểm tra
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
            throw new BusinessException("Phiên đấu giá không tồn tại hoặc đã kết thúc.");
        }
        User bidderB = userDAO.findById(bidderB_Id);
        if (!bidderB.getRole().equals(User.Role.BIDDER)) {
            throw new BusinessException("Chỉ người mua mới được phép đặt giá.");
        }
        if (auction.getSellerId().equals(bidderB_Id)) {
            throw new BusinessException("Người bán không được phép đặt giá trên chính phiên đấu giá của mình.");
        }
        if (bidderB.getBalance().compareTo(amountB) < 0) {
            throw new BusinessException("Số dư không đủ để đặt giá.");
        }
        String bidderA_Id = auction.getCurrentHighestBidderId(); // Lấy ID người cũ
        BigDecimal amountA = auction.getCurrentPrice();
        if (amountB.compareTo(amountA.add(auction.getBidIncrement())) < 0) {
            throw new BusinessException("Giá đặt phải cao hơn giá hiện tại ít nhất bằng bước giá.");
        }

        // Xử lý Transaction
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                DatabaseManager.beginTransaction(conn); // Tắt autocommit
                // Trừ tiền B
                userDAO.updateBalance(conn, bidderB_Id, userDAO.findById(bidderB_Id).getBalance().subtract(amountB));
                // Cộng tiền A nếu có tồn tại
                if (bidderA_Id != null) {
                    User bidderA = userDAO.findById(bidderA_Id);
                    if (bidderA != null) {
                        userDAO.updateBalance(conn, bidderA_Id, bidderA.getBalance().add(amountA));
                    }
                }
                // Cập nhật phiên đấu giá
                auctionDAO.updateCurrentBid(conn, auctionId, amountB, bidderB_Id);
                // Tạo bid
                bidTransactionDAO.insert(conn, new BidTransaction(auctionId, bidderB_Id, amountB));
                // commit
                DatabaseManager.commitTransaction(conn);
            } catch (Exception e) {
                DatabaseManager.rollbackTransaction(conn);
                throw new BusinessException("Đặt giá thất bại: " + e.getMessage());
            }
        } catch (SQLException e) {
            // Xử lý lỗi kết nối hoặc đóng kết nối
            throw new BusinessException("Lỗi hệ thống: " + e.getMessage());
        }
    }
    /**
     * Lấy toàn bộ lịch sử đặt giá của 1 phiên, dùng để hiển thị lịch sử cho người xem
     */
    public List<BidTransaction> getBidHistoryByAuction(String auctionId) {
        return bidTransactionDAO.findByAuctionId(auctionId);
    }
    /**
     * Lấy lịch sử đặt giá của 1 bidder, dùng cho trang lịch sử cá nhân
     */
    public List<BidTransaction> getBidHistoryByBidder(String bidderId) {
        return bidTransactionDAO.findByBidderId(bidderId);
    }
    /**
     * Lấy lần bid cao nhất hiện tại của phiên, dùng để xác định người thắng khi phiên kết thúc
     */
    public BidTransaction getBidHistoryByBidderAndAuction(String auctionId) {
        return bidTransactionDAO.getHighestBid(auctionId);
    }
}