package com.team4.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class VehicleValidationTest {

    private Vehicle createTestVehicle(int odo, int manufacturingYear, Vehicle.EngineType engineType, Vehicle.Transmission transmission) {
        return new Vehicle(
                "Test Vehicle",
                new BigDecimal("500000000"),
                "A test vehicle",
                "seller1",
                "Toyota",
                "Camry",
                manufacturingYear,
                odo,
                engineType,
                "Black",
                true,
                transmission
        );
    }

    @Test
    void testValidVehicleCreation() {
        Vehicle item = createTestVehicle(10000, 2020, Vehicle.EngineType.GASOLINE, Vehicle.Transmission.AUTOMATIC);
        assertNotNull(item);
        assertEquals(10000, item.getOdo());
        assertEquals(2020, item.getManufacturingYear());
        assertEquals(Vehicle.EngineType.GASOLINE, item.getEngineType());
        assertEquals(Vehicle.Transmission.AUTOMATIC, item.getTransmission());
    }

    @Test
    void testInvalidOdo() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> {
            createTestVehicle(-1, 2020, Vehicle.EngineType.GASOLINE, Vehicle.Transmission.AUTOMATIC);
        });
        assertTrue(e1.getMessage().toLowerCase().contains("odo") || e1.getMessage().toLowerCase().contains("odometer"));

        Exception e2 = assertThrows(IllegalArgumentException.class, () -> {
            createTestVehicle(1000001, 2020, Vehicle.EngineType.GASOLINE, Vehicle.Transmission.AUTOMATIC);
        });
        assertTrue(e2.getMessage().toLowerCase().contains("odo") || e2.getMessage().toLowerCase().contains("odometer"));
    }

    @Test
    void testInvalidManufacturingYear() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> {
            createTestVehicle(10000, 1800, Vehicle.EngineType.GASOLINE, Vehicle.Transmission.AUTOMATIC);
        });
        assertTrue(e1.getMessage().toLowerCase().contains("year"));

        Exception e2 = assertThrows(IllegalArgumentException.class, () -> {
            createTestVehicle(10000, 3000, Vehicle.EngineType.GASOLINE, Vehicle.Transmission.AUTOMATIC);
        });
        assertTrue(e2.getMessage().toLowerCase().contains("year"));
    }

    @Test
    void testMissingEngineType() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            createTestVehicle(10000, 2020, null, Vehicle.Transmission.AUTOMATIC);
        });
        assertTrue(e.getMessage().toLowerCase().contains("engine"));
    }

    @Test
    void testDefaultTransmission() {
        Vehicle item = createTestVehicle(10000, 2020, Vehicle.EngineType.GASOLINE, null);
        assertEquals(Vehicle.Transmission.OTHER, item.getTransmission());
    }
}
