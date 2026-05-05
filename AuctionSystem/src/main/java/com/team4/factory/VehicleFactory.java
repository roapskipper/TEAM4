package com.team4.factory;

import com.team4.model.Vehicle;
import com.team4.model.Item;
import java.math.BigDecimal;

public class VehicleFactory implements ItemFactory {
    @Override
    public Item createItem(ItemRequest itemRequest) {
        return new Vehicle(itemRequest.getName(),
                itemRequest.getStartingPrice(),
                itemRequest.getDescription(),
                itemRequest.getOwnerId(),
                itemRequest.getBrand(),
                itemRequest.getModel(),
                itemRequest.getManufacturingYear(),
                itemRequest.getOdo(),
                itemRequest.getEngineType(),
                itemRequest.getColor(),
                itemRequest.isHasLegalPapers(),
                itemRequest.getTransmission());
    }
}