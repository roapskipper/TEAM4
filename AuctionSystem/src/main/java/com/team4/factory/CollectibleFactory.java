package com.team4.factory;

import com.team4.model.Collectible;
import com.team4.model.Item;

/**
 * CollectibleFactory - Nhà máy sản xuất vật phẩm sưu tầm (Tiền cổ, Tem, Đồ cổ).
 * Áp dụng Factory Method Design Pattern.
 */
public class CollectibleFactory implements ItemFactory {
    // Thuộc tính cơ bản (Inherited from Item)
    private String name;
    private double startingPrice;
    private String desc;
    private String ownerId; // Liên kết tới Nhà sưu tầm (Seller)

    // Thuộc tính riêng (Collectible Specific)
    private int yearOfOrigin;
    private String rarityLevel;   // Hiếm, Rất hiếm, Duy nhất
    private String conditionGrade;// Thang điểm bảo quản (ví dụ: 9.5/10)
    private boolean hasCertificate; // Chứng nhận đồ cổ thật
    private String origin;        // Xuất xứ (Quốc gia/Vương triều)
    private String specialFeatures;

    /**
     * CONSTRUCTOR: Chuẩn bị nguyên vật liệu để tạo vật phẩm sưu tầm.
     * Lưu ý: Không cần truyền ID, hệ thống UUID sẽ lo liệu.
     */
    public CollectibleFactory(String name, double startingPrice, String desc, String ownerId,
                              int yearOfOrigin, String rarityLevel, String conditionGrade,
                              boolean hasCertificate, String origin, String specialFeatures) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.yearOfOrigin = yearOfOrigin;
        this.rarityLevel = rarityLevel;
        this.conditionGrade = conditionGrade;
        this.hasCertificate = hasCertificate;
        this.origin = origin;
        this.specialFeatures = specialFeatures;
    }

    /**
     * SẢN XUẤT: Khởi tạo đối tượng Collectible cụ thể.
     * Category sẽ tự động được gán là "COLLECTIBLE".
     */
    @Override
    public Item createItem() {
        return new Collectible(
                name,
                startingPrice,
                desc,
                ownerId,
                yearOfOrigin,
                rarityLevel,
                conditionGrade,
                hasCertificate,
                origin,
                specialFeatures
        );
    }
}