package com.team4.dao;

import com.team4.model.Item;
import java.util.List;

public interface ItemDAO {
    Item findById(String id);
    List<Item> findAll();
    List<Item> findByCategory(String category);
    List<Item> findByOwnerId(String ownerId); // Rất cần cho màn hình Quản lý của Seller

    boolean insert(Item item);
    boolean update(Item item);
    boolean delete(String id);
}
