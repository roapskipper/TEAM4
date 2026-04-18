package com.team4.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User implements Serializable {
    private String storeName;            // Tên cửa hàng/Thương hiệu cá nhân
    private double rating;               // Điểm uy tín (ví dụ: 4.8/5.0 sao)
    private List<String> listedItemIds;  // Danh sách ID các món hàng ông này đang đăng bán

    public Seller(String id, String username, String password, String storeName) {

        super(id, username, password, "SELLER");
        this.storeName = storeName;
        this.rating = 5.0; // Mặc định mới tạo là 5 sao "uy tín" luôn
        this.listedItemIds = new ArrayList<>(); // Khởi tạo danh sách rỗng để đựng hàng
    }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public void addNewItem(String itemId) {
        if (!listedItemIds.contains(itemId)) {
            listedItemIds.add(itemId);
        }
    }

    public List<String> getListedItemIds() {
        return listedItemIds;
    }

    @Override
    public void displayRolePermissions() {
        System.out.println("--- [QUYỀN HẠN SELLER] ---");
        System.out.println("Cửa hàng: " + storeName + " (Chủ sở hữu: " + username + ")");
        System.out.println("- Điểm uy tín: " + rating + " / 5.0");
        System.out.println("- Có quyền: Đăng sản phẩm mới, Chỉnh sửa thông tin sản phẩm.");
        System.out.println("- Số lượng sản phẩm đang rao bán: " + listedItemIds.size());
    }
}