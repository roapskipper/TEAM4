package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Item;
import java.math.BigDecimal;

public class ArtFactory implements ItemFactory {
    // Thuộc tính cơ bản của Item
    private String name;
    private BigDecimal startingPrice;
    private String desc;
    private String ownerId; // Liên kết với Seller

    // Thuộc tính riêng của Art
    private String artist;
    private int creationYear;
    private Art.Medium medium;
    private String dimensions;

    public ArtFactory(String name, BigDecimal startingPrice, String desc, String ownerId,
                      String artist, int creationYear, Art.Medium medium, String dimensions) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;
        this.ownerId = ownerId;
        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
    }

    @Override
    public Item createItem() {
        // Sử dụng Constructor 1 của Art (Tự sinh UUID, gán category ART)
        return new Art(
                name,
                startingPrice,
                desc,
                ownerId,
                artist,
                creationYear,
                medium,
                dimensions
        );
    }
}