package com.team4.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder - Người tham gia đấu giá.
 * Kế thừa từ User (Thể hiện tính Inheritance)
 */
public class Bidder extends User implements Serializable {
    private String shippingAddress;
    private String phoneNumber;
    private List<String> biddedItemIds; // Danh sách ID các món hàng đã từng đấu giá

    /**
     * CONSTRUCTOR 1: Dùng khi một khách hàng đăng ký mới
     */
    public Bidder(String username, String password, double initialBalance,
                  String shippingAddress, String phoneNumber) {
        super(username, password, "BIDDER");
        this.balance = initialBalance; // Dùng biến protected từ lớp cha User
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
        this.biddedItemIds = new ArrayList<>();
    }

    /**
     * CONSTRUCTOR 2: Dùng khi lấy dữ liệu từ MySQL
     */
    public Bidder(String id, String username, String password, String fullName,
                  String email, double balance, String shippingAddress, String phoneNumber) {
        super(id, username, password, fullName, email, "BIDDER", balance);
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
        this.biddedItemIds = new ArrayList<>();
    }

    // --- LOGIC NGHIỆP VỤ ---

    public void addBiddedItem(String itemId) {
        if (itemId != null && !biddedItemIds.contains(itemId)) {
            biddedItemIds.add(itemId);
        }
    }

    /**
     * Nạp tiền (Ghi đè hoặc dùng lại logic nạp tiền từ lớp cha)
     */
    public void addBalance(double amount) {
        deposit(amount); // Gọi phương thức nạp tiền đã viết ở lớp User
        System.out.println("[BIDDER] Nạp thành công: " + amount + "$. Số dư mới: " + balance + "$");
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void displayRolePermissions() {
        System.out.println("\n========== [ THÔNG TIN NGƯỜI ĐẤU GIÁ ] ==========");
        System.out.println("Tên User    : " + username);
        System.out.println("ID Hệ thống : " + getId());
        System.out.println("Số dư ví    : $" + balance);
        System.out.println("Địa chỉ ship: " + shippingAddress);
        System.out.println("Điện thoại  : " + phoneNumber);
        System.out.println("Quyền hạn   : Xem sản phẩm, Đặt giá thầu, Thanh toán.");
        System.out.println("Sản phẩm bid: " + biddedItemIds.size() + " mặt hàng.");
        System.out.println("==================================================\n");
    }

    // --- GETTERS & SETTERS ---

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String address) { this.shippingAddress = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public List<String> getBiddedItemIds() {
        return new ArrayList<>(biddedItemIds);
    }
}