package com.team4.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category default normalization (model layer)")
class DefaultNormalizationTest {

    @Test
    @DisplayName("Art: blank artist -> Unknown; year 0 preserved")
    void art_blankArtistAndMissingYear() {
        Art art = new Art("Test", new BigDecimal("100"), "Desc", "owner", "   ", 0, Art.Medium.OIL_PAINT, null);
        assertEquals("Unknown", art.getArtist());
        assertEquals(0, art.getCreationYear());
        assertEquals("Unknown", Art.resolveArtist(null));
        assertEquals(0, Art.resolveCreationYear(0));
    }

    @Test
    @DisplayName("Electronics: blank brand/model -> Unknown")
    void electronics_blankBrandAndModel() {
        Electronics elec = new Electronics(
                "Test", new BigDecimal("100"), "Desc", "owner",
                "", "   ", Electronics.ConditionGrade.GOOD, 12, true);
        assertEquals("Unknown", elec.getBrand());
        assertEquals("Unknown", elec.getModel());
        assertEquals("Unknown", Electronics.resolveBrand(null));
        assertEquals("Unknown", Electronics.resolveModel("  "));
    }

    @Test
    @DisplayName("Fashion: missing gender -> UNISEX")
    void fashion_missingGender() {
        Fashion fash = new Fashion(
                "Test", new BigDecimal("100"), "Desc", "owner",
                null, Fashion.Size.M, null, null, null, Fashion.ConditionGrade.GOOD, false);
        assertEquals(Fashion.Gender.UNISEX, fash.getGender());
        assertEquals(Fashion.Gender.UNISEX, Fashion.resolveGender(null));
    }

    @Test
    @DisplayName("Vehicle: missing transmission -> OTHER")
    void vehicle_missingTransmission() {
        Vehicle veh = new Vehicle(
                "Test", new BigDecimal("100"), "Desc", "owner",
                null, null, 2020, 10000, Vehicle.EngineType.GASOLINE, null, true, null);
        assertEquals(Vehicle.Transmission.OTHER, veh.getTransmission());
        assertEquals(Vehicle.Transmission.OTHER, Vehicle.resolveTransmission(null));
    }
}
