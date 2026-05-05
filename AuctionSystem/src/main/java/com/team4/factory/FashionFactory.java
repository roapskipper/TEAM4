package com.team4.factory;

import com.team4.model.Fashion;
import com.team4.model.Item;

public class FashionFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest itemRequest) {
        return new Fashion(
                itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getOwnerId(),
                itemRequest.getBrand(),
                itemRequest.getSize(),
                itemRequest.getMaterial(),
                itemRequest.getColor(),
                itemRequest.getGender(),
                itemRequest.getCondition(),
                itemRequest.isAuthentic()
        );
    }
}