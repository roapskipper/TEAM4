package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.factory.*;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.util.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mục đích: Quản lý mặt hàng trước khi đưa vào đấu giá. Dùng ItemDAO, có thể dùng UserDAO để kiểm tra owner.
 */
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public ItemService(ItemDAO itemDAO, UserDAO userDAO) {
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /**
     * Tạo mặt hàng mới: kiểm tra seller có tồn tại và đúng role không, tạo object Item qua Factory, lưu xuống DB
     */
    public Item createItem(String sellerId, ItemRequest itemRequest) {
        logger.info("Đang tạo mặt hàng mới cho người bán: sellerId={}, itemName={}, category={}", 
                sellerId, itemRequest.getName(), itemRequest.getCategory());
        
        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
            logger.warn("Tạo mặt hàng thất bại: Người bán không tồn tại hoặc không hợp lệ. sellerId={}", sellerId);
            throw new BusinessException("Người bán không tồn tại.");
        }
        // Chọn Factory
        ItemFactory factory;
        switch (itemRequest.getCategory()) {
            case ART:
                factory = new ArtFactory();
                break;
            case COLLECTIBLE:
                factory = new CollectibleFactory();
                break;
            case ELECTRONICS:
                factory = new ElectronicsFactory();
                break;
            case FASHION:
                factory = new FashionFactory();
                break;
            case VEHICLE:
                factory = new VehicleFactory();
                break;
            default:
                logger.warn("Tạo mặt hàng thất bại: Loại mặt hàng không hợp lệ. category={}", itemRequest.getCategory());
                throw new BusinessException("Loại mặt hàng không hợp lệ.");
        }
        // Tạo và lưu
        Item item = factory.createItem(itemRequest);
        if (!itemDAO.insert(item)) {
            logger.error("Lỗi hệ thống: Không thể lưu mặt hàng vào database. sellerId={}", sellerId);
            throw new BusinessException("Không thể tạo mặt hàng.");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.error("Lỗi bảo mật/dữ liệu: Người tạo không khớp với người sở hữu mặt hàng. sellerId={}, ownerId={}", sellerId, item.getOwnerId());
            throw new BusinessException("LỖI: Người bán không phải chủ sở hữu mặt hàng.");
        }
        logger.info("Đã tạo thành công mặt hàng: itemId={}, name={}", item.getId(), item.getName());
        return item;
    }

    /**
     * Cập nhật thông tin mặt hàng: kiểm tra item thuộc về seller này không, cập nhật DB
     */
    public Item updateItem(String sellerId, String itemId, String newName, String newDescription) {
        logger.info("Đang cập nhật mặt hàng: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Cập nhật thất bại: Mặt hàng không tồn tại. itemId={}", itemId);
            throw new BusinessException("Mặt hàng không tồn tại.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Cập nhật thất bại: Người bán không có quyền sở hữu mặt hàng này. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Lỗi về quyền sở hữu.");
        }
        
        existingItem.setName(newName);
        existingItem.setDescription(newDescription);
        boolean updated = itemDAO.update(existingItem);
        if (updated) {
            logger.info("Đã cập nhật thành công mặt hàng: itemId={}", itemId);
        } else {
            logger.error("Lỗi hệ thống: Không thể cập nhật mặt hàng vào database. itemId={}", itemId);
        }
        return existingItem;
    }

    /**
     * Xóa mặt hàng
     */
    public void deleteItem(String itemId, String sellerId) {
        logger.info("Đang xóa mặt hàng: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Xóa thất bại: Mặt hàng không tồn tại. itemId={}", itemId);
            throw new BusinessException("Mặt hàng không tồn tại.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Xóa thất bại: Người bán không có quyền sở hữu mặt hàng này. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Lỗi về quyền sở hữu.");
        }
        
        boolean deleted = itemDAO.delete(itemId);
        if (deleted) {
            logger.info("Đã xóa thành công mặt hàng: itemId={}", itemId);
        } else {
            logger.error("Lỗi hệ thống: Không thể xóa mặt hàng trong database. itemId={}", itemId);
        }
    }

    /**
     * Lấy danh sách mặt hàng theo danh mục, dùng cho trang lọc sản phẩm
     */
    public List<Item> getItemsByCategory(String category) {
        logger.debug("Đang lấy danh sách mặt hàng theo danh mục: category={}", category);
        return itemDAO.findByCategory(category);
    }

    /**
     * Lấy danh sách mặt hàng của 1 seller, dùng cho màn hình quản lý của seller
     */
    public List<Item> findByOwnerId(String sellerId) {
        logger.debug("Đang lấy danh sách mặt hàng của người sở hữu: sellerId={}", sellerId);
        return itemDAO.findByOwnerId(sellerId);
    }
}
