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
        logger.info("Creating new item for seller: sellerId={}, itemName={}, category={}", 
                sellerId, itemRequest.getName(), itemRequest.getCategory());
        
        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
            logger.warn("Item creation failed: seller does not exist or is invalid. sellerId={}", sellerId);
            throw new BusinessException("Seller does not exist.");
        }
        validateCommonItemRequest(itemRequest);
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
                logger.warn("Item creation failed: invalid item category. category={}", itemRequest.getCategory());
                throw new BusinessException("Invalid item category.");
        }
        // Tạo và lưu
        Item item;
        try {
            item = factory.createItem(itemRequest);
        } catch (IllegalArgumentException ex) {
            logger.warn("Item creation failed: invalid category specific fields. reason={}", ex.getMessage());
            throw new BusinessException(ex.getMessage());
        }
        if (!itemDAO.insert(item)) {
            logger.error("System error: unable to save item to database. sellerId={}", sellerId);
            throw new BusinessException("Unable to create item.");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            logger.error("Security/data error: creator does not match item owner. sellerId={}, ownerId={}", sellerId, item.getOwnerId());
            throw new BusinessException("ERROR: Seller is not the item owner.");
        }
        logger.info("Item created successfully: itemId={}, name={}", item.getId(), item.getName());
        return item;
    }

    /**
     * Rejects invalid common item payloads before factory construction and DB insert.
     */
    private void validateCommonItemRequest(ItemRequest itemRequest) {
        try {
            Item.validateCommonFields(
                    itemRequest.getName(),
                    itemRequest.getStartingPrice(),
                    itemRequest.getDescription(),
                    itemRequest.getCategory(),
                    itemRequest.getOwnerId());
        } catch (IllegalArgumentException ex) {
            logger.warn("Item creation failed: invalid common fields. reason={}", ex.getMessage());
            throw new BusinessException(ex.getMessage());
        }
    }

    /**
     * Cập nhật thông tin mặt hàng: kiểm tra item thuộc về seller này không, cập nhật DB
     */
    public Item updateItem(String sellerId, String itemId, String newName, String newDescription) {
        logger.info("Updating item: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Update failed: item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Update failed: seller does not own this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Ownership error.");
        }
        
        existingItem.setName(newName);
        existingItem.setDescription(newDescription);
        boolean updated = itemDAO.update(existingItem);
        if (updated) {
            logger.info("Item updated successfully: itemId={}", itemId);
        } else {
            logger.error("System error: unable to update item in database. itemId={}", itemId);
        }
        return existingItem;
    }

    /**
     * Xóa mặt hàng
     */
    public void deleteItem(String itemId, String sellerId) {
        logger.info("Deleting item: itemId={}, sellerId={}", itemId, sellerId);
        Item existingItem = itemDAO.findById(itemId);
        if (existingItem == null) {
            logger.warn("Delete failed: item does not exist. itemId={}", itemId);
            throw new BusinessException("Item does not exist.");
        }
        if (!existingItem.getOwnerId().equals(sellerId)) {
            logger.warn("Delete failed: seller does not own this item. sellerId={}, ownerId={}", sellerId, existingItem.getOwnerId());
            throw new BusinessException("Ownership error.");
        }
        
        boolean deleted = itemDAO.delete(itemId);
        if (deleted) {
            logger.info("Item deleted successfully: itemId={}", itemId);
        } else {
            logger.error("System error: unable to delete item in database. itemId={}", itemId);
        }
    }

    /**
     * Lấy danh sách mặt hàng theo danh mục, dùng cho trang lọc sản phẩm
     */
    public List<Item> getItemsByCategory(String category) {
        logger.debug("Loading items by category: category={}", category);
        return itemDAO.findByCategory(category);
    }

    /**
     * Lấy danh sách mặt hàng của 1 seller, dùng cho màn hình quản lý của seller
     */
    public List<Item> findByOwnerId(String sellerId) {
        logger.debug("Loading items owned by seller: sellerId={}", sellerId);
        return itemDAO.findByOwnerId(sellerId);
    }

    /**
     * Validate product pricing rules based on category
     */
    public void validatePrice(Item.ItemCategory category, java.math.BigDecimal price) {
        if (price == null) {
            throw new BusinessException("Starting price cannot be null.");
        }
        if (price.compareTo(new java.math.BigDecimal("50000")) < 0) {
            throw new BusinessException("Minimum starting price is 50,000 VND.");
        }
        if (price.remainder(new java.math.BigDecimal("1000")).compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new BusinessException("Price must be a multiple of 1,000 VND (no decimals).");
        }
        
        java.math.BigDecimal maxPrice;
        switch (category) {
            case ELECTRONICS:
                maxPrice = new java.math.BigDecimal("5000000000");
                break;
            case VEHICLE:
                maxPrice = new java.math.BigDecimal("10000000000");
                break;
            case ART:
            case COLLECTIBLE:
                maxPrice = new java.math.BigDecimal("2000000000");
                break;
            case FASHION:
                maxPrice = new java.math.BigDecimal("500000000");
                break;
            default:
                maxPrice = new java.math.BigDecimal("10000000000");
        }
        if (price.compareTo(maxPrice) > 0) {
            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.US);
            throw new BusinessException("Maximum starting price for " + category + " is " + formatter.format(maxPrice.longValue()) + " VND.");
        }
    }
}
