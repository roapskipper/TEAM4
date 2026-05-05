package com.team4.factory;

import com.team4.model.Collectible;
import com.team4.model.Item;
import java.math.BigDecimal;

public class CollectibleFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest itemRequest) {
        return new Collectible(
                itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getOwnerId(),
                itemRequest.getYearOfOrigin(),
                itemRequest.getRarityLevel(),
                itemRequest.getConditionGrade(),
                itemRequest.isHasCertificate(),
                itemRequest.getOrigin()
        );
    }
}