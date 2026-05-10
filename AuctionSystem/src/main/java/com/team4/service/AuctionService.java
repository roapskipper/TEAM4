package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.team4.dao.ItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    /**
     * Tạo phiên đấu giá mới: kiểm tra item hợp lệ và thuộc về seller, tạo Auction với status PENDING, lưu DB
     */
    public Auction createAuction(String itemId, String sellerId, BigDecimal startingPrice, BigDecimal bidIncrement, LocalDateTime endTime) {
        // Kiểm tra
        logger.info("Đang tạo phiên đấu giá itemId={} sellerId={} startingPrice={} bidIncrement={} endTime={}",
                itemId, sellerId, startingPrice, bidIncrement, endTime);
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            logger.warn("Tạo phiên đấu giá thất bại vì mặt hàng không tồn tại itemId={}", itemId);
            throw new BusinessException("Mặt hàng không tồn tại");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.warn("Tạo phiên đấu giá thất bại vì người bán không sở hữu mặt hàng itemId={} sellerId={} ownerId={}",
                    itemId, sellerId, item.getOwnerId());
            throw new BusinessException("Người bán không sở hữu mặt hàng này");
        }
        Auction auction = new Auction(itemId, sellerId, startingPrice, bidIncrement, endTime);
        auctionDAO.insert(auction);
        logger.info("Đã tạo phiên đấu giá id={} itemId={} sellerId={} status={}",
                auction.getId(), itemId, sellerId, auction.getStatus());
        return auction;
    }

    /**
     * Lấy thông tin chi tiết 1 phiên đấu giá theo id
     */
    public Auction getAuctionById(String auctionId) {
        logger.debug("Đang lấy thông tin phiên đấu giá id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Không tìm thấy phiên đấu giá id={}", auctionId);
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        return auction;
    }

    /**
     * Lấy danh sách phiên theo trạng thái, dùng cho trang chủ (ACTIVE) hoặc scheduler (kiểm tra hết hạn)
     */
    public List<Auction> getAuctionsByStatus(Auction.AuctionStatus status) {
        logger.debug("Đang lấy danh sách phiên đấu giá theo trạng thái status={}", status);
        return auctionDAO.findByStatus(status);
    }

    /**
     * Admin duyệt phiên: chuyển status PENDING -> RUNNING
     */
    public Auction approveAuction(String auctionId) {
        logger.info("Đang duyệt phiên đấu giá id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Duyệt phiên đấu giá thất bại vì phiên không tồn tại id={}", auctionId);
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        // Kiêm tra trạng thái hiện tại phải là PENDING
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Duyệt phiên đấu giá thất bại vì trạng thái không phải PENDING id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Chỉ có thể duyệt cuộc đấu giá đang chờ duyệt");
        }
        // Không kiểm tra quyền admin ở đây do nguy cơ chồng chéo & khó debug
        // Kiểm tra sẽ do AdminService thực hiện
        auction.approve();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        logger.info("Đã cập nhật trạng thái phiên đấu giá id={} từ {} sang {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.RUNNING);
        return auction;
    }

    /**
     * Hủy phiên đấu giá: chuyển status -> CANCELLED
     * Dành riêng cho Super Admin
     */
    public Auction cancelAuction(String auctionId) {
        logger.info("Đang hủy phiên đấu giá id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Hủy phiên đấu giá thất bại vì phiên không tồn tại id={}", auctionId);
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        if (auction.getStatus() == Auction.AuctionStatus.PAID) {
            logger.warn("Hủy phiên đấu giá thất bại vì phiên đã thanh toán id={}", auctionId);
            throw new BusinessException("Không thể hủy cuộc đấu giá đã thanh toán");
        }
        Auction.AuctionStatus oldStatus = auction.getStatus();
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Đã cập nhật trạng thái phiên đấu giá id={} từ {} sang {}",
                auctionId, oldStatus, Auction.AuctionStatus.CANCELLED);
        return auction;
    }

    /**
     * Từ chối duyệt phiên đấu giá: chuyển status từ PENDING -> CANCELLED
     */
    public void rejectAuction(String auctionId) {
        logger.info("Đang từ chối phiên đấu giá id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Từ chối phiên đấu giá thất bại vì phiên không tồn tại id={}", auctionId);
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Từ chối phiên đấu giá thất bại vì trạng thái không phải PENDING id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Chỉ có thể từ chối cuộc đấu giá đang chờ duyệt");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Đã cập nhật trạng thái phiên đấu giá id={} từ {} sang {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.CANCELLED);
    }

    /**
     *Tự động đóng các phiên đã hết thời gian: tìm phiên ACTIVE đã quá endTime, chuyển -> CLOSED, dùng cho scheduler
     */
    public void closeExpiredAuctions() {
        logger.info("Đang đóng các phiên đấu giá đã hết hạn");
        List<Auction> activeAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        int closedCount = 0;
        for (Auction auction : activeAuctions) {
            if (auction.getEndTime().isBefore(now)) {
                auction.close();
                auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.FINISHED);
                closedCount++;
                logger.info("Đã cập nhật trạng thái phiên đấu giá id={} từ {} sang {} endTime={} closedAt={}",
                        auction.getId(), Auction.AuctionStatus.RUNNING, Auction.AuctionStatus.FINISHED,
                        auction.getEndTime(), now);
            }
        }
        logger.info("Hoàn tất đóng phiên đấu giá hết hạn checkedCount={} closedCount={}",
                activeAuctions.size(), closedCount);
    }

    /**
     * Đánh dấu phiên đã thanh toán xong: chuyển status CLOSED -> PAID, dùng khi bidder thanh toán thành công
     */
    public Auction markPaid(String auctionId) {
        logger.info("Đang đánh dấu phiên đấu giá đã thanh toán id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Đánh dấu đã thanh toán thất bại vì phiên không tồn tại id={}", auctionId);
            throw new BusinessException("Cuộc đấu giá không tồn tại");
        }
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            logger.warn("Đánh dấu đã thanh toán thất bại vì trạng thái không phải FINISHED id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Chỉ có thể đánh dấu đã thanh toán cho cuộc đấu giá đã kết thúc");
        }
        auction.markPaid();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.PAID);
        logger.info("Đã cập nhật trạng thái phiên đấu giá id={} từ {} sang {}",
                auctionId, Auction.AuctionStatus.FINISHED, Auction.AuctionStatus.PAID);
        return auction;
    }
}
