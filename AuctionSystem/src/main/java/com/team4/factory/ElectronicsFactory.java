package com.team4.factory;

import com.team4.model.Electronics;
import com.team4.model.Item;
import java.math.BigDecimal;

public class ElectronicsFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest itemRequest) {
        // Sử dụng Constructor của Electronics (Sẽ tự sinh UUID và gán category = ELECTRONICS)
        return new Electronics(
                itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getOwnerId(),
                itemRequest.getBrand(),
                itemRequest.getModel(),
                itemRequest.getItemCondition(),
                itemRequest.getWarrantyMonths(),
                itemRequest.isFullyFunctional()
        );
    }
}