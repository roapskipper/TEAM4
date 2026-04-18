package com.team4.model;

import java.io.Serializable;
import java.util.ArrayList; // danh sách cụ thể (Class) (Khởi tạo ArrayList<>())
import java.util.List; //Interface danh sách

public class Bidder extends User implements Serializable {
    private double balance;              // Số dư tài khoản (tiền để đi đấu giá)
    private String shippingAddress;      // Địa chỉ nhận hàng nếu thắng cuộc
    private String phoneNumber;          // Số điện thoại liên lạc
    private List<String> biddedItemIds;  // Danh sách ID các món hàng đã từng tham gia trả giá

    public Bidder(String id, String username, String password, double initialBalance,
                  String shippingAddress, String phoneNumber) {

        super(id, username, password, "BIDDER");
        this.balance = initialBalance;
        this.shippingAddress = shippingAddress;
        this.phoneNumber = phoneNumber;
        this.biddedItemIds = new ArrayList<>(); // Khởi tạo danh sách rỗng
    }

    public double getBalance() { return balance; }

    public void addBalance(double amount) {
        if (amount > 0) {
            this.balance += amount;
            System.out.println("Nạp tiền thành công! Số dư mới: " + balance);
        }
    }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String address) { this.shippingAddress = address; }

    public String getPhoneNumber() { return phoneNumber; }

    public void addBiddedItem(String itemId) {
        if (!biddedItemIds.contains(itemId)) {
            biddedItemIds.add(itemId);
        }
    }

    @Override
    public void displayRolePermissions() {
        System.out.println("--- [QUYỀN HẠN BIDDER] ---");
        System.out.println("User: " + username);
        System.out.println("- Có quyền xem danh sách tất cả sản phẩm.");
        System.out.println("- Có quyền đặt giá thầu (Bid) cho sản phẩm.");
        System.out.println("- Số dư hiện có: " + balance + " USD");
        System.out.println("- Số lượng sản phẩm đã quan tâm: " + biddedItemIds.size());
    }
}