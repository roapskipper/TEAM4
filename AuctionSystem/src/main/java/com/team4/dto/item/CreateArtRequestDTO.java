package com.team4.dto.item;

import com.team4.dto.auction.CreateItemRequestDTO;
import com.team4.model.Art;
import com.team4.model.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class CreateArtRequestDTO extends CreateItemRequestDTO {
    private String artist;
    private int creationYear;
    private Art.Medium medium;
    private String dimensions;

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
            "^\\s*\\d{1,4}(?:\\.\\d{1,3})?\\s*[xX\\u00D7]\\s*\\d{1,4}(?:\\.\\d{1,3})?(?:\\s*[xX\\u00D7]\\s*\\d{1,4}(?:\\.\\d{1,3})?)?\\s*(?i)(cm|mm|m|in|inch|inches)?\\s*$"
    );

    protected void validateArtDTO() {
        if (artist == null) return;
        String a = artist.trim();
        if (a.length() < 2) {
            throw new IllegalArgumentException("Artist name must be at least 2 characters.");
        }
        if (a.length() > 50) {
            throw new IllegalArgumentException("Artist name must not exceed 50 characters.");
        }

        if (creationYear == 0) return;
        int current = LocalDateTime.now().getYear();
        final int MIN_YEAR = -3000;
        if (creationYear < MIN_YEAR || creationYear > current) {
            throw new IllegalArgumentException("Creation year is invalid. Valid range: " +
                    MIN_YEAR + " .. " + current + " (enter '0' if unknown).");
        }

        if (dimensions == null) return;
        String d = dimensions.trim();
        if (d.isEmpty()) return;
        if (d.length() > 50) {
            throw new IllegalArgumentException("Dimensions must not exceed 50 characters.");
        }
        if (!DIMENSION_PATTERN.matcher(d).matches()) {
            throw new IllegalArgumentException("Dimensions are invalid.");
        }

        if (medium == null) {
            throw new IllegalArgumentException("Medium must not be null.");
        }
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
