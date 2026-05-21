package com.team4.util;

import com.team4.model.Art;
import com.team4.model.Collectible;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Vehicle;

/**
 * Vietnamese display labels for product form enum values.
 */
public final class ProductFormLabels {

    private ProductFormLabels() {}

    public static String artMedium(Art.Medium m) {
        return switch (m) {
            case OIL_PAINT -> "Sơn dầu";
            case WATERCOLOR -> "Màu nước";
            case ACRYLIC -> "Acrylic";
            case GOUACHE -> "Gouache";
            case PASTEL -> "Phấn màu";
            case INK -> "Mực";
            case SCULPTURE_MARBLE -> "Điêu khắc đá";
            case SCULPTURE_WOOD -> "Điêu khắc gỗ";
            case SCULPTURE_CERAMIC -> "Gốm sứ";
            case PHOTOGRAPHY -> "Nhiếp ảnh";
            case MIXED_MEDIA -> "Đa chất liệu";
            case OTHER -> "Khác";
        };
    }

    public static String collectibleRarity(Collectible.RarityLevel r) {
        return switch (r) {
            case COMMON -> "Phổ biến";
            case UNCOMMON -> "Ít phổ biến";
            case RARE -> "Hiếm";
            case VERY_RARE -> "Rất hiếm";
            case ULTRA_RARE -> "Cực hiếm";
        };
    }

    public static String conditionGrade(String prefix, String grade) {
        return switch (grade) {
            case "POOR" -> prefix + "Kém";
            case "FAIR" -> prefix + "Trung bình";
            case "GOOD" -> prefix + "Tốt";
            case "VERY_GOOD" -> prefix + "Rất tốt";
            case "EXCELLENT" -> prefix + "Xuất sắc";
            case "MINT" -> prefix + "Hoàn hảo";
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
            case MALE -> "Nam (MALE)";
            case FEMALE -> "Nữ (FEMALE)";
            case UNISEX -> "Unisex (mặc định)";
        };
    }

    public static String vehicleEngine(Vehicle.EngineType e) {
        return switch (e) {
            case GASOLINE -> "Xăng";
            case DIESEL -> "Dầu diesel";
            case ELECTRIC -> "Điện";
            case HYBRID -> "Hybrid";
            case PLUG_IN_HYBRID -> "Plug-in Hybrid";
            case HYDROGEN -> "Hydro";
            case OTHER -> "Khác";
        };
    }

    public static String vehicleTransmission(Vehicle.Transmission t) {
        return switch (t) {
            case MANUAL -> "Số sàn";
            case AUTOMATIC -> "Tự động";
            case CVT -> "Vô cấp (CVT)";
            case DCT -> "Ly hợp kép (DCT)";
            case OTHER -> "Khác (mặc định)";
        };
    }
}
