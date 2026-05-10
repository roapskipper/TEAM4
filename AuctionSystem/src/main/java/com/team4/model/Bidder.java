package com.team4.model;

import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp Bidder - Người tham gia đấu giá.
 * Kế thừa từ User
 */
public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private String shippingAddress;
    private String phoneNumber;
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]\\d{6,14}$"
    ); // Số điện thoại quốc tế, có thể bắt đầu bằng +, theo sau là 7-15 chữ số (tùy quốc gia)
    /**
     * CONSTRUCTOR 1: Dùng khi một khách hàng đăng ký mới
     */
    public Bidder(String username, String passwordHash, String fullName, String email, String shippingAddress, String phoneNumber) {
        super(username, passwordHash, fullName, email, Role.BIDDER);
        this.shippingAddress = normalizeOptional(shippingAddress);
        this.phoneNumber = normalizeOptional(phoneNumber);
        validatePhoneNumber(this.phoneNumber);
        validateShippingAddress(this.shippingAddress);
    }

    /**
     * CONSTRUCTOR 2: Dùng khi lấy dữ liệu từ DB
     */
    public Bidder(String id, LocalDateTime creatAt, String username, String passwordHash, String fullName, String email, BigDecimal balance, String shippingAddress, String phoneNumber) {
        super(id, creatAt, username, passwordHash, fullName, email, Role.BIDDER, balance);
        this.shippingAddress = normalizeOptional(shippingAddress);
        this.phoneNumber = normalizeOptional(phoneNumber);
        validatePhoneNumber(this.phoneNumber);
        validateShippingAddress(this.shippingAddress);
    }

    // Kiểm tra định dạng của shippingAddress và phoneNumber
    private void validateShippingAddress(String shippingAddress) {
        if (shippingAddress != null) {
            String adr = shippingAddress.trim();
            if (adr.isEmpty()) {
                throw new IllegalArgumentException("Địa chỉ giao hàng không được rỗng nếu được cung cấp.");
            }
            if (adr.length() > 255) {
                throw new IllegalArgumentException("Địa chỉ giao hàng không được vượt quá 255 ký tự.");
            }
        }
    }
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber != null) {
            String num = phoneNumber.trim();
            if (!PHONE_PATTERN.matcher(num).matches()) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
            }
        }
    }
    // toString của User đã in ra thông tin cơ bản của Bidder, 2 thông tin dưới đây không nên được in ra
    // Chỉ dùng khi thật sự cần, không log ra ngoài do là thông tin cá nhân
    private String toShippingDetail() {
        return "shippingAddress: " + shippingAddress +
                " | phoneNumber: " + phoneNumber;
    }

    // Setter & Getter
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String address) { validateShippingAddress(address);
    this.shippingAddress = address;}

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { validatePhoneNumber(phoneNumber);
    this.phoneNumber = phoneNumber;}}
