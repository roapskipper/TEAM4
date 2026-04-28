package com.team4.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** Art: Đại diện cho các sản phẩm nghệ thuật */
public class Art extends Item {
    // enum Medium (cho nó chất)
    public enum Medium {
        OIL_PAINT,          // Sơn dầu
        WATERCOLOR,         // Màu nước
        ACRYLIC,            // Acrylic
        GOUACHE,            // Bột màu
        PASTEL,             // Phấn màu
        INK,                // Mực

        SCULPTURE_MARBLE,   // Điêu khắc đá cẩm thạch
        SCULPTURE_WOOD,     // Điêu khắc gỗ
        SCULPTURE_CERAMIC,  // Gốm sứ

        PHOTOGRAPHY,        // Nhiếp ảnh

        MIXED_MEDIA,        // Đa chất liệu
        OTHER
    }
    private String artist;        // tác giả
    private int creationYear;     // năm sáng tác
    private Medium medium;        // chất liệu (sơn dầu, màu nước...)
    private String dimensions;    // kích thước
    // Constructor dùng khi tạo Art mới (Seller đăng sản phẩm)
    public Art(String name,
               BigDecimal startingPrice,
               String description,
               String ownerId,
               String artist,
               int creationYear,
               Medium medium,
               String dimensions) {
        super(name, startingPrice, description, ItemCategory.ART, ownerId);
        this.artist = (artist == null || artist.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(artist);
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = normalizeOptional(dimensions);
        validateArtist(this.artist);
        validateDimensions(this.dimensions);
        validateCreationYear(this.creationYear);
        validateMedium(this.medium);
    }
    // Constructor dùng khi nạp Art từ DB
    public Art(String id,
               LocalDateTime createdAt,
               String name,
               BigDecimal startingPrice,
               String description,
               String ownerId,
               String artist,
               int creationYear,
               Medium medium,
               String dimensions) {
        super(id, createdAt, name, startingPrice, description, ItemCategory.ART, ownerId);
        this.artist = (artist == null || artist.trim().isEmpty())
                ? "Unknown"
                : normalizeOptional(artist);
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = normalizeOptional(dimensions);
        validateArtist(this.artist);
        validateDimensions(this.dimensions);
        validateCreationYear(this.creationYear);
        validateMedium(this.medium);
    }

    // Validate
    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "^\\s*\\d{1,4}(?:\\.\\d{1,3})?\\s*[x×]\\s*\\d{1,4}(?:\\.\\d{1,3})?(?:\\s*[x×]\\s*\\d{1,4}(?:\\.\\d{1,3})?)?\\s*(?i)(cm|mm|m|in|inch|inches)?\\s*$"
    );
    private static void validateArtist(String artist) {
        if (artist == null) return; // optional
        String a = artist.trim();
        if (a.length() < 2) {
            throw new IllegalArgumentException("Tên tác giả phải gồm 2 kí tự trở lên.");
        }
        if (a.length() > 50) {
            throw new IllegalArgumentException("Tên tác giả không được vượt quá 50 ký tự.");
        }
    }
    private static void validateCreationYear(int year) {
        if (year == 0) return; // 0 = unknown (chấp nhận).
        int current = LocalDateTime.now().getYear();
        final int MIN_YEAR = -3000; // chấp nhận đến khoảng 3000 TCN
        if (year < MIN_YEAR || year > current) {
            throw new IllegalArgumentException("Năm sáng tác không hợp lệ. Giá trị hợp lệ: " +
                    (MIN_YEAR) + " .. " + current + " (Bạn có thể điền '0' để chỉ 'không biết').");
        }
    }
    private static void validateDimensions(String dims) {
        if (dims == null) return;
        String d = dims.trim();
        if (d.isEmpty()) return;
        if (d.length() > 50) {
            throw new IllegalArgumentException("Kích thước quá dài (tối đa 50 ký tự).");
        }
        // Nếu khớp định dạng chuẩn (ví dụ "30x40 cm" hoặc "30 × 40 × 2 cm") thì OK
        if (!DIMENSION_PATTERN.matcher(d).matches()) {
            throw new IllegalArgumentException("Kích thước không hợp lệ.");
        }
    }
    private static void validateMedium(Medium medium) {
        if (medium == null)
            throw new IllegalArgumentException("Chất liệu không được null");
    }
    @Override
    public String summary() {
           return artist + " - " + creationYear + " - " + medium.name();
    }
    @Override
    public String toString() {
        return super.toString() +
                " | artist: " + artist +
                " | creationYear: " + creationYear +
                " | medium: " + medium +
                " | dimensions: " + dimensions;
    }

    // Getter/Setter
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = normalizeOptional(artist);
    validateArtist(this.artist);}
    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear;
    validateCreationYear(this.creationYear);}
    public Medium getMedium() { return medium; }
    public void setMedium(Medium medium) { this.medium = medium;
    validateMedium(this.medium);}
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = normalizeOptional(dimensions);
    validateDimensions(this.dimensions);}
}

