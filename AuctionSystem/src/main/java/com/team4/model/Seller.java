package com.team4.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User implements Serializable {
    private String storeName;
    private double rating;
    private List<String> listedItemIds;

    public Seller(String id, String username, String password, String storeName) {
        super(id, username, password, "SELLER");
        this.storeName = storeName;
        this.rating = 5.0;
        this.listedItemIds = new ArrayList<>();
    }
    public void addNewItem(String itemId) {
        if (!listedItemIds.contains(itemId)) {
            listedItemIds.add(itemId);
        }
    }

    @Override
    public void displayRolePermissions() {
        System.out.println("--- [QUYỀN HẠN SELLER] ---");
        System.out.println("Cửa hàng: " + storeName + " (Chủ sở hữu: " + username + ")");
        System.out.println("ID Hệ Thống: " + id); // Đã có UUID
        System.out.println("- Điểm uy tín: " + rating + " / 5.0");
        System.out.println("- Quyền: Đăng sản phẩm, Chỉnh sửa thông tin sản phẩm.");
    }

    // Getter/Setter cho storeName, rating...
    public String getStoreName() { return storeName; }
    public double getRating() { return rating; }
}