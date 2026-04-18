package com.team4.model;

import java.io.Serializable;

public class Collectible extends Item implements Serializable {
    private int yearOfOrigin;      // Năm sản xuất/phát hành (ví dụ: 1950)
    private String rarityLevel;    // Độ hiếm (Common, Rare, Ultra Rare, Unique)
    private String conditionGrade; // Thang điểm tình trạng (ví dụ: Mint 10, PSA 9, New)
    private String category;       // Loại đồ sưu tầm (Tiền cổ, Thẻ bài, Đồng hồ, Đồ chơi cổ)
    private boolean hasCertificate; // Có giấy chứng nhận kiểm định không?
    private String origin;         // Nguồn gốc/Xuất xứ (ví dụ: Sưu tập cá nhân, Đấu giá từ Pháp)
    private String specialFeatures; // Đặc điểm độc bản (ví dụ: Có chữ ký, Lỗi in ấn quý hiếm)

    public Collectible(String id, String name, double startingPrice, String desc,
                       int yearOfOrigin, String rarityLevel, String conditionGrade,
                       String category, boolean hasCertificate, String origin,
                       String specialFeatures) {

        super(id, name, startingPrice, desc);
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.category = category;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        this.specialFeatures = specialFeatures;
    }

    public int getYearOfOrigin() { return yearOfOrigin; }
    public String getRarityLevel() { return rarityLevel; }
    public String getConditionGrade() { return conditionGrade; }
    public String getCategory() { return category; }
    public boolean isHasCertificate() { return hasCertificate; }
    public String getOrigin() { return origin; }
    public String getSpecialFeatures() { return specialFeatures; }

    @Override
    public void showInfo() {
        System.out.println("--- [ĐỒ SƯU TẦM] ---");
        System.out.println("Sản phẩm: " + name + " | Loại: " + category);
        System.out.println("Năm: " + yearOfOrigin + " | Độ hiếm: " + rarityLevel);
        System.out.println("Tình trạng: " + conditionGrade + " | Nguồn gốc: " + origin);
        System.out.println("Kiểm định: " + (hasCertificate ? "Đã có chứng chỉ" : "Chưa kiểm định"));
        if (specialFeatures != null && !specialFeatures.isEmpty()) {
            System.out.println("Đặc điểm quý: " + specialFeatures);
        }
        System.out.println("Giá hiện tại: " + currentPrice);
    }
}