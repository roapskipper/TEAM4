package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Electronics;
import com.team4.model.Item;

import java.math.BigDecimal;

public class CreateElectronicsRequestDTO extends CreateItemRequestDTO {
    private String brand;                           // thương hiệu
    private String model;                           // tên model
    private Electronics.ConditionGrade itemCondition; // tình trạng
    private int warrantyMonths;                     // bảo hành (tháng)
    private boolean fullyFunctional;                // hoạt động đầy đủ

    public CreateElectronicsRequestDTO() {}

    public CreateElectronicsRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category,
                                       String brand, String model, Electronics.ConditionGrade itemCondition, 
                                       int warrantyMonths, boolean fullyFunctional) {
        super(name, startingPrice, description, category);
        this.brand = brand;
        this.model = model;
        this.itemCondition = itemCondition;
        this.warrantyMonths = warrantyMonths;
        this.fullyFunctional = fullyFunctional;
        validateElectronicsDTO();
    }

    protected void validateElectronicsDTO() {
        if (brand != null) {
            String b = brand.trim();
            if (b.length() > 50) {
                throw new IllegalArgumentException("Brand không được vượt quá 50 ký tự.");
            }
        }
        
        if (model != null) {
            String m = model.trim();
            if (m.length() > 50) {
                throw new IllegalArgumentException("Model không được vượt quá 50 ký tự.");
            }
        }
        
        if (itemCondition == null) {
            throw new IllegalArgumentException("Item condition không được để trống.");
        }
        
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("Warranty months phải >= 0.");
        }
    }

    public void validate() {
        validateItemDTO();
        validateElectronicsDTO();
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

    @Override
    public String toString() {
        return super.toString() +
                " | brand: " + brand +
                " | model: " + model +
                " | condition: " + itemCondition +
                " | warrantyMonths: " + warrantyMonths +
                " | fullyFunctional: " + fullyFunctional;
    }
}
