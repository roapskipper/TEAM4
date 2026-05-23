package com.team4.dto.auction;

import com.team4.model.Fashion;
import com.team4.model.Item;

import java.math.BigDecimal;

public class CreateFashionRequestDTO extends CreateItemRequestDTO {
    private String brand;
    private Fashion.Size size;
    private String material;
    private String color;
    private Fashion.Gender gender;
    private Fashion.ConditionGrade condition;
    private boolean authentic;

    public CreateFashionRequestDTO() {}

    public CreateFashionRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category,
                                   String brand, Fashion.Size size, String material, String color,
                                   Fashion.Gender gender, Fashion.ConditionGrade condition, boolean authentic) {
        super(name, startingPrice, description, category);
        this.brand = brand;
        this.size = size;
        this.material = material;
        this.color = color;
        this.gender = gender;
        this.condition = condition;
        this.authentic = authentic;
        validateFashionDTO();
    }

    protected void validateFashionDTO() {
        if (brand != null) {
            String b = brand.trim();
            if (b.length() > 120) {
                throw new IllegalArgumentException("Brand must not exceed 120 characters.");
            }
        }

        if (size == null) {
            throw new IllegalArgumentException("Size is required.");
        }

        if (material != null) {
            String m = material.trim();
            if (m.length() > 120) {
                throw new IllegalArgumentException("Material must not exceed 120 characters.");
            }
        }

        if (color != null) {
            String c = color.trim();
            if (c.length() > 50) {
                throw new IllegalArgumentException("Color must not exceed 50 characters.");
            }
        }

        if (gender == null) {
            throw new IllegalArgumentException("Gender is required.");
        }

        if (condition == null) {
            throw new IllegalArgumentException("Condition grade is required.");
        }
    }

    public void validate() {
        validateItemDTO();
        validateFashionDTO();
    }

    public String getBrand() {
        return brand;
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

    @Override
    public String toString() {
        return super.toString() +
                " | brand: " + brand +
                " | size: " + size +
                " | material: " + material +
                " | color: " + color +
                " | gender: " + gender +
                " | condition: " + condition +
                " | authentic: " + authentic;
    }
}
