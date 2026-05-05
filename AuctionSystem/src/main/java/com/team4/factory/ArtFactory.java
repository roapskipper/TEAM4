package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Item;
import java.math.BigDecimal;

public class ArtFactory implements ItemFactory {

    @Override
    public Item createItem(ItemRequest itemRequest) {
        // Sử dụng Constructor 1 của Art (Tự sinh UUID, gán category ART)
        return new Art(
                itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getOwnerId(),
                itemRequest.getArtist(),
                itemRequest.getCreationYear(),
                itemRequest.getMedium(),
                itemRequest.getDimensions()
        );
    }
}