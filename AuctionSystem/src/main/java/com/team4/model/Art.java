package com.team4.model;

import java.io.Serializable;

/**
 * Lớp Art - Đại diện cho tác phẩm nghệ thuật.
 * Kế thừa từ Item (Thể hiện tính Inheritance & Polymorphism)
 */
public class Art extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Các thuộc tính riêng của Nghệ thuật
    private String artist;
    private int creationYear;
    private String medium;
    private String dimensions;
    private String style;
    private boolean isOriginal;
    private String exhibitionHistory;

    /**
     * CONSTRUCTOR 1: Dùng khi Seller đăng một bức tranh mới.
     * category mặc định là "ART".
     */
    public Art(String name, double startingPrice, String desc, String ownerId,
               String artist, int creationYear, String medium, String dimensions,
               String style, boolean isOriginal, String exhibitionHistory) {

        // Gọi constructor 1 của Item (Tự sinh UUID, gán category là ART)
        super(name, startingPrice, desc, "ART", ownerId);

        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        this.style = style;
        this.isOriginal = isOriginal;
        this.exhibitionHistory = exhibitionHistory;
    }

    /**
     * CONSTRUCTOR 2: Dùng khi ItemDAO nạp từ MySQL lên (Giữ nguyên ID và CurrentPrice).
     */
    public Art(String id, String name, double startingPrice, double currentPrice,
               String desc, String ownerId, String artist, int creationYear,
               String medium, String dimensions, String style, boolean isOriginal,
               String exhibitionHistory) {

        // Gọi constructor 2 của Item
        super(id, name, startingPrice, currentPrice, desc, "ART", ownerId);

        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        this.style = style;
        this.isOriginal = isOriginal;
        this.exhibitionHistory = exhibitionHistory;
    }

    // --- TRIỂN KHAI ĐA HÌNH (POLYMORPHISM) ---

    @Override
    public void showInfo() {
        System.out.println("\n----------- [ TÁC PHẨM NGHỆ THUẬT ] -----------");
        System.out.println("Tên tác phẩm : " + getName()); // Lấy từ Item
        System.out.println("ID Sản phẩm  : " + getId());   // Lấy từ Entity
        System.out.println("Họa sĩ       : " + artist);
        System.out.println("Năm sáng tác : " + creationYear);
        System.out.println("Chất liệu    : " + medium);
        System.out.println("Trường phái  : " + style);
        System.out.println("Kích thước   : " + dimensions);
        System.out.println("Tình trạng   : " + (isOriginal ? "BẢN GỐC" : "Bản sao giới hạn"));

        if (exhibitionHistory != null && !exhibitionHistory.isEmpty()) {
            System.out.println("Triển lãm    : " + exhibitionHistory);
        }

        System.out.println("Giá khởi điểm: $" + getStartingPrice());
        System.out.println(">>> GIÁ HIỆN TẠI: $" + getCurrentPrice() + " <<<");
        System.out.println("Người sở hữu (ID): " + getOwnerId());
        System.out.println("-----------------------------------------------\n");
    }

    // --- GETTERS & SETTERS ---
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }
    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public boolean isOriginal() { return isOriginal; }
    public void setOriginal(boolean original) { isOriginal = original; }
    public String getExhibitionHistory() { return exhibitionHistory; }
    public void setExhibitionHistory(String exhibitionHistory) { this.exhibitionHistory = exhibitionHistory; }
}