package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Art;
import com.team4.model.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class CreateArtRequestDTO extends CreateItemRequestDTO {
    private String artist;        // tác giả
    private int creationYear;     // năm sáng tác
    private Art.Medium medium;        // chất liệu (sơn dầu, màu nước...)
    private String dimensions;    // kích thước

    public CreateArtRequestDTO() {}
    public CreateArtRequestDTO(String name, BigDecimal startingPrice, String description, Item.ItemCategory category, String artist, int creationYear, Art.Medium medium, String dimensions) {
        super(name, startingPrice, description, category);
        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        validateItemDTO();
    }

    public void validate() {
        validateItemDTO();
        validateArtDTO();
    }

    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "^\\s*\\d{1,4}(?:\\.\\d{1,3})?\\s*[x×]\\s*\\d{1,4}(?:\\.\\d{1,3})?(?:\\s*[x×]\\s*\\d{1,4}(?:\\.\\d{1,3})?)?\\s*(?i)(cm|mm|m|in|inch|inches)?\\s*$"
    );
    protected void validateArtDTO() {
        if (artist == null) return; // optional
        String a = artist.trim();
        if (a.length() < 2) {
            throw new IllegalArgumentException("Artist name must contain at least 2 characters.");
        }
        if (a.length() > 50) {
            throw new IllegalArgumentException("Artist name must not exceed 50 characters.");
        }

        if (creationYear == 0) return; // 0 = unknown (chấp nhận).
        int current = LocalDateTime.now().getYear();
        final int MIN_YEAR = -3000; // chấp nhận đến khoảng 3000 TCN
        if (creationYear < MIN_YEAR || creationYear > current) {
            throw new IllegalArgumentException("Invalid creation year. Valid range: " +
                    (MIN_YEAR) + " .. " + current + " (Use '0' if unknown).");
        }

        if (dimensions == null) return;
        String d = dimensions.trim();
        if (d.isEmpty()) return;
        if (d.length() > 50) {
            throw new IllegalArgumentException("Dimensions are too long (maximum 50 characters).");
        }
        // Nếu khớp định dạng chuẩn (ví dụ "30x40 cm" hoặc "30 × 40 × 2 cm") thì OK
        if (!DIMENSION_PATTERN.matcher(d).matches()) {
            throw new IllegalArgumentException("Invalid dimensions.");
        }

        if (medium == null)
            throw new IllegalArgumentException("Medium must not be null");
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

    @Override
    public String toString() {
        return super.toString() +
                " | artist: " + artist +
                " | creationYear: " + creationYear +
                " | medium: " + medium +
                " | dimensions: " + dimensions;
    }
}
