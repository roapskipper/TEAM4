package com.team4.factory;

import com.team4.model.Art;
import com.team4.model.Electronics;
import com.team4.model.Fashion;
import com.team4.model.Item;
import com.team4.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemRequestDefaults request-layer normalization")
class ItemRequestDefaultsTest {

    @Test
    @DisplayName("Áp dụng default cho Art trên ItemRequest")
    void apply_artDefaults() {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.ART);
        req.setArtist("  ");
        req.setCreationYear(0);

        ItemRequestDefaults.apply(req);

        assertEquals("Unknown", req.getArtist());
        assertEquals(0, req.getCreationYear());
    }

    @Test
    @DisplayName("Áp dụng default cho Electronics trên ItemRequest")
    void apply_electronicsDefaults() {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.ELECTRONICS);
        req.setBrand(null);
        req.setModel("");

        ItemRequestDefaults.apply(req);

        assertEquals("Unknown", req.getBrand());
        assertEquals("Unknown", req.getModel());
    }

    @Test
    @DisplayName("Áp dụng default cho Fashion trên ItemRequest")
    void apply_fashionDefaults() {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.FASHION);
        req.setGender(null);

        ItemRequestDefaults.apply(req);

        assertEquals(Fashion.Gender.UNISEX, req.getGender());
    }

    @Test
    @DisplayName("Áp dụng default cho Vehicle trên ItemRequest")
    void apply_vehicleDefaults() {
        ItemRequest req = new ItemRequest();
        req.setCategory(Item.ItemCategory.VEHICLE);
        req.setTransmission(null);

        ItemRequestDefaults.apply(req);

        assertEquals(Vehicle.Transmission.OTHER, req.getTransmission());
    }
}
