package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Item;

public class ArtFactory implements ItemFactory {
    private String id;
    private String name;
    private double startingPrice;
    private String desc;

    private String artist;
    private int creationYear;
    private String medium;
    private String dimensions;
    private String style;
    private boolean isOriginal;
    private String exhibitionHistory;

    public ArtFactory(String id, String name, double startingPrice, String desc,
                      String artist, int creationYear, String medium, String dimensions,
                      String style, boolean isOriginal, String exhibitionHistory) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.desc = desc;

        this.artist = artist;
        this.creationYear = creationYear;
        this.medium = medium;
        this.dimensions = dimensions;
        this.style = style;
        this.isOriginal = isOriginal;
        this.exhibitionHistory = exhibitionHistory;
    }

    @Override
    public Item createItem() {
        return new Art(id, name, startingPrice, desc, artist, creationYear,
                medium, dimensions, style, isOriginal, exhibitionHistory);
    }
}