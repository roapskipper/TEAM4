package com.team4.service;

import com.team4.dao.AuctionDAO;
import com.team4.model.Auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.team4.mapper.AuctionMapper;

import java.util.ArrayList;
import java.util.List;
import com.team4.dao.ItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team4.model.Item;
import com.team4.util.BusinessException;
import com.team4.dto.auction.CreateAuctionRequestDTO;
import com.team4.dto.auction.AuctionResponseDTO;

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
    public Auction createAuction(CreateAuctionRequestDTO createAuctionRequestDTO) {
        String itemId            = createAuctionRequestDTO.getItemId();
        String sellerId          = createAuctionRequestDTO.getSellerId();
        BigDecimal startingPrice = createAuctionRequestDTO.getStartingPrice();
        BigDecimal bidIncrement  = createAuctionRequestDTO.getBidIncrement();
        LocalDateTime endTime    = createAuctionRequestDTO.getEndTime();
        // Kiểm tra
        logger.info("Creating auction itemId={} sellerId={} startingPrice={} bidIncrement={} endTime={}",
                itemId, sellerId, startingPrice, bidIncrement, endTime);
        Item item = itemDAO.findById(itemId);
        if (item == null) {
            logger.warn("Auction creation failed because item does not exist itemId={}", itemId);
            throw new BusinessException("Item does not exist");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.warn("Auction creation failed because seller does not own the item itemId={} sellerId={} ownerId={}",
                    itemId, sellerId, item.getOwnerId());
            throw new BusinessException("Seller does not own this item");
        }
        Auction auction = new Auction(itemId, sellerId, startingPrice, bidIncrement, endTime);
        auctionDAO.insert(auction);
        logger.info("Auction created id={} itemId={} sellerId={} status={}",
                auction.getId(), itemId, sellerId, auction.getStatus());
        return auction;
    }

    /**
     * Phương thức chuyển model thành dto
     * Hoạt động khi người dùng gọi response dto
     */
    public AuctionResponseDTO toAuctionResponseDTO(String auctionId) {
        Auction auction = getAuctionById(auctionId);
        return AuctionMapper.toAuctionResponseDTO(auction);
    }
    /**
     * Lấy thông tin chi tiết 1 phiên đấu giá theo id
     */
    public Auction getAuctionById(String auctionId) {
        logger.debug("Loading auction details id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Auction not found id={}", auctionId);
            throw new BusinessException("Auction does not exist");
        }
        return auction;
    }
    /**
     * Lấy danh sách Auction theo status
     * Dùng cho đội ngũ admin
     */
    public List<Auction> getAuctionByStatus(Auction.AuctionStatus status) {
        logger.debug("Đang lấy danh sách phiên đấu giá theo trạng thái status={}", status);
        return auctionDAO.findByStatus(status);

    }
    /**
     * Lấy danh AuctionResponseDTO sách phiên theo trạng thái
     * Dungf cho client
     */
    public List<AuctionResponseDTO> getAuctionResponseDTOByStatus(Auction.AuctionStatus status) {
        logger.debug("Đang lấy danh sách phiên đấu giá theo trạng thái status={}", status);
        List<Auction> auctionList = auctionDAO.findByStatus(status);
        List<AuctionResponseDTO> auctionResponseDTOList = new ArrayList<>();
        for  (Auction auction : auctionList) {
            auctionResponseDTOList.add(AuctionMapper.toAuctionResponseDTO(auction));
        }
        return auctionResponseDTOList;
    }

    /**
     * Admin duyệt phiên: chuyển status PENDING -> RUNNING
     */
    public Auction approveAuction(String auctionId) {
        logger.info("Approving auction id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Auction approval failed because auction does not exist id={}", auctionId);
            throw new BusinessException("Auction does not exist");
        }
        // Kiêm tra trạng thái hiện tại phải là PENDING
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Auction approval failed because status is not PENDING id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only pending auctions can be approved");
        }
        // Không kiểm tra quyền admin ở đây do nguy cơ chồng chéo & khó debug
        // Kiểm tra sẽ do AdminService thực hiện
        auction.approve();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.RUNNING);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.RUNNING);
        return auction;
    }

    /**
     * Hủy phiên đấu giá: chuyển status -> CANCELLED
     * Dành riêng cho Super Admin
     */
    public Auction cancelAuction(String auctionId) {
        logger.info("Cancelling auction id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Auction cancellation failed because auction does not exist id={}", auctionId);
            throw new BusinessException("Auction does not exist");
        }
        if (auction.getStatus() == Auction.AuctionStatus.PAID) {
            logger.warn("Auction cancellation failed because auction has already been paid id={}", auctionId);
            throw new BusinessException("Cannot cancel a paid auction");
        }
        Auction.AuctionStatus oldStatus = auction.getStatus();
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, oldStatus, Auction.AuctionStatus.CANCELLED);
        return auction;
    }

    /**
     * Từ chối duyệt phiên đấu giá: chuyển status từ PENDING -> CANCELLED
     */
    public void rejectAuction(String auctionId) {
        logger.info("Rejecting auction id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Auction rejection failed because auction does not exist id={}", auctionId);
            throw new BusinessException("Auction does not exist");
        }
        if (auction.getStatus() != Auction.AuctionStatus.PENDING) {
            logger.warn("Auction rejection failed because status is not PENDING id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only pending auctions can be rejected");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.PENDING, Auction.AuctionStatus.CANCELLED);
    }

    /**
     *Tự động đóng các phiên đã hết thời gian: tìm phiên ACTIVE đã quá endTime, chuyển -> CLOSED, dùng cho scheduler
     */
    public void closeExpiredAuctions() {
        logger.info("Closing expired auctions");
        List<Auction> activeAuctions = auctionDAO.findByStatus(Auction.AuctionStatus.RUNNING);
        LocalDateTime now = LocalDateTime.now();
        int closedCount = 0;
        for (Auction auction : activeAuctions) {
            if (!auction.getEndTime().isAfter(now)) {
                auction.close();
                auctionDAO.updateStatus(auction.getId(), Auction.AuctionStatus.FINISHED);
                closedCount++;
                logger.info("Updated auction status id={} from {} to {} endTime={} closedAt={}",
                        auction.getId(), Auction.AuctionStatus.RUNNING, Auction.AuctionStatus.FINISHED,
                        auction.getEndTime(), now);
            }
        }
        logger.info("Finished closing expired auctions checkedCount={} closedCount={}",
                activeAuctions.size(), closedCount);
    }

    /**
     * Đánh dấu phiên đã thanh toán xong: chuyển status CLOSED -> PAID, dùng khi bidder thanh toán thành công
     */
    public Auction markPaid(String auctionId) {
        logger.info("Marking auction as paid id={}", auctionId);
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            logger.warn("Mark paid failed because auction does not exist id={}", auctionId);
            throw new BusinessException("Auction does not exist");
        }
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            logger.warn("Mark paid failed because status is not FINISHED id={} currentStatus={}",
                    auctionId, auction.getStatus());
            throw new BusinessException("Only finished auctions can be marked as paid");
        }
        auction.markPaid();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.PAID);
        logger.info("Updated auction status id={} from {} to {}",
                auctionId, Auction.AuctionStatus.FINISHED, Auction.AuctionStatus.PAID);
        return auction;
    }

    /**
     * Hủy phiên đấu giá do người thắng không đủ tiền
     */
    public Auction cancelDueToInsufficientFunds(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) {
            throw new BusinessException("Auction does not exist");
        }
        if (auction.getStatus() != Auction.AuctionStatus.FINISHED) {
            throw new BusinessException("Only finished auctions can be cancelled");
        }
        auction.cancel();
        auctionDAO.updateStatus(auctionId, Auction.AuctionStatus.CANCELLED);
        return auction;
    }
}
