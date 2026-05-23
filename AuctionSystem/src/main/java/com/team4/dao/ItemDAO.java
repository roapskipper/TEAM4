package com.team4.dao;

import com.team4.model.Item;
import java.sql.Connection;
import java.util.List;

public interface ItemDAO {
    Item findById(String id);
    List<Item> findAll();
    List<Item> findByCategory(String category);
    List<Item> findOwnedByBidderId(String bidderId);
    List<Item> findByOwnerId(String ownerId); // Rất cần cho màn hình Quản lý của Seller

    boolean insert(Item item);
    boolean insert(Connection conn, Item item);
    boolean update(Item item);
    boolean updateOwner(Connection conn, String itemId, String ownerId);
    boolean delete(String id);
}
