package com.team4.model;

import java.util.UUID;

/**
 * Lớp trừu tượng Item - Đại diện cho vật phẩm đấu giá.
 * Kế thừa từ Entity để sử dụng String ID (UUID).
 */
public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentPrice; // Giá hiện tại sau khi có người bid
    protected String category;     // Loại mặt hàng
    protected String ownerId;      // ID của Seller sở hữu món hàng này

    /**
     * CONSTRUCTOR 1: Dùng khi Seller đăng một mặt hàng mới lên hệ thống.
     * Tự động sinh UUID cho Item.
     */
    public Item(String name, double startingPrice, String desc, String category, String ownerId) {
        super(UUID.randomUUID().toString()); // Sinh mã ID tự động
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // Mới đăng thì giá hiện tại = giá khởi điểm
        this.description = desc;
        this.category = category;
        this.ownerId = ownerId;
    }

    /**
     * CONSTRUCTOR 2: Dùng khi ItemDAO lấy dữ liệu từ MySQL nạp vào Java.
     */
    public Item(String id, String name, double startingPrice, double currentPrice, String desc, String category, String ownerId) {
        super(id); // Sử dụng ID cũ từ Database
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.description = desc;
        this.category = category;
        this.ownerId = ownerId;
    }

    // --- CÁC PHƯƠNG THỨC LOGIC ---

    public void updateCurrentPrice(double newBid) {
        // Business Logic: Chỉ cập nhật nếu giá mới cao hơn giá cũ
        if (newBid > this.currentPrice) {
            this.currentPrice = newBid;
            System.out.println("[SYSTEM] Giá vật phẩm '" + name + "' đã tăng lên: $" + newBid);
        } else {
            System.out.println("[ERROR] Lỗi: Giá thầu phải cao hơn giá hiện tại (" + currentPrice + ")");
        }
    }

    public abstract void showInfo(); // Thể hiện tính Abstraction

    // --- GETTERS & SETTERS (ENCAPSULATION) ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }

    public double getCurrentPrice() { return currentPrice; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}