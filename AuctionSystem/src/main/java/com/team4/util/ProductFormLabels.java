package com.team4.util;

import com.team4.model.Art;
import com.team4.model.Collectible;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Vehicle;

/**
 * English display labels for product form enum values.
 */
public final class ProductFormLabels {

    private ProductFormLabels() {}

    public static String artMedium(Art.Medium m) {
        return switch (m) {
            case OIL_PAINT -> "Oil Paint";
            case WATERCOLOR -> "Watercolor";
            case ACRYLIC -> "Acrylic";
            case GOUACHE -> "Gouache";
            case PASTEL -> "Pastel";
            case INK -> "Ink";
            case SCULPTURE_MARBLE -> "Marble Sculpture";
            case SCULPTURE_WOOD -> "Wood Sculpture";
            case SCULPTURE_CERAMIC -> "Ceramic Sculpture";
            case PHOTOGRAPHY -> "Photography";
            case MIXED_MEDIA -> "Mixed Media";
            case OTHER -> "Other";
        };
    }

    public static String collectibleRarity(Collectible.RarityLevel r) {
        return switch (r) {
            case COMMON -> "Common";
            case UNCOMMON -> "Uncommon";
            case RARE -> "Rare";
            case VERY_RARE -> "Very Rare";
            case ULTRA_RARE -> "Ultra Rare";
        };
    }

    public static String conditionGrade(String prefix, String grade) {
        return switch (grade) {
            case "POOR" -> prefix + "Poor";
            case "FAIR" -> prefix + "Fair";
            case "GOOD" -> prefix + "Good";
            case "VERY_GOOD" -> prefix + "Very Good";
            case "EXCELLENT" -> prefix + "Excellent";
            case "MINT" -> prefix + "Mint";
            default -> grade;
        };
    }

    public static String electronicsCondition(Electronics.ConditionGrade g) {
        return conditionGrade("", g.name());
    }

    public static String fashionSize(Fashion.Size s) {
        return s.name();
    }

    public static String fashionGender(Fashion.Gender g) {
        return switch (g) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case UNISEX -> "Unisex (Default)";
        };
    }

    public static String vehicleEngine(Vehicle.EngineType e) {
        return switch (e) {
            case GASOLINE -> "Gasoline";
            case DIESEL -> "Diesel";
            case ELECTRIC -> "Electric";
            case HYBRID -> "Hybrid";
            case PLUG_IN_HYBRID -> "Plug-in Hybrid";
            case HYDROGEN -> "Hydrogen";
            case OTHER -> "Other";
        };
    }

    public static String vehicleTransmission(Vehicle.Transmission t) {
        return switch (t) {
            case MANUAL -> "Manual";
            case AUTOMATIC -> "Automatic";
            case CVT -> "CVT";
            case DCT -> "Dual-Clutch (DCT)";
            case OTHER -> "Other (Default)";
        };
    }
}
