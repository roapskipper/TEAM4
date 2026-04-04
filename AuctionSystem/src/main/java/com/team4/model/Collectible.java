package com.team4.model;

import java.io.Serializable;

/**
 * Lớp Collectible - Đại diện cho đồ sưu tầm quý hiếm.
 * Kế thừa từ Item (Tính Inheritance và Polymorphism).
 */
public class Collectible extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính riêng của Đồ sưu tầm
    private int yearOfOrigin;
    private String rarityLevel;   // Hiếm, Rất hiếm, Duy nhất
    private String conditionGrade;// Điểm bảo quản (VD: 9/10)
    private String categorySpecific; // Phân loại nhỏ (Tiền cổ, Tem, Đồng hồ...)
    private boolean hasCertificate;
    private String origin;
    private String specialFeatures;

    /**
     * CONSTRUCTOR 1: Khớp với CollectibleFactory (Giải quyết lỗi đỏ trên IDE)
     */
    public Collectible(String name, double startingPrice, String desc, String ownerId,
                       int yearOfOrigin, String rarityLevel, String conditionGrade,
                       boolean hasCertificate, String origin, String specialFeatures) {

        // Gọi super() lên Item để tự sinh UUID và gán category tổng là "COLLECTIBLE"
        super(name, startingPrice, desc, "COLLECTIBLE", ownerId);

        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        this.specialFeatures = specialFeatures;
    }

    /**
     * CONSTRUCTOR 2: Dùng cho Database mapping (Dành cho chặng sau)
     */
    public Collectible(String id, String name, double startingPrice, double currentPrice,
                       String desc, String ownerId, int yearOfOrigin, String rarityLevel,
                       String conditionGrade, boolean hasCertificate, String origin,
                       String specialFeatures) {

        super(id, name, startingPrice, currentPrice, desc, "COLLECTIBLE", ownerId);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        this.specialFeatures = specialFeatures;
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void showInfo() {
        System.out.println("\n----------- [ VẬT PHẨM SƯU TẦM ] -----------");
        System.out.println("Tên vật phẩm : " + getName() + " (ID: " + getId() + ")");
        System.out.println("Năm sản xuất : " + yearOfOrigin + " | Xuất xứ: " + origin);
        System.out.println("Độ hiếm     : " + rarityLevel);
        System.out.println("Tình trạng   : Grade " + conditionGrade);
        System.out.println("Xác minh     : " + (hasCertificate ? "ĐÃ CÓ CHỨNG CHỈ KIỂM ĐỊNH ✔" : "Chưa có chứng chỉ ⚠"));

        if (specialFeatures != null && !specialFeatures.isEmpty()) {
            System.out.println("Đặc điểm quý : " + specialFeatures);
        }

        System.out.println("----------------------------------------------");
        System.out.println("GIÁ HIỆN TẠI : $" + getCurrentPrice());
        System.out.println("Người sở hữu : Seller-" + getOwnerId());
        System.out.println("----------------------------------------------\n");
    }

    // --- GETTERS & SETTERS (ENCAPSULATION) ---
    public int getYearOfOrigin() { return yearOfOrigin; }
    public String getRarityLevel() { return rarityLevel; }
    public String getConditionGrade() { return conditionGrade; }
    public boolean isHasCertificate() { return hasCertificate; }
    public String getOrigin() { return origin; }
    public String getSpecialFeatures() { return specialFeatures; }
}