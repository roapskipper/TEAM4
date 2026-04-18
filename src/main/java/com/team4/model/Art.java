package com.team4.model;

import java.io.Serializable;

public class Art extends Item implements Serializable {
    private String artist;           // Tên họa sĩ hoặc tác giả sáng tạo
    private int creationYear;        // Năm tác phẩm được hoàn thành
    private String medium;           // Chất liệu nghệ thuật (Sơn dầu, Màu nước, Thạch cao...)
    private String dimensions;       // Kích thước tác phẩm (Dài x Rộng x Cao)
    private String style;            // Trường phái nghệ thuật (Ấn tượng, Trừu tượng, Phục hưng...)
    private boolean isOriginal;      // Có phải bản gốc duy nhất không? (true: Bản gốc, false: Bản sao/Limited)
    private String exhibitionHistory; // Lịch sử trưng bày tại các triển lãm lớn (nếu có)

    public Art(String id, String name, double startingPrice, String desc,
               String artist, int creationYear, String medium, String dimensions,
               String style, boolean isOriginal, String exhibitionHistory) {

        super(id, name, startingPrice, desc);
        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        this.style = style;
        this.isOriginal = isOriginal;
        this.exhibitionHistory = exhibitionHistory;
    }

    public String getArtist() { return artist; }
    public int getCreationYear() { return creationYear; }
    public String getMedium() { return medium; }
    public String getDimensions() { return dimensions; }
    public String getStyle() { return style; }
    public boolean isOriginal() { return isOriginal; }
    public String getExhibitionHistory() { return exhibitionHistory; }

    @Override
    public void showInfo() {
        System.out.println("--- [TÁC PHẨM NGHỆ THUẬT] ---");
        System.out.println("Tên: " + name + " | Tác giả: " + artist);
        System.out.println("Năm: " + creationYear + " | Chất liệu: " + medium);
        System.out.println("Kích thước: " + dimensions + " | Trường phái: " + style);
        System.out.println("Phân loại: " + (isOriginal ? "Tác phẩm GỐC" : "Bản sao có giới hạn"));

        // Chỉ in lịch sử triển lãm nếu có dữ liệu
        if (exhibitionHistory != null && !exhibitionHistory.isEmpty()) {
            System.out.println("Triển lãm đã tham gia: " + exhibitionHistory);
        }

        System.out.println("Giá đấu hiện tại: " + currentPrice + " USD");
    }
}