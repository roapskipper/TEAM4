package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Item;

/**
 * ArtFactory - Nhà máy sản xuất các vật phẩm Nghệ thuật.
 * Thể hiện tính Abstraction và Design Pattern: Factory Method.
 */
public class ArtFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private double startingPrice;
    private String desc;
    private String ownerId; // Liên kết với Seller

    // Thuộc tính riêng của Art
    private String artist;
    private int creationYear;
    private String medium;
    private String dimensions;
    private String style;
    private boolean isOriginal;
    private String exhibitionHistory;

    /**
     * CONSTRUCTOR: Nhận đầy đủ thông số để chuẩn bị tạo vật phẩm.
     * Lưu ý: Không truyền ID ở đây, vì ID sẽ do UUID tự sinh trong Art.
     */
    public ArtFactory(String name, double startingPrice, String desc, String ownerId,
                      String artist, int creationYear, String medium, String dimensions,
                      String style, boolean isOriginal, String exhibitionHistory) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        this.style = style;
        this.isOriginal = isOriginal;
        this.exhibitionHistory = exhibitionHistory;
    }

    /**
     * THỰC THI FACTORY METHOD: Tạo ra đối tượng Art thực tế.
     */
    @Override
    public Item createItem() {
        // Sử dụng Constructor 1 của Art (Tự sinh UUID, gán category ART)
        return new Art(
                name,
                startingPrice,
                desc,
                ownerId,
                artist,
                creationYear,
                medium,
                dimensions,
                style,
                isOriginal,
                exhibitionHistory
        );
    }
}