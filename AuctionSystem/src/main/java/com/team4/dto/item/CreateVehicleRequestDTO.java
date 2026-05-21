package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Item;
import com.team4.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateVehicleRequestDTO extends CreateItemRequestDTO {
    private String brand;           // thương hiệu
    private String model;           // tên model
    private int manufacturingYear;  // năm sản xuất
    private int odo;                // số km đã đi
    private Vehicle.EngineType engineType;      // loại động cơ
    private String color;           // màu sắc
    private boolean hasLegalPapers; // có giấy tờ pháp lý
    private Vehicle.Transmission transmission; // hộp số

    public CreateVehicleRequestDTO() {}

    public CreateVehicleRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category,
                                   String brand, String model, int manufacturingYear, int odo,
                                   Vehicle.EngineType engineType, String color, boolean hasLegalPapers,
                                   Vehicle.Transmission transmission) {
        super(name, startingPrice, description, category);
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.odo = odo;
        this.engineType = engineType;
        this.color = color;
        this.hasLegalPapers = hasLegalPapers;
        this.transmission = transmission;
        validateVehicleDTO();
    }

    protected void validateVehicleDTO() {
        if (brand != null) {
            String b = brand.trim();
            if (b.length() > 120) {
                throw new IllegalArgumentException("Brand không được vượt quá 120 ký tự.");
            }
        }
        
        if (model != null) {
            String m = model.trim();
            if (m.length() > 120) {
                throw new IllegalArgumentException("Model không được vượt quá 120 ký tự.");
            }
        }
        
        if (manufacturingYear != 0) {
            int current = LocalDate.now().getYear();
            final int MIN_YEAR = 1886;
            if (manufacturingYear < MIN_YEAR || manufacturingYear > current) {
                throw new IllegalArgumentException("Năm sản xuất không hợp lệ. Giá trị hợp lệ: " +
                        MIN_YEAR + " .. " + current + " (Nếu không rõ có thể điền '0').");
            }
        }
        
        if (odo < 0) {
            throw new IllegalArgumentException("Odometer (odo) phải >= 0.");
        }
        if (odo > 1_000_000) {
            throw new IllegalArgumentException("Odometer có vẻ không hợp lệ (>1,000,000).");
        }
        
        if (engineType == null) {
            throw new IllegalArgumentException("EngineType không được để trống.");
        }
        
        if (color != null) {
            String c = color.trim();
            if (c.length() > 50) {
                throw new IllegalArgumentException("Color không được vượt quá 50 ký tự.");
            }
        }
        
        if (transmission == null) {
            throw new IllegalArgumentException("Transmission không được để trống.");
        }
    }

    public void validate() {
        validateItemDTO();
        validateVehicleDTO();
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
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

    public String getColor() {
        return color;
    }

    public boolean isHasLegalPapers() {
        return hasLegalPapers;
    }

    public Vehicle.Transmission getTransmission() {
        return transmission;
    }

    @Override
    public String toString() {
        return super.toString() +
                " | brand: " + brand +
                " | model: " + model +
                " | manufacturingYear: " + manufacturingYear +
                " | odo: " + odo +
                " | engineType: " + engineType +
                " | color: " + color +
                " | hasLegalPapers: " + hasLegalPapers +
                " | transmission: " + transmission;
    }
}
