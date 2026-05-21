package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Item;
import com.team4.model.Vehicle;

/**
 * Applies category-specific default values to {@link ItemRequest} before entity construction.
 */
public final class ItemRequestDefaults {

    private ItemRequestDefaults() {}

    public static void apply(ItemRequest request) {
        if (request == null || request.getCategory() == null) {
            return;
        }
        switch (request.getCategory()) {
            case ART -> {
                request.setArtist(Art.resolveArtist(request.getArtist()));
                request.setCreationYear(Art.resolveCreationYear(request.getCreationYear()));
            }
            case ELECTRONICS -> {
                request.setBrand(Electronics.resolveBrand(request.getBrand()));
                request.setModel(Electronics.resolveModel(request.getModel()));
            }
            case FASHION -> request.setGender(Fashion.resolveGender(request.getGender()));
            case VEHICLE -> request.setTransmission(Vehicle.resolveTransmission(request.getTransmission()));
            default -> { /* COLLECTIBLE and others use their own defaults */ }
        }
    }
}
