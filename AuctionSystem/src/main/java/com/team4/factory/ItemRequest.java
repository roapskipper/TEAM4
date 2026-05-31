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
    private java.time.LocalDateTime startTime;
    private java.time.LocalDateTime endTime;

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

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public String getOwnerId() { return ownerId; }
    public String getArtist() { return artist; }
    public int getCreationYear() { return creationYear; }
    public Art.Medium getMedium() { return medium; }
    public String getDimensions() { return dimensions; }
    public int getYearOfOrigin() { return yearOfOrigin; }
    public Collectible.RarityLevel getRarityLevel() { return rarityLevel; }
    public Collectible.ConditionGrade getConditionGrade() { return conditionGrade; }
    public boolean isHasCertificate() { return hasCertificate; }
    public String getOrigin() { return origin; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Electronics.ConditionGrade getItemCondition() { return itemCondition; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public boolean isFullyFunctional() { return fullyFunctional; }
    public Fashion.Size getSize() { return size; }
    public String getMaterial() { return material; }
    public String getColor() { return color; }
    public Fashion.Gender getGender() { return gender; }
    public Fashion.ConditionGrade getCondition() { return condition; }
    public boolean isAuthentic() { return authentic; }
    public int getManufacturingYear() { return manufacturingYear; }
    public int getOdo() { return odo; }
    public Vehicle.EngineType getEngineType() { return engineType; }
    public boolean isHasLegalPapers() { return hasLegalPapers; }
    public Vehicle.Transmission getTransmission() { return transmission; }
    public Item.ItemCategory getCategory() { return category; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }
    public void setCategory(Item.ItemCategory category) { this.category = category; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }
    public void setMedium(Art.Medium medium) { this.medium = medium; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public void setYearOfOrigin(int yearOfOrigin) { this.yearOfOrigin = yearOfOrigin; }
    public void setRarityLevel(Collectible.RarityLevel rarityLevel) { this.rarityLevel = rarityLevel; }
    public void setConditionGrade(Collectible.ConditionGrade conditionGrade) { this.conditionGrade = conditionGrade; }
    public void setHasCertificate(boolean hasCertificate) { this.hasCertificate = hasCertificate; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setModel(String model) { this.model = model; }
    public void setItemCondition(Electronics.ConditionGrade itemCondition) { this.itemCondition = itemCondition; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public void setFullyFunctional(boolean fullyFunctional) { this.fullyFunctional = fullyFunctional; }
    public void setSize(Fashion.Size size) { this.size = size; }
    public void setMaterial(String material) { this.material = material; }
    public void setColor(String color) { this.color = color; }
    public void setGender(Fashion.Gender gender) { this.gender = gender; }
    public void setCondition(Fashion.ConditionGrade condition) { this.condition = condition; }
    public void setAuthentic(boolean authentic) { this.authentic = authentic; }
    public void setManufacturingYear(int manufacturingYear) { this.manufacturingYear = manufacturingYear; }
    public void setOdo(int odo) { this.odo = odo; }
    public void setEngineType(Vehicle.EngineType engineType) { this.engineType = engineType; }
    public void setHasLegalPapers(boolean hasLegalPapers) { this.hasLegalPapers = hasLegalPapers; }
    public void setTransmission(Vehicle.Transmission transmission) { this.transmission = transmission; }

    public java.time.LocalDateTime getStartTime() { return startTime; }
    public java.time.LocalDateTime getEndTime() { return endTime; }
    public void setStartTime(java.time.LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(java.time.LocalDateTime endTime) { this.endTime = endTime; }
}
