package com.team4.service;

import com.team4.dao.ItemDAO;
import com.team4.dao.UserDAO;
import com.team4.factory.*;
import com.team4.model.Item;
import com.team4.model.Seller;
import com.team4.model.User;
import com.team4.util.BusinessException;

import java.util.List;

/**
 * Mục đích: Quản lý mặt hàng trước khi đưa vào đấu giá. Dùng ItemDAO, có thể dùng UserDAO để kiểm tra owner.
 */
public class ItemService {
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
        User seller = userDAO.findById(sellerId);
        if (seller == null || !(seller instanceof Seller)) {
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
                throw new BusinessException("Loại mặt hàng không hợp lệ.");
        }
        // Tạo và lưu
        Item item = factory.createItem(itemRequest);
        if (!itemDAO.insert(item)) {
            throw new BusinessException("Không thể tạo mặt hàng.");
        }
        if (!item.getOwnerId().equals(sellerId)) {
            throw new BusinessException("LỖI: Người bán không phải chủ sở hữu mặt hàng.");
        }
        return item;
    }

    /**
     * Cập nhật thông tin mặt hàng: kiểm tra item thuộc về seller này không, cập nhật DB
     */
    public Item updateItem(String sellerId, String itemId, String newName, String newDescription) {
        if (itemDAO.findById(itemId) == null) {
            throw new BusinessException("Mặt hàng không tồn tại.");
        }
        if (!itemDAO.findById(itemId).getOwnerId().equals(sellerId)) {
            throw new BusinessException("Lỗi về quyền sở hữu.");
        }
        Item item = itemDAO.findById(itemId);
        item.setName(newName);
        item.setDescription(newDescription);
        itemDAO.update(item);
        return item;
    }

    /**
     * Xóa mặt hàng
     */
    public void deleteItem(String itemId, String sellerId) {
        if (itemDAO.findById(itemId) == null) {
            throw new BusinessException("Mặt hàng không tồn tại.");
        }
        if (!itemDAO.findById(itemId).getOwnerId().equals(sellerId)) {
            throw new BusinessException("Lỗi về quyền sở hữu.");
        }
        itemDAO.delete(itemId);
    }

    /**
     * Lấy danh sách mặt hàng theo danh mục, dùng cho trang lọc sản phẩm
     */
    public List<Item> getItemsByCategory(String category) {
        return itemDAO.findByCategory(category);
    }

    /**
     * Lấy danh sách mặt hàng của 1 seller, dùng cho màn hình quản lý của seller
     */
    public List<Item> findByOwnerId(String sellerId) {
        return itemDAO.findByOwnerId(sellerId);
    }
}
