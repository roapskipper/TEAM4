package com.team4.factory;

import com.team4.model.*;
import java.math.BigDecimal;

public class ItemRequest {
    // Thuộc tính chung
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private Item.ItemCategory category;
    private String ownerId;

    // Art
    private String artist;
    private int creationYear;
    private Art.Medium medium;
    private String dimensions;

    // Collectible
    private int yearOfOrigin;       // năm xuất xứ
    private Collectible.RarityLevel rarityLevel;     // độ hiếm
    private Collectible.ConditionGrade conditionGrade; // tình trạng
    private boolean hasCertificate;  // có chứng chỉ không
    private String origin;           // xuất xứ (quốc gia/vùng)

    // Electronics
    private String brand;           // thương hiệu
    private String model;           // tên model
    private Electronics.ConditionGrade itemCondition;       // tình trạng
    private int warrantyMonths;     // bảo hành (tháng)
    private boolean fullyFunctional; // hoạt động đầy đủ

    // Fashion
    private Fashion.Size size;
    private String material;
    private String color;
    private Fashion.Gender gender;           // giới tính/đối tượng
    private Fashion.ConditionGrade condition; // tình trạng
    private boolean authentic;       // is_authentic

    // Vehicle
    private int manufacturingYear;  // năm sản xuất
    private int odo;                // số km đã đi
    private Vehicle.EngineType engineType;      // loại động cơ
    private boolean hasLegalPapers; // có giấy tờ pháp lý
    private Vehicle.Transmission transmission; // hộp số

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public String getArtist() {
        return artist;
    }
    public int getCreationYear() {
        return creationYear;
    }
    public Art.Medium getMedium() {
        return medium;
    }
    public String getDimensions() {
        return dimensions;
    }
    public int getYearOfOrigin() {
        return yearOfOrigin;
    }
    public Collectible.RarityLevel getRarityLevel() {
        return rarityLevel;
    }
    public Collectible.ConditionGrade getConditionGrade() {
        return conditionGrade;
    }
    public boolean isHasCertificate() {
        return hasCertificate;
    }
    public String getOrigin() {
        return origin;
    }
    public String getBrand() {
        return brand;
    }
    public String getModel() {
        return model;
    }
    public Electronics.ConditionGrade getItemCondition() {
        return itemCondition;
    }
    public int getWarrantyMonths() {
        return warrantyMonths;
    }
    public boolean isFullyFunctional() {
        return fullyFunctional;
    }
    public Fashion.Size getSize() {
        return size;
    }
    public String getMaterial() {
        return material;
    }
    public String getColor() {
        return color;
    }
    public Fashion.Gender getGender() {
        return gender;
    }
    public Fashion.ConditionGrade getCondition() {
        return condition;
    }
    public boolean isAuthentic() {
        return authentic;
    }
    public int getManufacturingYear() {
        return manufacturingYear;
    }
    public int getOdo() {
        return odo;
    }
    public Vehicle.EngineType getEngineType() {
        return engineType;
    }
    public boolean isHasLegalPapers() {
        return hasLegalPapers;
    }
    public Vehicle.Transmission getTransmission() {
        return transmission;
    }
    public Item.ItemCategory getCategory() {
        return category;
    }
}
