package com.team4.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Seller - Đại diện cho Người bán trong hệ thống.
 * Kế thừa từ User (Abstraction & Inheritance)
 */
public class Seller extends User implements Serializable {
    private String storeName;
    private double rating;
    private List<String> listedItemIds; // Lưu danh sách ID các vật phẩm đang đấu giá

    /**
     * CONSTRUCTOR 1: Dùng khi tạo một Seller mới (Đăng ký mới)
     * Tự động gán role "SELLER", rating 5.0 và khởi tạo danh sách trống.
     */
    public Seller(String username, String password, String storeName) {
        // Gọi constructor của User(String username, String password, String role)
        // Nó sẽ tự gọi tiếp Entity để tạo UUID mới.
        super(username, password, "SELLER");
        this.storeName = storeName;
        this.rating = 5.0;
        this.listedItemIds = new ArrayList<>();
    }

    /**
     * CONSTRUCTOR 2: Dùng khi DAO lấy dữ liệu từ MySQL lên
     * Chứa đầy đủ thông tin từ ID đến Balance.
     */
    public Seller(String id, String username, String password, String fullName,
                  String email, double balance, String storeName, double rating) {
        // Gọi constructor đầy đủ của lớp User
        super(id, username, password, fullName, email, "SELLER", balance);
        this.storeName = storeName;
        this.rating = rating;
        this.listedItemIds = new ArrayList<>();
    }

    // --- LOGIC NGHIỆP VỤ (BUSINESS LOGIC) ---

    public void addNewItem(String itemId) {
        if (itemId != null && !listedItemIds.contains(itemId)) {
            listedItemIds.add(itemId);
        }
    }

    public void setListedItemIds(List<String> listedItemIds) {
        this.listedItemIds = listedItemIds != null ? listedItemIds : new ArrayList<>();
    }

    public List<String> getListedItemIds() {
        return new ArrayList<>(listedItemIds); // Trả về bản sao để bảo vệ tính đóng gói
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void displayRolePermissions() {
        System.out.println("\n========== [ QUYỀN HẠN NGƯỜI BÁN ] ==========");
        System.out.println("Tên shop    : " + storeName);
        System.out.println("Chủ sở hữu  : " + username); // Biến protected từ lớp cha User
        System.out.println("ID Hệ thống : " + getId());   // Lấy từ lớp ông nội Entity
        System.out.println("Điểm uy tín : " + rating + " / 5.0");
        System.out.println("Quyền hạn   : Đăng hàng, Cập nhật giá, Quản lý kho.");
        System.out.println("Số mặt hàng : " + listedItemIds.size());
        System.out.println("==============================================\n");
    }

    // --- GETTERS & SETTERS ---

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}