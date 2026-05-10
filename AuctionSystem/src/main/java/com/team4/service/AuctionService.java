package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.dao.UserDAO;
import com.team4.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.team4.dao.ItemDAO;

import com.team4.model.Item;
import com.team4.util.BusinessException;

/**
 * Mục đích: Quản lý vòng đời phiên đấu giá. Dùng AuctionDAO, ItemDAO, có thể dùng UserDAO.
 * Không nên để file này trực tiếp xử lý một lần bid đầy đủ.
 */
public class AuctionService {
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
    }

    /**
     * Tạo phiên đấu giá mới: kiểm tra item hợp lệ và thuộc về seller, tạo Auction với status PENDING, lưu DB
     */
    public Auction createAuction(String itemId, String sellerId, BigDecimal startingPrice, BigDecimal bidIncrement, LocalDateTime endTime) {
        // Kiểm tra
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new BusinessException("Mặt hàng không tồn tại");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            throw new BusinessException("Người bán không sở hữu mặt hàng này");
        }
        Auction auction = new Auction(itemId, sellerId, startingPrice, bidIncrement, endTime);
        auctionDAO.insert(auction);
        return auction;
    }

    /**
     * Lấy thông tin chi tiết 1 phiên đấu giá theo id
     */
    public Auction getAuctionById(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        return auction;
    }

    /**
     * Lấy danh sách phiên theo trạng thái, dùng cho trang chủ (ACTIVE) hoặc scheduler (kiểm tra hết hạn)
     */
    public List<Auction> getAuctionsByStatus(Auction.AuctionStatus status) {
        return auctionDAO.findByStatus(status);
    }

    /**
     * Admin duyệt phiên: chuyển status PENDING -> RUNNING
     */
    public Auction approveAuction(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        // Kiêm tra trạng thái hiện tại phải là PENDING
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            throw new BusinessException("Chỉ có thể duyệt cuộc đấu giá đang chờ duyệt");
        }
        // Không kiểm tra quyền admin ở đây do nguy cơ chồng chéo & khó debug
        // Kiểm tra sẽ do AdminService thực hiện
        auction.approve();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        return auction;
    }

    /**
     * Hủy phiên đấu giá: chuyển status → CANCELLED
     */
    public Auction cancelAuction(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        if (auction.getStatus() == Auction.AuctionStatus.PAID) {
            throw new BusinessException("Không thể hủy cuộc đấu giá đã thanh toán");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        return auction;
    }

    /**
     *Tự động đóng các phiên đã hết thời gian: tìm phiên ACTIVE đã quá endTime, chuyển -> CLOSED, dùng cho scheduler
     */
    public void closeExpiredAuctions() {
        List<Auction> activeAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions) {
            if (!auction.getEndTime().isAfter(now)) {
                auction.close();
                auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.FINISHED);
            }
        }
    }

    /**
     * Đánh dấu phiên đã thanh toán xong: chuyển status CLOSED -> PAID, dùng khi bidder thanh toán thành công
     */
    public Auction markPaid(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            throw new BusinessException("Chỉ có thể đánh dấu đã thanh toán cho cuộc đấu giá đã kết thúc");
        }
        auction.markPaid();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.PAID);
        return auction;
    }
}
